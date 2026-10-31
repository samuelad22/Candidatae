package com.candidatae.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs de la autenticación de Candidatae.
 */
public final class AuthDto {

    private AuthDto() {
    }

    /**
     * Datos de alta de un usuario nuevo.
     */
    public record Register(

            @NotBlank(message = "Email is mandatory")
            @Email(message = "Invalid email format")
            @Size(max = 255, message = "Email cannot exceed 255 characters")
            String email,

            @NotBlank(message = "Password is mandatory")
            @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
            String password,

            @NotBlank(message = "Name is mandatory")
            @Size(max = 100, message = "Name cannot exceed 100 characters")
            String name
    ) {
    }

    /**
     * Datos para iniciar sesión.
     */
    public record Login(

            @NotBlank(message = "El email es obligatorio")
            @Email(message = "El email no tiene un formato válido")
            @Size(max = 255, message = "El email no puede superar los 255 caracteres")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            String password
    ) {
    }

    /**
     * Respuesta que devolvemos tras un registro o login correcto.
     */
    public record Response(
            String token,
            String email,
            boolean termsAccepted
    ) {
        public Response(String token, String email) {
            this(token, email, false);
        }
    }
}
