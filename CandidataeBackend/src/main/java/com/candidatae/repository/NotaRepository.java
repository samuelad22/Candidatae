package com.candidatae.repository;

import com.candidatae.domain.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de acceso a datos para las notas de una candidatura.
 */
@Repository
public interface NotaRepository extends JpaRepository<Nota, Long> {

    /**
     * Lista las notas de una candidatura ordenadas por fecha descendente.
     */
    List<Nota> findByCandidaturaIdOrderByFechaDesc(Long candidaturaId);
}
