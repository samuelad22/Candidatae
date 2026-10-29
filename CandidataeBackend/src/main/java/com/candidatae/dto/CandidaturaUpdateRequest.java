package com.candidatae.dto;

import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.Modalidad;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Petición de entrada para actualizar una candidatura existente (PUT).
 * Los campos presentes (no nulos) se modificarán.
 */
@Schema(description = "Datos para editar una candidatura existente")
public record CandidaturaUpdateRequest(

        @Schema(description = "Nombre de la empresa que publica la oferta", example = "Google España S.L.")
        @Size(max = 255, message = "El nombre de la empresa no puede superar los 255 caracteres")
        @JsonAlias({"empresaNombre", "nombreEmpresa"})
        String empresa,

        @Schema(description = "Código de la plataforma de empleo. Valores permitidos: LINKEDIN, INFOJOBS, INDEED, TECNOEMPLEO, GLASSDOOR", example = "LINKEDIN")
        @Size(max = 50, message = "El código de plataforma no puede superar los 50 caracteres")
        String plataformaCodigo,

        @Schema(description = "Título o puesto de trabajo ofertado", example = "Junior Backend Developer")
        @Size(max = 255, message = "El puesto no puede superar los 255 caracteres")
        String puesto,

        @Schema(description = "Ubicación geográfica o ciudad del puesto", example = "Madrid")
        @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres")
        String ubicacion,

        @Schema(description = "Modalidad de trabajo. Valores permitidos: PRESENCIAL, HIBRIDO, REMOTO", example = "HIBRIDO")
        Modalidad modalidad,

        @Schema(description = "Rango o retribución salarial aproximada", example = "28.000€ - 32.000€")
        @Size(max = 100, message = "El salario no puede superar los 100 caracteres")
        String salarioTexto,

        @Schema(description = "Enlace web directo a la oferta de trabajo", example = "https://linkedin.com/jobs/view/123")
        String enlaceOferta,

        @Schema(description = "Fecha en que se envió la solicitud (formato ISO YYYY-MM-DD)", example = "2026-10-27")
        LocalDate fechaSolicitud,

        @Schema(description = "Estado actual en el proceso. Valores permitidos: ENVIADA, EN_PROCESO, ENTREVISTA, OFERTA, RECHAZADA, DESCARTADA", example = "ENTREVISTA")
        EstadoCandidatura estadoActual
) {
}
