package com.candidatae.dto;

import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.HistorialEstado;
import com.candidatae.domain.OrigenEvento;

import java.time.Instant;

/**
 * Evento o transicion de estado en el historial de una candidatura.
 */
public record HistorialEstadoResponse(
        Long id,
        EstadoCandidatura estadoAnterior,
        EstadoCandidatura estadoNuevo,
        Instant fechaEvento,
        OrigenEvento origen,
        Long correoProcesadoId
) {
    /**
     * Mapea una entidad HistorialEstado al record de respuesta.
     */
    public static HistorialEstadoResponse fromEntity(HistorialEstado historial) {
        if (historial == null) {
            return null;
        }
        return new HistorialEstadoResponse(
                historial.getId(),
                historial.getEstadoAnterior(),
                historial.getEstadoNuevo(),
                historial.getFechaEvento(),
                historial.getOrigen(),
                historial.getCorreoProcesadoId()
        );
    }
}
