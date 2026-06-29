package pe.bancoconfianza.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String nombre,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6) String password
) {}
