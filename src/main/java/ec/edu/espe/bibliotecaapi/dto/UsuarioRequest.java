package ec.edu.espe.bibliotecaapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "El nombre del usuario es obligatorio")
        @Size(max = 255, message = "El nombre admite hasta 255 caracteres") String nombre,
        @NotBlank(message = "La cedula es obligatoria")
        @Size(max = 255, message = "La cedula admite hasta 255 caracteres") String cedula,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato valido")
        @Size(max = 255, message = "El email admite hasta 255 caracteres") String email) {
}
