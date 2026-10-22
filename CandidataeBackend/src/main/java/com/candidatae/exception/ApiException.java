package com.candidatae.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción única de la aplicación para los errores de negocio y de integración.
 *
 * <p><strong>Por qué una sola clase y no cinco:</strong> todas las excepciones de
 * negocio compartían exactamente la misma estructura (un mensaje y un status HTTP) y
 * el {@link GlobalExceptionHandler} las traducía con el mismo código, una por una.
 * Lo que de verdad distingue un error de otro es <em>el status</em>, no la clase: el
 * manejador despacha por status, y ningún servicio captura estas excepciones por
 * tipo. Con una única clase, la taxonomía de errores cabe en una pantalla y sigue
 * siendo explícita en cada punto donde se lanza.</p>
 *
 * <p>El mensaje está pensado para el usuario final: explica qué ha pasado sin
 * filtrar detalles técnicos. La causa original se registra en el log, nunca en la
 * respuesta HTTP.</p>
 *
 * <p>Uso:</p>
 * <pre>
 * throw ApiException.notFound("Cuenta de correo no encontrada");      // 404
 * throw ApiException.conflict("El email ya está registrado");          // 409
 * throw ApiException.badRequest("Solo se puede marcar como EXPIRADA");  // 400
 * throw ApiException.unauthorized("Credenciales inválidas");            // 401
 * throw ApiException.oauth("Google ha rechazado la vinculación");      // 503
 * </pre>
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Petición sintácticamente válida cuya lógica de negocio no permite la operación (400).
     *
     * <p>Ejemplo: intentar pasar una cuenta de correo a estado ACTIVA sin pasar por
     * el flujo OAuth de Google.</p>
     */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Petición sin una sesión válida (401).
     *
     * <p>Ejemplos: credenciales incorrectas al iniciar sesión, o un token JWT
     * caducado o ausente en un endpoint protegido.</p>
     */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Recurso que no existe o que no pertenece al usuario autenticado (404).
     *
     * <p>Se usa sobre todo para el aislamiento de datos: si un usuario pide una
     * cuenta de correo que no es suya, la respuesta es 404 (y no 403) para no revelar
     * que ese recurso existe en el sistema.</p>
     */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Conflicto con el estado actual del recurso (409).
     *
     * <p>Ejemplos: el usuario intenta vincular dos veces la misma bandeja de correo,
     * o existe ya una cuenta con ese email para el mismo usuario.</p>
     */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    /**
     * Fallo en una integración con un proveedor externo (503).
     *
     * <p>Ejemplos: Google rechaza el código de autorización, no devuelve un refresh
     * token, o el servidor no está configurado con las credenciales de OAuth2.</p>
     */
    public static ApiException oauth(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
