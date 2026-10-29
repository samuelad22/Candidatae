package com.candidatae.dto;

import com.candidatae.domain.Candidatura;
import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.Modalidad;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Respuesta con la información completa de una candidatura.
 * Se devuelve al cliente tras crear, actualizar o consultar una candidatura.
 */
public record CandidaturaResponse(
        Long id,
        Long empresaId,
        String empresaNombre,
        Long plataformaId,
        String plataformaCodigo,
        String plataformaNombre,
        String puesto,
        String ubicacion,
        Modalidad modalidad,
        String salarioTexto,
        String enlaceOferta,
        LocalDate fechaSolicitud,
        EstadoCandidatura estadoActual,
        Instant fechaUltimoCambio,
        String claveDeduplicacion
) {
    /**
     * Convierte una entidad JPA Candidatura al record DTO de salida.
     */
    public static CandidaturaResponse fromEntity(Candidatura candidatura) {
        if (candidatura == null) {
            return null;
        }

        return new CandidaturaResponse(
                candidatura.getId(),
                candidatura.getEmpresa() != null ? candidatura.getEmpresa().getId() : null,
                candidatura.getEmpresa() != null ? candidatura.getEmpresa().getNombre() : null,
                candidatura.getPlataforma() != null ? candidatura.getPlataforma().getId() : null,
                candidatura.getPlataforma() != null ? candidatura.getPlataforma().getCodigo() : null,
                candidatura.getPlataforma() != null ? candidatura.getPlataforma().getNombre() : null,
                candidatura.getPuesto(),
                candidatura.getUbicacion(),
                candidatura.getModalidad(),
                candidatura.getSalarioTexto(),
                candidatura.getEnlaceOferta(),
                candidatura.getFechaSolicitud(),
                candidatura.getEstadoActual(),
                candidatura.getFechaUltimoCambio(),
                candidatura.getClaveDeduplicacion()
        );
    }
}
