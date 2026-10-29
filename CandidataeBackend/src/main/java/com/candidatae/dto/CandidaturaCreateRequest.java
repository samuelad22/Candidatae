package com.candidatae.dto;

import com.candidatae.domain.Modalidad;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Petición de entrada para dar de alta manualmente una nueva candidatura.
 * Contiene los datos de la oferta y la empresa introducidos por el usuario.
 */
@Schema(description = "Datos para dar de alta una nueva candidatura de empleo")
public record CandidaturaCreateRequest(

        @Schema(description = "Nombre de la empresa a la que se postula", example = "Google España S.L.")
        @NotBlank(message = "El nombre de la empresa es obligatorio")
        @Size(max = 255, message = "El nombre de la empresa no puede superar los 255 caracteres")
        @JsonAlias({"empresaNombre", "nombreEmpresa"})
        String empresa,

        @Schema(description = "Título o puesto de trabajo ofertado", example = "Junior Backend Developer")
        @NotBlank(message = "El puesto es obligatorio")
        @Size(max = 255, message = "El puesto no puede superar los 255 caracteres")
        String puesto,

        @Schema(description = "Código de la plataforma de empleo de origen. Valores permitidos: LINKEDIN, INFOJOBS, INDEED, TECNOEMPLEO, GLASSDOOR", example = "LINKEDIN")
        @Size(max = 50, message = "El código de plataforma no puede superar los 50 caracteres")
        String plataformaCodigo,

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
        @NotNull(message = "La fecha de solicitud es obligatoria")
        LocalDate fechaSolicitud
) {
}
