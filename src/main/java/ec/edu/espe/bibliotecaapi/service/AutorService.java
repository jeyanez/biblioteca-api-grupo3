package ec.edu.espe.bibliotecaapi.service;

import ec.edu.espe.bibliotecaapi.dto.AutorRequest;
import ec.edu.espe.bibliotecaapi.dto.AutorResponse;
import ec.edu.espe.bibliotecaapi.entity.Autor;
import ec.edu.espe.bibliotecaapi.exception.RecursoNoEncontradoException;
import ec.edu.espe.bibliotecaapi.exception.ReglaNegocioException;
import ec.edu.espe.bibliotecaapi.repository.AutorRepository;
import ec.edu.espe.bibliotecaapi.repository.LibroRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class AutorService {
    private final AutorRepository autorRepository;
    private final LibroRepository libroRepository;

    public AutorService(AutorRepository autorRepository, LibroRepository libroRepository) {
        this.autorRepository = autorRepository;
        this.libroRepository = libroRepository;
    }

    public List<AutorResponse> listarAutores() {
        return autorRepository.findAll().stream().map(AutorService::toResponse).toList();
    }

    public AutorResponse obtenerAutor(@NotNull @Positive Long id) {
        return toResponse(buscarAutor(id));
    }

    @Transactional
    public AutorResponse guardarAutor(@NotNull @Valid AutorRequest request) {
        Autor autor = new Autor();
        aplicarDatos(autor, request);
        return toResponse(autorRepository.saveAndFlush(autor));
    }

    @Transactional
    public AutorResponse actualizarAutor(@NotNull @Positive Long id, @NotNull @Valid AutorRequest request) {
        Autor autor = buscarAutor(id);
        aplicarDatos(autor, request);
        return toResponse(autorRepository.saveAndFlush(autor));
    }

    @Transactional
    public void eliminarAutor(@NotNull @Positive Long id) {
        Autor autor = buscarAutor(id);
        if (libroRepository.existsByAutorId(id)) {
            throw new ReglaNegocioException("No se puede eliminar un autor que tiene libros registrados");
        }
        autorRepository.delete(autor);
        autorRepository.flush();
    }

    private Autor buscarAutor(Long id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el autor con id " + id));
    }

    private static void aplicarDatos(Autor autor, AutorRequest request) {
        autor.setNombre(request.nombre().trim());
        autor.setNacionalidad(request.nacionalidad().trim());
    }

    private static AutorResponse toResponse(Autor autor) {
        return new AutorResponse(autor.getId(), autor.getNombre(), autor.getNacionalidad());
    }
}
