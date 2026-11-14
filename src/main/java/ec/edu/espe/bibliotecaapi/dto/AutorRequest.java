package ec.edu.espe.bibliotecaapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AutorRequest(
        @NotBlank(message = "El nombre del autor es obligatorio")
        @Size(max = 255, message = "El nombre admite hasta 255 caracteres") String nombre,
        @NotBlank(message = "La nacionalidad es obligatoria")
        @Size(max = 255, message = "La nacionalidad admite hasta 255 caracteres") String nacionalidad) {
}
