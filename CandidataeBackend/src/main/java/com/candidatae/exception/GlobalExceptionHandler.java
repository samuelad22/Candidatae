package com.candidatae.exception;

import com.candidatae.dto.ApiErrorResponse;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.ValueInstantiationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejador global de excepciones: convierte cualquier error lancerado por los
 * controladores o servicios en una respuesta JSON homogénea.
 *
 * <p>Antes de existir esta clase, cada servicio lanzaba su propio
 * {@code ResponseStatusException} y Spring devolvía un cuerpo de error distinto
 * en cada caso. El frontend no tenía un formato estable con el que trabajar.</p>
 *
 * <p>Los errores de negocio llegan aquí como {@link ApiException} (una sola clase
 * con su status HTTP) y los del framework se tratan con handlers específicos. Todos
 * acaban en el mismo JSON: {@link ApiErrorResponse}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Cualquier error de negocio o de integración de la aplicación.
     *
     * <p>Un único handler para todas: lo que distingue un error de otro es su status
     * HTTP, y ese status lo lleva la propia excepción ({@link ApiException}).</p>
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex,
                                                               HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    /**
     * Mantiene el formato de error también para las excepciones de negocio que
     * lanza Spring Security o el propio framework (no solo las nuestras).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex,
                                                                  HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return build(status, message, request);
    }

    /**
     * Validación del cuerpo de la petición ({@code @Valid @RequestBody}).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(),
                    fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Valor inválido");
        }
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Hay datos inválidos en la petición",
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Validación de parámetros de método ({@code @Validated} + {@code @Min}, {@code @Max}, ...).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Hay datos inválidos en la petición",
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Spring Security lanza estas excepciones dentro del controlador. Si no las
     * capturamos aquí, el manejador genérico las convertiría en un 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "No tienes permiso para esta operación", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "No autenticado", request);
    }

    /**
     * Peticiones con un cuerpo JSON ilegible, ausente o con valores de Enum inválidos.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                   HttpServletRequest request) {
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException ifx && ifx.getTargetType() != null && ifx.getTargetType().isEnum()) {
            String fieldName = extractFieldName(ifx.getPath());
            String valor = ifx.getValue() != null ? ifx.getValue().toString() : "";
            String message = String.format("Valor '%s' no válido para el campo '%s'. Valores permitidos: %s",
                    valor, fieldName, Arrays.toString(ifx.getTargetType().getEnumConstants()));
            return build(HttpStatus.BAD_REQUEST, message, request);
        }

        if (cause instanceof ValueInstantiationException vie && vie.getType() != null && vie.getType().isEnumType()) {
            String fieldName = extractFieldName(vie.getPath());
            String valor = vie.getCause() != null && vie.getCause().getMessage() != null
                    ? vie.getCause().getMessage()
                    : "";
            String message = String.format("Valor '%s' no válido para el campo '%s'. Valores permitidos: %s",
                    valor, fieldName, Arrays.toString(vie.getType().getRawClass().getEnumConstants()));
            return build(HttpStatus.BAD_REQUEST, message, request);
        }

        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido", request);
    }

    private String extractFieldName(List<?> path) {
        if (path == null || path.isEmpty()) {
            return "campo";
        }
        Object lastRef = path.get(path.size() - 1);
        String refStr = lastRef.toString();
        int start = refStr.lastIndexOf("[\"");
        int end = refStr.lastIndexOf("\"]");
        if (start != -1 && end != -1 && end > start + 2) {
            return refStr.substring(start + 2, end);
        }
        return "campo";
    }

    /**
     * Falta un parámetro obligatorio de la URL (por ejemplo, el {@code code} o el
     * {@code state} del callback de Google). Sin este handler, el manejador genérico
     * lo convertiría en un 500.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'",
                request);
    }

    /**
     * Método HTTP no soportado por el endpoint (por ejemplo, PUT sobre un recurso
     * que solo admite GET y DELETE).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                       HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido", request);
    }

    /**
     * Ruta inexistente. Sin este handler, el manejador genérico la convertiría en un 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", request);
    }

    /**
     * Cualquier otra excepción es un fallo del servidor: se registra con su stack
     * trace completo y se devuelve un mensaje genérico (nunca detalles internos).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex,
                                                               HttpServletRequest request) {
        log.error("Error no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message,
                                                    HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
