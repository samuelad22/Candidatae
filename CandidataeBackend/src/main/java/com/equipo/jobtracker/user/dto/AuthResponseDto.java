package com.equipo.jobtracker.user.dto;

/**
 * Respuesta que devolvemos tras un registro o login correcto.
 *
 * <p>El cliente debe guardar el {@code token} y enviarlo en cada petición posterior
 * a endpoints protegidos, en la cabecera HTTP:</p>
 * <pre>Authorization: Bearer &lt;token&gt;</pre>
 *
 * <p>Fíjate en que NUNCA devolvemos la contraseña ni su hash: solo lo imprescindible.</p>
 *
 * Ejemplo de JSON generado:
 * <pre>{ "token": "eyJhbGciOiJIUzI1NiJ9...", "email": "ana@correo.com" }</pre>
 */
public record AuthResponseDto(
        String token,
        String email
) {
}
