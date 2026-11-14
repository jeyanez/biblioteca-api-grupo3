package ec.edu.espe.bibliotecaapi.service;

import ec.edu.espe.bibliotecaapi.dto.LibroRequest;
import ec.edu.espe.bibliotecaapi.dto.LibroResponse;
import ec.edu.espe.bibliotecaapi.entity.Autor;
import ec.edu.espe.bibliotecaapi.entity.Libro;
import ec.edu.espe.bibliotecaapi.exception.IsbnDuplicadoException;
import ec.edu.espe.bibliotecaapi.exception.LibroNoEncontradoException;
import ec.edu.espe.bibliotecaapi.exception.RecursoNoEncontradoException;
import ec.edu.espe.bibliotecaapi.exception.ReglaNegocioException;
import ec.edu.espe.bibliotecaapi.repository.AutorRepository;
import ec.edu.espe.bibliotecaapi.repository.LibroRepository;
import ec.edu.espe.bibliotecaapi.repository.PrestamoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class LibroService {
    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final PrestamoRepository prestamoRepository;

    public LibroService(LibroRepository libroRepository, AutorRepository autorRepository,
                        PrestamoRepository prestamoRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.prestamoRepository = prestamoRepository;
    }

    public List<LibroResponse> listarLibros() {
        return libroRepository.findAll().stream().map(LibroService::toResponse).toList();
    }

    public List<LibroResponse> listarDisponibles() {
        return libroRepository.findByStockDisponibleGreaterThan(0).stream()
                .map(LibroService::toResponse).toList();
    }

    public LibroResponse obtenerLibro(@NotNull @Positive Long id) {
        return toResponse(libroRepository.findById(id)
                .orElseThrow(() -> new LibroNoEncontradoException(id)));
    }

    @Transactional
    public LibroResponse guardarLibro(@NotNull @Valid LibroRequest request) {
        String isbn = normalizarIsbn(request.isbn());
        if (libroRepository.existsByIsbn(isbn)) {
            throw new IsbnDuplicadoException(isbn);
        }
        Autor autor = buscarAutor(request.autorId());
        Libro libro = new Libro();
        aplicarDatos(libro, request, isbn, autor);
        return toResponse(libroRepository.saveAndFlush(libro));
    }

    @Transactional
    public LibroResponse actualizarLibro(@NotNull @Positive Long id, @NotNull @Valid LibroRequest request) {
        // Comparte el bloqueo con prestamos/devoluciones para serializar cambios del stock.
        Libro libro = libroRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new LibroNoEncontradoException(id));
        String isbn = normalizarIsbn(request.isbn());
        if (libroRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new IsbnDuplicadoException(isbn);
        }
        Autor autor = buscarAutor(request.autorId());
        aplicarDatos(libro, request, isbn, autor);
        return toResponse(libroRepository.saveAndFlush(libro));
    }

    @Transactional
    public void eliminarLibro(@NotNull @Positive Long id) {
        Libro libro = libroRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new LibroNoEncontradoException(id));
        if (prestamoRepository.existsByLibroId(id)) {
            throw new ReglaNegocioException("No se puede eliminar un libro que tiene prestamos registrados");
        }
        libroRepository.delete(libro);
        libroRepository.flush();
    }

    private Autor buscarAutor(Long id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el autor con id " + id));
    }

    private static String normalizarIsbn(String isbn) {
        return isbn.trim().toUpperCase(Locale.ROOT);
    }

    private static void aplicarDatos(Libro libro, LibroRequest request, String isbn, Autor autor) {
        libro.setTitulo(request.titulo().trim());
        libro.setIsbn(isbn);
        libro.setStockDisponible(request.stockDisponible());
        libro.setAutor(autor);
    }

    private static LibroResponse toResponse(Libro libro) {
        return new LibroResponse(libro.getId(), libro.getTitulo(), libro.getIsbn(),
                libro.getStockDisponible(), libro.getAutor().getId());
    }
}
