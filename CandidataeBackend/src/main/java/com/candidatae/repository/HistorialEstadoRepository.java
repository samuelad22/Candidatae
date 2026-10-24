package com.candidatae.repository;

import com.candidatae.domain.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para el historial de estados de una
 * candidatura.
 */
@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    /**
     * Obtiene el último estado registrado de una candidatura.
     */
    Optional<HistorialEstado> findFirstByCandidaturaIdOrderByFechaEventoDesc(Long candidaturaId);
}
