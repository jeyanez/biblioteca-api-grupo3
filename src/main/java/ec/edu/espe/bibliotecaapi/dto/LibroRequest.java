package ec.edu.espe.bibliotecaapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LibroRequest(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 255, message = "El titulo admite hasta 255 caracteres") String titulo,
        @NotBlank(message = "El ISBN es obligatorio")
        @Size(max = 32, message = "El ISBN admite hasta 32 caracteres") String isbn,
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo") Integer stockDisponible,
        @NotNull(message = "El autor es obligatorio")
        @Positive(message = "El identificador del autor debe ser positivo") Long autorId) {
}
