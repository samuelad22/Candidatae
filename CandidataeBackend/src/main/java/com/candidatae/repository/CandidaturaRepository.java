package com.candidatae.repository;

import com.candidatae.domain.Candidatura;
import com.candidatae.domain.EstadoCandidatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Candidatura.
 */
@Repository
public interface CandidaturaRepository extends JpaRepository<Candidatura, Long>, JpaSpecificationExecutor<Candidatura> {

    /**
     * Busca una candidatura por su ID y el ID del usuario propietario.
     */
    Optional<Candidatura> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Comprueba si existe una candidatura por su ID y el usuario propietario.
     */
    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Busca una candidatura del usuario por su clave de deduplicación.
     */
    Optional<Candidatura> findByUsuarioIdAndClaveDeduplicacion(Long usuarioId, String claveDeduplicacion);

    /**
     * Cuenta el total de candidaturas de un usuario.
     */
    long countByUsuarioId(Long usuarioId);

    /**
     * Cuenta las candidaturas de un usuario en un estado determinado.
     */
    long countByUsuarioIdAndEstadoActual(Long usuarioId, EstadoCandidatura estadoActual);
}
