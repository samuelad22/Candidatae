package com.candidatae.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltorio homogéneo para respuestas paginadas de la API REST.
 *
 * @param <T> Tipo de datos de los elementos de la lista.
 */
@Schema(description = "Respuesta paginada homogénea")
public record PageResponse<T>(
        @Schema(description = "Elementos contenidos en la página actual")
        List<T> content,

        @Schema(description = "Número de página actual (índice base 0)", example = "0")
        int page,

        @Schema(description = "Tamaño de página solicitado", example = "20")
        int size,

        @Schema(description = "Número total de elementos disponibles en la consulta", example = "42")
        long totalElements,

        @Schema(description = "Número total de páginas disponibles", example = "3")
        int totalPages,

        @Schema(description = "Indica si la página actual es la última", example = "false")
        boolean last
) {
    /**
     * Construye un PageResponse a partir de un Page de Spring Data.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
