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
     * Busca una plataforma por su codigo unico.
     */
    Optional<Plataforma> findByCodigo(String codigo);

    /**
     * Busca una plataforma por su codigo ignorando mayusculas y minusculas.
     */
    Optional<Plataforma> findByCodigoIgnoreCase(String codigo);

    /**
     * Comprueba si ya existe una plataforma registrada con un codigo concreto.
     */
    boolean existsByCodigoIgnoreCase(String codigo);

    /**
     * Comprueba si ya existe otra plataforma con el mismo codigo pero distinto identificador.
     */
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    /**
     * Lista todas las plataformas ordenadas alfabeticamente por nombre.
     */
    List<Plataforma> findAllByOrderByNombreAsc();

    /**
     * Filtra plataformas por coincidencia parcial en el nombre.
     */
    List<Plataforma> findByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

    /**
     * Lista plataformas secundarias que tienen asignada una plataforma de origen concreta.
     */
    List<Plataforma> findByPlataformaOrigenId(Long plataformaOrigenId);
}
