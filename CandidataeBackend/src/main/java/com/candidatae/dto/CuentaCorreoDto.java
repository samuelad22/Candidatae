package com.candidatae.dto;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.EstadoConexion;
import com.candidatae.domain.ProveedorCorreo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTOs de las cuentas de correo del usuario.
 *
 * <p>Se agrupan en un único fichero porque los cuatro son siempre del mismo
 * recurso: la cuenta de correo y sus operaciones (alta, edición, lectura y
 * vinculación con Gmail).</p>
 *
 * <p><strong>Por qué varios {@code record} y no uno solo:</strong> cada uno es un
 * <em>contrato distinto</em>.</p>
 * <ul>
 *   <li>{@link Create} y {@link Update} son de <strong>entrada</strong>: se deserializan
 *       de datos no fiables y por eso llevan validación. Sus reglas no coinciden
 *       (crear exige email y proveedor; editar admite campos sueltos), así que
 *       separarlos deja cada contrato con sus propias reglas, en vez de una bolsa
 *       de campos opcionales.</li>
 *   <li>{@link Response} es de <strong>salida</strong>, y es el que garantiza que el
 *       refresh token de Gmail nunca salga por la API.</li>
 *   <li>{@link GmailAuthorize} es la respuesta del inicio del flujo OAuth2.</li>
 * </ul>
 *
 * <p>Todos son {@code record}: inmutables, con equals/hashCode por defecto y sin
 * boilerplate.</p>
 */
public final class CuentaCorreoDto {

    private CuentaCorreoDto() {
    }

    /**
     * Alta manual de una cuenta de correo (sin pasar por el flujo OAuth de Google).
     *
     * <p>La cuenta nace en estado {@code PENDIENTE}: es una bandeja declarada por el
     * usuario que todavía no se ha autorizado. Sirve para que el usuario pueda tener
     * el registro aunque la vinculación automática tarde o falle.</p>
     */
    public record Create(

            @NotBlank(message = "El email de la cuenta es obligatorio")
            @Email(message = "El email de la cuenta no tiene un formato válido")
            @Size(max = 255, message = "El email de la cuenta no puede superar los 255 caracteres")
            String emailCuenta,

            @NotNull(message = "El proveedor es obligatorio")
            ProveedorCorreo proveedor
    ) {
    }

    /**
     * Edición de una cuenta de correo ya existente.
     *
     * <p>Todos los campos son opcionales: solo se aplica lo que viene informado.</p>
     *
     * <p>Restricciones de negocio (las aplica el servicio, no la validación de formato):</p>
     * <ul>
     *   <li>{@code estadoConexion} solo admite {@code PENDIENTE} o {@code EXPIRADA}: pasar a
     *       {@code ACTIVA} exige pasar por el flujo OAuth de Google (nunca a mano), y
     *       {@code DESCONECTADA} se hace con el DELETE.</li>
     *   <li>No se puede tocar el refresh token desde aquí.</li>
     * </ul>
     *
     * @param emailCuenta    nuevo email de la bandeja (opcional).
     * @param estadoConexion nuevo estado (opcional, con las restricciones de arriba).
     * @param resetCursor    si es {@code true}, borra el cursor de sincronización para que la
     *                       próxima lectura vuelva a empezar desde el principio (opcional).
     */
    public record Update(

            @Email(message = "El email de la cuenta no tiene un formato válido")
            @Size(max = 255, message = "El email de la cuenta no puede superar los 255 caracteres")
            String emailCuenta,

            EstadoConexion estadoConexion,

            Boolean resetCursor
    ) {
    }

    /**
     * Representación pública de una cuenta de correo.
     *
     * <p>NUNCA incluye el refresh token: es un secreto que da acceso al buzón completo
     * del usuario y no debe viajar al frontend ni quedar registrado en los logs.</p>
     *
     * <p>Ejemplo de JSON:</p>
     * <pre>{ "id": 1, "proveedor": "GMAIL", "emailCuenta": "ana@correo.com",
     *        "estadoConexion": "ACTIVA", "fechaConexion": "2026-10-20T10:15:00Z",
     *        "fechaDesconexion": null, "activa": true }</pre>
     */
    public record Response(
            Long id,
            String proveedor,
            String emailCuenta,
            String estadoConexion,
            Instant fechaConexion,
            Instant fechaDesconexion,
            boolean activa
    ) {

        /**
         * Convierte la entidad en su versión pública (sin datos sensibles).
         */
        public static Response from(CuentaCorreo cuenta) {
            return new Response(
                    cuenta.getId(),
                    cuenta.getProveedor().name(),
                    cuenta.getEmailCuenta(),
                    cuenta.getEstadoConexion().name(),
                    cuenta.getFechaConexion(),
                    cuenta.getFechaDesconexion(),
                    cuenta.getEstadoConexion() == EstadoConexion.ACTIVA);
        }
    }

    /**
     * Datos que el frontend necesita para iniciar la vinculación OAuth2 de Gmail.
     *
     * <p>El frontend no construye la URL de Google: pide este endpoint con su JWT y
     * redirige el navegador a {@code authorizationUrl}. Así el backend centraliza las
     * URLs, los scopes y el "state" de protección contra CSRF.</p>
     *
     * @param authorizationUrl URL de Google a la que debe redirigir el navegador.
     * @param state            valor de protección anti-CSRF (viaja en la propia URL).
     */
    public record OutlookAuthorize(
            String authorizationUrl,
            String state
    ) {
    }

    public record GmailAuthorize(
            String authorizationUrl,
            String state
    ) {
    }
}
