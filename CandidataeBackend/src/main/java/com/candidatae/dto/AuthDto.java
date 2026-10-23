package com.candidatae.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs de la autenticación de Candidatae.
 *
 * <p>Se agrupan en un único fichero porque los tres viajan siempre juntos: el
 * registro y el login consumen una petición y devuelven la misma respuesta.</p>
 *
 * <p><strong>Por qué varios {@code record} y no uno solo:</strong> cada uno es un
 * <em>contrato distinto</em>. El de entrada se deserializa de datos no fiables
 * (y por eso lleva validación), el de salida se serializa hacia el cliente. Si se
 * fusionaran en un único tipo, un campo opcional sin proteger podría acabar
 * viajando en la respuesta sin querer.</p>
 *
 * <p>Todos son {@code record}: inmutables, con equals/hashCode por defecto y sin
 * boilerplate.</p>
 *
 * <p>Ejemplo de JSON de la respuesta:</p>
 * <pre>{ "token": "eyJhbGciOiJIUzI1NiJ9...", "email": "ana@correo.com" }</pre>
 */
public final class AuthDto {

    private AuthDto() {
    }

    /**
     * Datos de alta de un usuario nuevo.
     *
     * <p>Ejemplo de JSON esperado:</p>
     * <pre>{ "email": "ana@correo.com", "password": "miPassword123", "name": "Ana" }</pre>
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
     *
     * <p>Ejemplo de JSON esperado:</p>
     * <pre>{ "email": "ana@correo.com", "password": "miPassword123" }</pre>
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
     *
     * <p>El cliente debe guardar el {@code token} y enviarlo en cada petición
     * posterior a endpoints protegidos, en la cabecera HTTP:</p>
     * <pre>Authorization: Bearer &lt;token&gt;</pre>
     *
     * <p>Fíjate en que NUNCA devolvemos la contraseña ni su hash: solo lo
     * imprescindible.</p>
     */
    public record Response(
            String token,
            String email
    ) {
    }
}
