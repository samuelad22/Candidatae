package com.candidatae.dto;

import com.candidatae.domain.Nota;

import java.time.Instant;

/**
 * Nota personal asociada a una candidatura.
 */
public record NotaResponse(
        Long id,
        String texto,
        Instant fecha
) {
    /**
     * Mapea una entidad Nota al record de respuesta.
     */
    public static NotaResponse fromEntity(Nota nota) {
        if (nota == null) {
            return null;
        }
        return new NotaResponse(
                nota.getId(),
                nota.getTexto(),
                nota.getFecha()
        );
    }
}
