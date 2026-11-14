package ec.edu.espe.bibliotecaapi.service;

import ec.edu.espe.bibliotecaapi.dto.PrestamoRequest;
import ec.edu.espe.bibliotecaapi.dto.PrestamoResponse;
import ec.edu.espe.bibliotecaapi.entity.Libro;
import ec.edu.espe.bibliotecaapi.entity.Prestamo;
import ec.edu.espe.bibliotecaapi.entity.Usuario;
import ec.edu.espe.bibliotecaapi.exception.LibroNoEncontradoException;
import ec.edu.espe.bibliotecaapi.exception.PrestamoInvalidoException;
import ec.edu.espe.bibliotecaapi.exception.RecursoNoEncontradoException;
import ec.edu.espe.bibliotecaapi.exception.SinEjemplaresException;
import ec.edu.espe.bibliotecaapi.repository.LibroRepository;
import ec.edu.espe.bibliotecaapi.repository.PrestamoRepository;
import ec.edu.espe.bibliotecaapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 14);

    @Mock private PrestamoRepository prestamoRepository;
    @Mock private LibroRepository libroRepository;
    @Mock private UsuarioRepository usuarioRepository;
    private PrestamoService service;
    private Libro libro;
    private Usuario usuario;

    @BeforeEach
    void prepararServicio() {
        service = new PrestamoService(prestamoRepository, libroRepository, usuarioRepository);
        libro = new Libro();
        libro.setId(10L);
        libro.setStockDisponible(2);
        usuario = new Usuario();
        usuario.setId(20L);
    }

    @Test
    void guardarPrestamoDescuentaUnEjemplarYLoMarcaPendiente() {
        referenciasExistentes();
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocation -> {
            Prestamo prestamo = invocation.getArgument(0);
            prestamo.setId(30L);
            return prestamo;
        });

        PrestamoResponse resultado = service.guardarPrestamo(solicitud(FECHA.plusDays(7)));

        assertEquals(1, libro.getStockDisponible());
        assertEquals(30L, resultado.id());
        assertEquals("PENDIENTE", resultado.estado());
        assertEquals(10L, resultado.libroId());
        assertEquals(20L, resultado.usuarioId());
        ArgumentCaptor<Prestamo> guardado = ArgumentCaptor.forClass(Prestamo.class);
        verify(prestamoRepository).save(guardado.capture());
        assertSame(libro, guardado.getValue().getLibro());
        assertSame(usuario, guardado.getValue().getUsuario());
        assertEquals(FECHA.plusDays(7), guardado.getValue().getFechaDevolucion());
        verify(libroRepository).save(libro);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rechazaPrestamoSinEjemplaresSinPersistir(int stock) {
        referenciasExistentes();
        libro.setStockDisponible(stock);

        assertThrows(SinEjemplaresException.class,
                () -> service.guardarPrestamo(solicitud(FECHA.plusDays(1))));

        assertEquals(stock, libro.getStockDisponible());
        verificarSinEscrituras();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rechazaDevolucionPrevistaIgualOAnteriorAlPrestamo(int dias) {
        assertThrows(PrestamoInvalidoException.class,
                () -> service.guardarPrestamo(solicitud(FECHA.plusDays(dias))));

        assertEquals(2, libro.getStockDisponible());
        verificarSinEscrituras();
    }

    @Test
    void rechazaLibroInexistente() {
        lenient().when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
        when(libroRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(LibroNoEncontradoException.class,
                () -> service.guardarPrestamo(solicitud(FECHA.plusDays(1))));

        verificarSinEscrituras();
    }

    @Test
    void rechazaUsuarioInexistenteSinDescontarStock() {
        lenient().when(libroRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(libro));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.guardarPrestamo(solicitud(FECHA.plusDays(1))));

        assertEquals(2, libro.getStockDisponible());
        verificarSinEscrituras();
    }

    @Test
    void devolverPrestamoReponeUnSoloEjemplar() {
        Prestamo prestamo = prestamo("PENDIENTE");
        when(prestamoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(prestamo));
        when(libroRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(libro));
        when(prestamoRepository.save(prestamo)).thenReturn(prestamo);

        PrestamoResponse resultado = service.devolverPrestamo(30L);

        assertEquals(3, libro.getStockDisponible());
        assertEquals("DEVUELTO", resultado.estado());
        assertEquals("DEVUELTO", prestamo.getEstado());
        verify(libroRepository).save(libro);
        verify(prestamoRepository).save(prestamo);
    }

    @Test
    void segundaDevolucionNoDuplicaStock() {
        Prestamo prestamo = prestamo("DEVUELTO");
        when(prestamoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(prestamo));

        assertThrows(PrestamoInvalidoException.class, () -> service.devolverPrestamo(30L));

        assertEquals(2, libro.getStockDisponible());
        verificarSinEscrituras();
    }

    @Test
    void consultaPendientesUsaElEstadoPendienteYDevuelveDtos() {
        when(prestamoRepository.findByEstado("PENDIENTE"))
                .thenReturn(List.of(prestamo("PENDIENTE")));

        List<PrestamoResponse> pendientes = service.listarPendientes();

        assertEquals(1, pendientes.size());
        assertEquals("PENDIENTE", pendientes.getFirst().estado());
        verify(prestamoRepository).findByEstado("PENDIENTE");
    }

    private void referenciasExistentes() {
        // El orden de búsqueda de referencias no forma parte del contrato de negocio.
        lenient().when(libroRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(libro));
        lenient().when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuario));
    }

    private PrestamoRequest solicitud(LocalDate devolucion) {
        return new PrestamoRequest(FECHA, devolucion, 10L, 20L);
    }

    private Prestamo prestamo(String estado) {
        Prestamo prestamo = new Prestamo();
        prestamo.setId(30L);
        prestamo.setFechaPrestamo(FECHA);
        prestamo.setFechaDevolucion(FECHA.plusDays(7));
        prestamo.setEstado(estado);
        prestamo.setLibro(libro);
        prestamo.setUsuario(usuario);
        return prestamo;
    }

    private void verificarSinEscrituras() {
        verify(prestamoRepository, never()).save(any(Prestamo.class));
        verify(libroRepository, never()).save(any(Libro.class));
    }
}
