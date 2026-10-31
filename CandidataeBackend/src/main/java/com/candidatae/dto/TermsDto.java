package com.candidatae.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Data Transfer Objects para el registro y consulta del consentimiento
 * de Términos y Condiciones (RGPD / LOPDGDD).
 */
public final class TermsDto {

    private TermsDto() {
    }

    @Schema(description = "Petición para registrar la aceptación o denegación de los términos y condiciones")
    public record TermsAcceptanceRequest(
            @NotNull(message = "El campo 'accepted' es obligatorio")
            @Schema(description = "Si el usuario acepta los términos y condiciones", example = "true")
            Boolean accepted,

            @Size(max = 20, message = "La versión no puede superar los 20 caracteres")
            @Schema(description = "Versión del documento legal aceptado", example = "1.0")
            String version
    ) {
    }

    @Schema(description = "Estado actual del consentimiento del usuario sobre los términos y condiciones")
    public record TermsStatusResponse(
            @Schema(description = "Si el usuario ha aceptado los términos vigentes", example = "true")
            boolean termsAccepted,

            @Schema(description = "Fecha y hora de aceptación", example = "2026-10-31T12:00:00Z")
            Instant termsAcceptedAt,

            @Schema(description = "Versión aceptada", example = "1.0")
            String termsVersion
    ) {
    }
}

