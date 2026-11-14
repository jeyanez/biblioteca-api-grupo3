package ec.edu.espe.bibliotecaapi.controller;

import ec.edu.espe.bibliotecaapi.dto.PrestamoRequest;
import ec.edu.espe.bibliotecaapi.dto.PrestamoResponse;
import ec.edu.espe.bibliotecaapi.service.PrestamoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {
    private final PrestamoService service;

    public PrestamoController(PrestamoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrestamoResponse> listar() {
        return service.listarPrestamos();
    }

    @GetMapping("/pendientes")
    public List<PrestamoResponse> pendientes() {
        return service.listarPendientes();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<PrestamoResponse> porUsuario(@PathVariable Long usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }

    @GetMapping("/{id}")
    public PrestamoResponse obtener(@PathVariable Long id) {
        return service.obtenerPrestamo(id);
    }

    @PostMapping
    public ResponseEntity<PrestamoResponse> crear(@Valid @RequestBody PrestamoRequest request) {
        PrestamoResponse prestamo = service.guardarPrestamo(request);
        return ResponseEntity.created(URI.create("/api/prestamos/" + prestamo.id())).body(prestamo);
    }

    @PutMapping("/{id}/devolucion")
    public PrestamoResponse devolver(@PathVariable Long id) {
        return service.devolverPrestamo(id);
    }
}
