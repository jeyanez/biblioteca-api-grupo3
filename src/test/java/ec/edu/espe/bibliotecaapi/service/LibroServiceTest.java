package ec.edu.espe.bibliotecaapi.service;

import ec.edu.espe.bibliotecaapi.dto.LibroRequest;
import ec.edu.espe.bibliotecaapi.dto.LibroResponse;
import ec.edu.espe.bibliotecaapi.entity.Autor;
import ec.edu.espe.bibliotecaapi.entity.Libro;
import ec.edu.espe.bibliotecaapi.exception.IsbnDuplicadoException;
import ec.edu.espe.bibliotecaapi.exception.ReglaNegocioException;
import ec.edu.espe.bibliotecaapi.repository.AutorRepository;
import ec.edu.espe.bibliotecaapi.repository.LibroRepository;
import ec.edu.espe.bibliotecaapi.repository.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibroServiceTest {
    private static final String ISBN = "9780307474728";
    @Mock private LibroRepository libroRepository;
    @Mock private AutorRepository autorRepository;
    @Mock private PrestamoRepository prestamoRepository;
    private LibroService service;
    private Autor autor;
    private Libro libro;

    @BeforeEach
    void prepararServicio() {
        service = new LibroService(libroRepository, autorRepository, prestamoRepository);
        autor = new Autor();
        autor.setId(1L);
        autor.setNombre("Gabriel García Márquez");
        libro = new Libro();
        libro.setId(10L);
        libro.setTitulo("Cien años de soledad");
        libro.setIsbn(ISBN);
        libro.setStockDisponible(3);
        libro.setAutor(autor);
    }

    @Test
    void registrarIsbnDuplicadoNoGuardaUnSegundoLibro() {
        when(libroRepository.existsByIsbn(ISBN)).thenReturn(true);
        lenient().when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));

        assertThrows(IsbnDuplicadoException.class, () -> service.guardarLibro(solicitud(ISBN)));

        verify(libroRepository, never()).saveAndFlush(any(Libro.class));
    }

    @Test
    void registroValidoRelacionaElAutorYConservaStockInicial() {
        when(libroRepository.existsByIsbn(ISBN)).thenReturn(false);
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));
        when(libroRepository.saveAndFlush(any(Libro.class))).thenAnswer(invocation -> {
            Libro nuevo = invocation.getArgument(0);
            nuevo.setId(10L);
            assertSame(autor, nuevo.getAutor());
            return nuevo;
        });

        LibroResponse resultado = service.guardarLibro(solicitud(ISBN));

        assertEquals(ISBN, resultado.isbn());
        assertEquals(3, resultado.stockDisponible());
        assertEquals(1L, resultado.autorId());
        verify(libroRepository).saveAndFlush(any(Libro.class));
    }

    @Test
    void actualizarPermiteConservarElIsbnDelMismoLibro() {
        when(libroRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(libro));
        when(libroRepository.existsByIsbnAndIdNot(ISBN, 10L)).thenReturn(false);
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));
        when(libroRepository.saveAndFlush(libro)).thenReturn(libro);

        LibroResponse resultado = service.actualizarLibro(10L,
                new LibroRequest("Título corregido", ISBN, 4, 1L));

        assertEquals("Título corregido", resultado.titulo());
        assertEquals(4, resultado.stockDisponible());
        verify(libroRepository).existsByIsbnAndIdNot(ISBN, 10L);
        verify(libroRepository).saveAndFlush(libro);
    }

    @Test
    void actualizarRechazaElIsbnDeOtroLibroSinModificarElOriginal() {
        String otroIsbn = "9788497592208";
        when(libroRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(libro));
        when(libroRepository.existsByIsbnAndIdNot(otroIsbn, 10L)).thenReturn(true);
        lenient().when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));

        assertThrows(IsbnDuplicadoException.class,
                () -> service.actualizarLibro(10L, solicitud(otroIsbn)));

        assertEquals(ISBN, libro.getIsbn());
        assertEquals(3, libro.getStockDisponible());
        verify(libroRepository, never()).saveAndFlush(any(Libro.class));
    }

    @Test
    void eliminarLibroConPrestamosProtegeElHistorial() {
        lenient().when(libroRepository.findById(10L)).thenReturn(Optional.of(libro));
        lenient().when(libroRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(libro));
        when(prestamoRepository.existsByLibroId(10L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.eliminarLibro(10L));

        verify(libroRepository, never()).delete(any(Libro.class));
        verify(libroRepository, never()).deleteById(any());
    }

    private LibroRequest solicitud(String isbn) {
        return new LibroRequest("Cien años de soledad", isbn, 3, 1L);
    }
}
