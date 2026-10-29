package com.candidatae.repository;

import com.candidatae.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Empresa.
 */
@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /**
     * Busca una empresa por su nombre normalizado.
     */
    Optional<Empresa> findByNombreNormalizado(String nombreNormalizado);

    /**
     * Busca una empresa por su nombre comercial.
     */
    Optional<Empresa> findByNombreIgnoreCase(String nombre);

    /**
     * Comprueba si ya existe una empresa con ese nombre normalizado.
     */
    boolean existsByNombreNormalizado(String nombreNormalizado);
}
