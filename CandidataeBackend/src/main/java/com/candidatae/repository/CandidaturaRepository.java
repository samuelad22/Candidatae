package com.candidatae.repository;

import com.candidatae.domain.Candidatura;
import com.candidatae.domain.EstadoCandidatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * Comprueba si el usuario ya tiene registrada una candidatura con esa clave de deduplicación.
     */
    boolean existsByUsuarioIdAndClaveDeduplicacion(Long usuarioId, String claveDeduplicacion);

    /**
     * Busca una candidatura existente por su clave de deduplicación para sincronizaciones automáticas.
     */
    Optional<Candidatura> findByUsuarioIdAndClaveDeduplicacion(Long usuarioId, String claveDeduplicacion);

    /**
     * Busca candidaturas de forma paginada según los criterios de filtrado.
     */
    @Override
    @EntityGraph(attributePaths = {"empresa", "plataforma"})
    Page<Candidatura> findAll(Specification<Candidatura> spec, Pageable pageable);

    /**
     * Cuenta el total de candidaturas de un usuario.
     */
    long countByUsuarioId(Long usuarioId);

    /**
     * Cuenta las candidaturas de un usuario en un estado determinado.
     */
    long countByUsuarioIdAndEstadoActual(Long usuarioId, EstadoCandidatura estadoActual);

    /**
     * Comprueba si existen candidaturas asociadas a una empresa (protección de integridad referencial).
     */
    boolean existsByEmpresaId(Long empresaId);

    /**
     * Comprueba si existen candidaturas asociadas a una plataforma (proteccion de integridad referencial).
     */
    boolean existsByPlataformaId(Long plataformaId);
}
