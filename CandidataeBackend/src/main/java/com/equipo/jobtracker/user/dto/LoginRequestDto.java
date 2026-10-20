package com.equipo.jobtracker.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos que envía el cliente para iniciar sesión.
 *
 * <p>Es un {@code record} por los mismos motivos explicados en {@link RegistroRequestDto}:
 * inmutable, conciso y pensado exclusivamente para transportar datos.</p>
 *
 * Ejemplo de JSON esperado:
 * <pre>{ "email": "ana@correo.com", "password": "miPassword123" }</pre>
 */
public record LoginRequestDto(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
