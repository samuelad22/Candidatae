package com.candidatae.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Formato único de error de la API.
 *
 * <p>Todas las excepciones de la aplicación se traducen a este JSON, de modo que el
 * frontend siempre recibe la misma estructura y no tiene que parsear textos sueltos.</p>
 *
 * <p>Ejemplo:</p>
 * <pre>{ "timestamp": "2026-10-20T10:15:00Z", "status": 404,
 *        "error": "Not Found", "message": "Cuenta de correo no encontrada",
 *        "path": "/api/email-accounts/9" }</pre>
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    /**
     * Crea una respuesta de error sin errores de campo (validación).
     */
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null);
    }

    /**
     * Crea una respuesta de error con el detalle de los campos que no han superado la validación.
     */
    public static ApiErrorResponse of(int status, String error, String message, String path,
                                      Map<String, String> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path,
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
    }
}
