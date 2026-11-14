package ec.edu.espe.bibliotecaapi.dto;

import java.time.LocalDate;

public record PrestamoResponse(Long id, LocalDate fechaPrestamo, LocalDate fechaDevolucion,
                               String estado, Long libroId, Long usuarioId) {
}
