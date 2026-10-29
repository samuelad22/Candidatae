package com.candidatae.repository;

import com.candidatae.domain.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de acceso a datos para los contactos de una candidatura.
 */
@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {

    /**
     * Lista los contactos asociados a una candidatura.
     */
    List<Contacto> findByCandidaturaId(Long candidaturaId);
}
