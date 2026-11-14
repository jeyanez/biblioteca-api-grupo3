package ec.edu.espe.bibliotecaapi.controller;

import ec.edu.espe.bibliotecaapi.dto.AutorRequest;
import ec.edu.espe.bibliotecaapi.dto.AutorResponse;
import ec.edu.espe.bibliotecaapi.service.AutorService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autores")
public class AutorController {
    private final AutorService service;

    public AutorController(AutorService service) {
        this.service = service;
    }

    @GetMapping
    public List<AutorResponse> listar() {
        return service.listarAutores();
    }

    @GetMapping("/{id}")
    public AutorResponse obtener(@PathVariable Long id) {
        return service.obtenerAutor(id);
    }

    @PostMapping
    public ResponseEntity<AutorResponse> crear(@Valid @RequestBody AutorRequest request) {
        AutorResponse autor = service.guardarAutor(request);
        return ResponseEntity.created(URI.create("/api/autores/" + autor.id())).body(autor);
    }

    @PutMapping("/{id}")
    public AutorResponse actualizar(@PathVariable Long id, @Valid @RequestBody AutorRequest request) {
        return service.actualizarAutor(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarAutor(id);
        return ResponseEntity.noContent().build();
    }
}
