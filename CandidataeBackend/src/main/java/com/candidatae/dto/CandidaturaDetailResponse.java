package com.candidatae.dto;

import com.candidatae.domain.Candidatura;
import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.HistorialEstado;
import com.candidatae.domain.Modalidad;
import com.candidatae.domain.Nota;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Detalle completo de una candidatura incluyendo su historial, notas y contactos.
 */
public record CandidaturaDetailResponse(
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
        String claveDeduplicacion,
        List<HistorialEstadoResponse> historial,
        List<NotaResponse> notas,
        List<ContactoResponse> contactos
) {
    /**
     * Mapea una entidad Candidatura y sus relaciones al record de detalle.
     */
    public static CandidaturaDetailResponse fromEntity(Candidatura candidatura) {
        if (candidatura == null) {
            return null;
        }

        List<HistorialEstadoResponse> historial = candidatura.getHistoriales() == null ? List.of() :
                candidatura.getHistoriales().stream()
                        .sorted(Comparator.comparing(HistorialEstado::getFechaEvento, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(HistorialEstadoResponse::fromEntity)
                        .toList();

        List<NotaResponse> notas = candidatura.getNotas() == null ? List.of() :
                candidatura.getNotas().stream()
                        .sorted(Comparator.comparing(Nota::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                        .map(NotaResponse::fromEntity)
                        .toList();

        List<ContactoResponse> contactos = candidatura.getContactos() == null ? List.of() :
                candidatura.getContactos().stream()
                        .map(ContactoResponse::fromEntity)
                        .toList();

        return new CandidaturaDetailResponse(
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
                candidatura.getClaveDeduplicacion(),
                historial,
                notas,
                contactos
        );
    }
}
