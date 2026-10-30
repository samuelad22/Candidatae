package com.candidatae.dto;

import com.candidatae.domain.Plataforma;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Objects para la gestion y consulta de Plataformas de empleo.
 */
public final class PlataformaDto {

    private PlataformaDto() {
    }

    /**
     * Respuesta publica con los datos de una plataforma de empleo.
     */
    @Schema(description = "Datos de una plataforma o portal de empleo")
    public record PlataformaResponse(
            @Schema(description = "Identificador unico de la plataforma", example = "1")
            Long id,

            @Schema(description = "Codigo identificador unico en mayusculas", example = "LINKEDIN")
            String codigo,

            @Schema(description = "Nombre comercial de la plataforma", example = "LinkedIn")
            String nombre,

            @Schema(description = "ID de la plataforma origen equivalente si comparte bolsa", example = "3")
            Long plataformaOrigenId,

            @Schema(description = "Nombre de la plataforma origen equivalente", example = "Indeed")
            String plataformaOrigenNombre
    ) {
        public static PlataformaResponse fromEntity(Plataforma plataforma) {
            if (plataforma == null) {
                return null;
            }
            Long origenId = plataforma.getPlataformaOrigen() != null
                    ? plataforma.getPlataformaOrigen().getId()
                    : null;
            String origenNombre = plataforma.getPlataformaOrigen() != null
                    ? plataforma.getPlataformaOrigen().getNombre()
                    : null;

            return new PlataformaResponse(
                    plataforma.getId(),
                    plataforma.getCodigo(),
                    plataforma.getNombre(),
                    origenId,
                    origenNombre
            );
        }
    }

    /**
     * Peticion para dar de alta una nueva plataforma de empleo.
     */
    @Schema(description = "Datos para registrar una nueva plataforma")
    public record PlataformaCreateRequest(
            @Schema(description = "Codigo unico de la plataforma", example = "MANFRED")
            @NotBlank(message = "El codigo de la plataforma es obligatorio")
            @Size(max = 50, message = "El codigo no puede superar los 50 caracteres")
            @JsonAlias({"code", "codigo"})
            String codigo,

            @Schema(description = "Nombre comercial de la plataforma", example = "Manfred")
            @NotBlank(message = "El nombre de la plataforma es obligatorio")
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
            @JsonAlias({"name", "nombre"})
            String nombre,

            @Schema(description = "ID opcional de la plataforma origen si es subsidiaria o bolsa compartida", example = "2")
            @JsonAlias({"plataformaOrigenId", "parentPlatformId", "parentId", "origenId"})
            Long plataformaOrigenId
    ) {
    }

    /**
     * Peticion para actualizar una plataforma de empleo existente.
     */
    @Schema(description = "Datos para modificar una plataforma existente")
    public record PlataformaUpdateRequest(
            @Schema(description = "Codigo unico de la plataforma", example = "MANFRED")
            @Size(max = 50, message = "El codigo no puede superar los 50 caracteres")
            @JsonAlias({"code", "codigo"})
            String codigo,

            @Schema(description = "Nombre comercial de la plataforma", example = "Manfred Daily")
            @NotBlank(message = "El nombre de la plataforma es obligatorio")
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
            @JsonAlias({"name", "nombre"})
            String nombre,

            @Schema(description = "ID opcional de la plataforma origen si es subsidiaria o bolsa compartida", example = "2")
            @JsonAlias({"plataformaOrigenId", "parentPlatformId", "parentId", "origenId"})
            Long plataformaOrigenId
    ) {
    }
}

