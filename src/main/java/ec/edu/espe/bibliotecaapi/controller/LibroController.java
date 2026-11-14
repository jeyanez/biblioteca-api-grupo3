package ec.edu.espe.bibliotecaapi.controller;

import ec.edu.espe.bibliotecaapi.dto.LibroRequest;
import ec.edu.espe.bibliotecaapi.dto.LibroResponse;
import ec.edu.espe.bibliotecaapi.service.LibroService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/libros")
public class LibroController {
    private final LibroService service;

    public LibroController(LibroService service) {
        this.service = service;
    }

    @GetMapping
    public List<LibroResponse> listar() {
        return service.listarLibros();
    }

    @GetMapping("/disponibles")
    public List<LibroResponse> disponibles() {
        return service.listarDisponibles();
    }

    @GetMapping("/{id}")
    public LibroResponse obtener(@PathVariable Long id) {
        return service.obtenerLibro(id);
    }

    @PostMapping
    public ResponseEntity<LibroResponse> crear(@Valid @RequestBody LibroRequest request) {
        LibroResponse libro = service.guardarLibro(request);
        return ResponseEntity.created(URI.create("/api/libros/" + libro.id())).body(libro);
    }

    @PutMapping("/{id}")
    public LibroResponse actualizar(@PathVariable Long id, @Valid @RequestBody LibroRequest request) {
        return service.actualizarLibro(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarLibro(id);
        return ResponseEntity.noContent().build();
    }
}
