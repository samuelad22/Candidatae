package com.candidatae.security;

import com.candidatae.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Devuelve un 403 con el formato JSON de error de la API.
 *
 * <p>Complementa a {@link JsonAuthenticationEntryPoint}: el 403 significa "estás
 * autenticado pero no tienes permiso", el 401 "no estás autenticado".</p>
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "No tienes permiso para esta operación",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
