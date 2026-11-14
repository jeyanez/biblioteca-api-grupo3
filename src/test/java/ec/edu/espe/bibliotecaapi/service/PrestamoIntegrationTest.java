package ec.edu.espe.bibliotecaapi.service;

import ec.edu.espe.bibliotecaapi.dto.LibroRequest;
import ec.edu.espe.bibliotecaapi.dto.PrestamoRequest;
import ec.edu.espe.bibliotecaapi.dto.PrestamoResponse;
import ec.edu.espe.bibliotecaapi.entity.Autor;
import ec.edu.espe.bibliotecaapi.entity.Libro;
import ec.edu.espe.bibliotecaapi.entity.Prestamo;
import ec.edu.espe.bibliotecaapi.entity.Usuario;
import ec.edu.espe.bibliotecaapi.exception.PrestamoInvalidoException;
import ec.edu.espe.bibliotecaapi.exception.SinEjemplaresException;
import ec.edu.espe.bibliotecaapi.repository.AutorRepository;
import ec.edu.espe.bibliotecaapi.repository.LibroRepository;
import ec.edu.espe.bibliotecaapi.repository.PrestamoRepository;
import ec.edu.espe.bibliotecaapi.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Sin @Transactional: cada llamada al servicio confirma o revierte su propia transacción. */
@SpringBootTest
@ActiveProfiles("test")
class PrestamoIntegrationTest {
    @Autowired
    private PrestamoService prestamoService;
    @Autowired
    private LibroService libroService;
    @Autowired
    private AutorRepository autorRepository;
    @Autowired
    private LibroRepository libroRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @MockitoSpyBean
    private PrestamoRepository prestamoRepository;

    private Long autorId;
    private Long libroId;
    private Long usuarioId;

    @BeforeEach
    void prepararBaseDeDatos() {
        prestamoRepository.deleteAll();
        libroRepository.deleteAll();
        usuarioRepository.deleteAll();
        autorRepository.deleteAll();

        Autor autor = new Autor();
        autor.setNombre("Gabriel García Márquez");
        autor.setNacionalidad("Colombiana");
        autorId = autorRepository.saveAndFlush(autor).getId();

        Libro libro = new Libro();
        libro.setTitulo("Cien años de soledad");
        libro.setIsbn("9780307474728");
        libro.setStockDisponible(1);
        libro.setAutor(autor);
        libroId = libroRepository.saveAndFlush(libro).getId();

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana Pérez");
        usuario.setCedula("1712345678");
        usuario.setEmail("ana@example.com");
        usuarioId = usuarioRepository.saveAndFlush(usuario).getId();
    }

    @Test
    void guardarPrestamoPersisteElPrestamoYDescuentaUnEjemplar() {
        PrestamoResponse respuesta = prestamoService.guardarPrestamo(solicitud());

        assertNotNull(respuesta.id());
        assertEquals(0, stockPersistido());
        assertEquals(1, prestamoRepository.count());
        Prestamo persistido = prestamoRepository.findById(respuesta.id()).orElseThrow();
        assertEquals(PrestamoService.PENDIENTE, persistido.getEstado());
        assertEquals(libroId, persistido.getLibro().getId());
        assertEquals(usuarioId, persistido.getUsuario().getId());
        assertEquals(solicitud().fechaDevolucion(), persistido.getFechaDevolucion());
    }

    @Test
    void devolverReponeUnEjemplarYUnaSegundaDevolucionNoAlteraElStock() {
        PrestamoResponse prestamo = prestamoService.guardarPrestamo(solicitud());
        assertEquals(List.of(prestamo.id()), prestamoService.listarPendientes().stream()
                .map(PrestamoResponse::id).toList());

        PrestamoResponse devuelto = prestamoService.devolverPrestamo(prestamo.id());

        assertEquals(PrestamoService.DEVUELTO, devuelto.estado());
        assertEquals(1, stockPersistido());
        assertTrue(prestamoService.listarPendientes().isEmpty());
        assertThrows(PrestamoInvalidoException.class,
                () -> prestamoService.devolverPrestamo(prestamo.id()));
        assertEquals(1, stockPersistido());
        assertEquals(PrestamoService.DEVUELTO,
                prestamoRepository.findById(prestamo.id()).orElseThrow().getEstado());
    }

    @Test
    @Timeout(30)
    void dosSolicitudesConcurrentesNoPrestanDosVecesElUltimoEjemplar() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<Boolean> primera = executor.submit(() -> intentarPrestamo(preparados, iniciar));
            Future<Boolean> segunda = executor.submit(() -> intentarPrestamo(preparados, iniciar));
            assertTrue(preparados.await(5, TimeUnit.SECONDS), "Los dos solicitantes deben estar listos");
            iniciar.countDown();

            boolean exitoPrimero = primera.get(15, TimeUnit.SECONDS);
            boolean exitoSegundo = segunda.get(15, TimeUnit.SECONDS);

            assertTrue(exitoPrimero ^ exitoSegundo, "Exactamente un préstamo debe ser aceptado");
            assertEquals(1, prestamoRepository.count());
            assertEquals(0, stockPersistido());
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Los solicitantes deben finalizar");
        }
    }

    @Test
    @Timeout(30)
    void dosDevolucionesConcurrentesDelMismoLibroReponenAmbosEjemplares() throws Exception {
        Libro libro = libroRepository.findById(libroId).orElseThrow();
        libro.setStockDisponible(2);
        libroRepository.saveAndFlush(libro);
        Long primerId = prestamoService.guardarPrestamo(solicitud()).id();
        Long segundoId = prestamoService.guardarPrestamo(solicitud()).id();
        assertEquals(0, stockPersistido());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<PrestamoResponse> primera = executor.submit(
                    () -> devolverCuandoInicie(primerId, preparados, iniciar));
            Future<PrestamoResponse> segunda = executor.submit(
                    () -> devolverCuandoInicie(segundoId, preparados, iniciar));
            assertTrue(preparados.await(5, TimeUnit.SECONDS), "Las dos devoluciones deben estar listas");
            iniciar.countDown();

            assertEquals(PrestamoService.DEVUELTO, primera.get(15, TimeUnit.SECONDS).estado());
            assertEquals(PrestamoService.DEVUELTO, segunda.get(15, TimeUnit.SECONDS).estado());
            assertEquals(2, stockPersistido(), "Cada devolución debe reponer un ejemplar");
            assertEquals(2, prestamoRepository.count());
            assertTrue(prestamoService.listarPendientes().isEmpty());
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Las devoluciones deben finalizar");
        }
    }

    @Test
    void unFalloAlGuardarElPrestamoRevierteInclusoElDescuentoYaEnviadoALaBase() {
        doAnswer(invocacion -> {
            // Fuerza el UPDATE del stock dentro de la transacción antes del fallo simulado.
            libroRepository.flush();
            throw new DataIntegrityViolationException("Fallo simulado al persistir el préstamo");
        }).when(prestamoRepository).save(any(Prestamo.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> prestamoService.guardarPrestamo(solicitud()));

        assertEquals(1, stockPersistido());
        assertEquals(0, prestamoRepository.count());
    }

    @Test
    void laValidacionDelServicioImpideRegistrarStockNegativo() {
        LibroRequest invalido = new LibroRequest("Otro libro", "9780307474729", -1, autorId);

        assertThrows(ConstraintViolationException.class, () -> libroService.guardarLibro(invalido));

        assertEquals(1, libroRepository.count());
        assertEquals(1, stockPersistido());
    }

    private boolean intentarPrestamo(CountDownLatch preparados, CountDownLatch iniciar)
            throws InterruptedException {
        preparados.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("No se inició la prueba concurrente");
        }
        try {
            prestamoService.guardarPrestamo(solicitud());
            return true;
        } catch (SinEjemplaresException esperado) {
            return false;
        }
    }

    private PrestamoResponse devolverCuandoInicie(Long prestamoId, CountDownLatch preparados,
                                                  CountDownLatch iniciar) throws InterruptedException {
        preparados.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("No se inició la prueba de devoluciones concurrentes");
        }
        return prestamoService.devolverPrestamo(prestamoId);
    }

    private PrestamoRequest solicitud() {
        LocalDate fecha = LocalDate.of(2026, 11, 14);
        return new PrestamoRequest(fecha, fecha.plusDays(7), libroId, usuarioId);
    }

    private int stockPersistido() {
        return libroRepository.findById(libroId).orElseThrow().getStockDisponible();
    }
}
