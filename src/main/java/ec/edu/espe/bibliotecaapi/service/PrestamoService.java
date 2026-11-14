package ec.edu.espe.bibliotecaapi.service;

import ec.edu.espe.bibliotecaapi.dto.PrestamoRequest;
import ec.edu.espe.bibliotecaapi.dto.PrestamoResponse;
import ec.edu.espe.bibliotecaapi.entity.Libro;
import ec.edu.espe.bibliotecaapi.entity.Prestamo;
import ec.edu.espe.bibliotecaapi.entity.Usuario;
import ec.edu.espe.bibliotecaapi.exception.LibroNoEncontradoException;
import ec.edu.espe.bibliotecaapi.exception.PrestamoInvalidoException;
import ec.edu.espe.bibliotecaapi.exception.RecursoNoEncontradoException;
import ec.edu.espe.bibliotecaapi.exception.ReglaNegocioException;
import ec.edu.espe.bibliotecaapi.exception.SinEjemplaresException;
import ec.edu.espe.bibliotecaapi.repository.LibroRepository;
import ec.edu.espe.bibliotecaapi.repository.PrestamoRepository;
import ec.edu.espe.bibliotecaapi.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class PrestamoService {
    public static final String PENDIENTE = "PENDIENTE";
    public static final String DEVUELTO = "DEVUELTO";

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;

    public PrestamoService(PrestamoRepository prestamoRepository,
                           LibroRepository libroRepository,
                           UsuarioRepository usuarioRepository) {
        this.prestamoRepository = prestamoRepository;
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<PrestamoResponse> listarPrestamos() {
        return prestamoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PrestamoResponse obtenerPrestamo(@NotNull @Positive Long id) {
        return toResponse(prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el préstamo con id " + id)));
    }

    public List<PrestamoResponse> listarPorUsuario(@NotNull @Positive Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException("No existe el usuario con id " + usuarioId);
        }
        return prestamoRepository.findByUsuarioId(usuarioId).stream().map(this::toResponse).toList();
    }

    public List<PrestamoResponse> listarPendientes() {
        return prestamoRepository.findByEstado(PENDIENTE).stream().map(this::toResponse).toList();
    }

    /** La fecha de devolución del DTO es la fecha prevista, no la fecha real. */
    @Transactional
    public PrestamoResponse guardarPrestamo(@NotNull @Valid PrestamoRequest request) {
        if (request.fechaPrestamo() == null || request.fechaDevolucion() == null
                || !request.fechaDevolucion().isAfter(request.fechaPrestamo())) {
            throw new PrestamoInvalidoException("La fecha de devolución debe ser posterior a la fecha de préstamo");
        }
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + request.usuarioId()));
        // El bloqueo se mantiene hasta terminar la transacción: no se presta dos veces el último ejemplar.
        Libro libro = libroRepository.findByIdForUpdate(request.libroId())
                .orElseThrow(() -> new LibroNoEncontradoException(request.libroId()));
        if (libro.getStockDisponible() == null || libro.getStockDisponible() <= 0) {
            throw new SinEjemplaresException(libro.getId());
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setFechaPrestamo(request.fechaPrestamo());
        prestamo.setFechaDevolucion(request.fechaDevolucion());
        prestamo.setEstado(PENDIENTE);
        prestamo.setLibro(libro);
        prestamo.setUsuario(usuario);
        libro.setStockDisponible(libro.getStockDisponible() - 1);
        libroRepository.save(libro);
        // Si falla este guardado, también se revierte el descuento de stock.
        return toResponse(prestamoRepository.save(prestamo));
    }

    @Transactional
    public PrestamoResponse devolverPrestamo(@NotNull @Positive Long id) {
        Prestamo prestamo = prestamoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el préstamo con id " + id));
        if (!PENDIENTE.equals(prestamo.getEstado())) {
            throw new PrestamoInvalidoException("Solo se puede devolver un préstamo pendiente");
        }
        Libro libro = libroRepository.findByIdForUpdate(prestamo.getLibro().getId())
                .orElseThrow(() -> new LibroNoEncontradoException(prestamo.getLibro().getId()));
        Integer stock = libro.getStockDisponible();
        if (stock == null || stock < 0 || stock == Integer.MAX_VALUE) {
            throw new ReglaNegocioException("El stock del libro no permite registrar la devolución");
        }
        libro.setStockDisponible(stock + 1);
        prestamo.setEstado(DEVUELTO);
        libroRepository.save(libro);
        return toResponse(prestamoRepository.save(prestamo));
    }

    private PrestamoResponse toResponse(Prestamo prestamo) {
        return new PrestamoResponse(prestamo.getId(), prestamo.getFechaPrestamo(),
                prestamo.getFechaDevolucion(), prestamo.getEstado(),
                prestamo.getLibro().getId(), prestamo.getUsuario().getId());
    }
}
