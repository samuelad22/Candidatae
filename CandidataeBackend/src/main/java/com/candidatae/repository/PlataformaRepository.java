package com.candidatae.repository;

import com.candidatae.domain.Plataforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Plataforma.
 */
@Repository
public interface PlataformaRepository extends JpaRepository<Plataforma, Long> {

    /**
     * Busca una plataforma por su código único.
     */
    Optional<Plataforma> findByCodigo(String codigo);

    /**
     * Busca una plataforma por su código ignorando mayúsculas y minúsculas.
     */
    Optional<Plataforma> findByCodigoIgnoreCase(String codigo);

    /**
     * Lista todas las plataformas ordenadas alfabéticamente por nombre.
     */
    List<Plataforma> findAllByOrderByNombreAsc();
}
