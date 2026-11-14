package ec.edu.espe.bibliotecaapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** La fecha de devolucion es la fecha prevista para devolver el ejemplar. */
public record PrestamoRequest(
        @NotNull(message = "La fecha del prestamo es obligatoria") LocalDate fechaPrestamo,
        @NotNull(message = "La fecha de devolucion es obligatoria") LocalDate fechaDevolucion,
        @NotNull(message = "El libro es obligatorio")
        @Positive(message = "El identificador del libro debe ser positivo") Long libroId,
        @NotNull(message = "El usuario es obligatorio")
        @Positive(message = "El identificador del usuario debe ser positivo") Long usuarioId) {
}
