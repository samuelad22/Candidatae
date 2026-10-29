package com.candidatae.service;

import com.candidatae.domain.Plataforma;
import com.candidatae.dto.PlataformaDto.PlataformaCreateRequest;
import com.candidatae.dto.PlataformaDto.PlataformaResponse;
import com.candidatae.dto.PlataformaDto.PlataformaUpdateRequest;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CandidaturaRepository;
import com.candidatae.repository.PlataformaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Servicio de negocio para la gestion y consulta de plataformas de empleo (catalogo, CRUD y selectores).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlataformaService {

    private final PlataformaRepository plataformaRepository;
    private final CandidaturaRepository candidaturaRepository;

    /**
     * Lista todas las plataformas ordenadas alfabeticamente. Si se introduce un termino
     * de busqueda, filtra por coincidencia parcial en nombre o codigo.
     */
    @Transactional(readOnly = true)
    public List<PlataformaResponse> listar(String search) {
        List<Plataforma> plataformas;
        if (search != null && !search.trim().isBlank()) {
            String sanitized = search.trim();
            if (sanitized.length() > 100) {
                sanitized = sanitized.substring(0, 100);
            }
            plataformas = plataformaRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc(sanitized);
        } else {
            plataformas = plataformaRepository.findAllByOrderByNombreAsc();
        }
        return plataformas.stream()
                .map(PlataformaResponse::fromEntity)
                .toList();
    }

    /**
     * Obtiene el detalle de una plataforma por su ID.
     */
    @Transactional(readOnly = true)
    public PlataformaResponse obtenerPorId(Long id) {
        Plataforma plataforma = plataformaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Plataforma no encontrada"));
        return PlataformaResponse.fromEntity(plataforma);
    }

    /**
     * Obtiene el detalle de una plataforma por su codigo identificador.
     */
    @Transactional(readOnly = true)
    public PlataformaResponse obtenerPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw ApiException.badRequest("El codigo de plataforma es obligatorio");
        }
        Plataforma plataforma = plataformaRepository.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> ApiException.notFound("Plataforma no encontrada con codigo '" + codigo + "'"));
        return PlataformaResponse.fromEntity(plataforma);
    }

    /**
     * Registra una nueva plataforma en el catalogo.
     *
     * @throws ApiException 409 si ya existe una plataforma con ese codigo.
     * @throws ApiException 400 si la plataforma origen especificada no existe.
     */
    @Transactional
    public PlataformaResponse crear(PlataformaCreateRequest request) {
        String codigoLimpio = request.codigo().trim().toUpperCase(Locale.ROOT);
        String nombreLimpio = request.nombre().trim();

        if (plataformaRepository.existsByCodigoIgnoreCase(codigoLimpio)) {
            throw ApiException.conflict("Ya existe una plataforma registrada con el codigo '" + codigoLimpio + "'");
        }

        Plataforma origen = null;
        if (request.plataformaOrigenId() != null) {
            origen = plataformaRepository.findById(request.plataformaOrigenId())
                    .orElseThrow(() -> ApiException.badRequest("La plataforma de origen con ID " + request.plataformaOrigenId() + " no existe"));
        }

        Plataforma plataforma = Plataforma.builder()
                .codigo(codigoLimpio)
                .nombre(nombreLimpio)
                .plataformaOrigen(origen)
                .build();

        Plataforma guardada = plataformaRepository.save(plataforma);
        log.info("Plataforma creada con id {}: {} ({})", guardada.getId(), guardada.getNombre(), guardada.getCodigo());
        return PlataformaResponse.fromEntity(guardada);
    }

    /**
     * Actualiza los datos de una plataforma existente.
     *
     * @throws ApiException 404 si la plataforma no existe.
     * @throws ApiException 409 si el nuevo codigo ya lo utiliza otra plataforma.
     * @throws ApiException 400 si se intenta asignar a si misma como origen o la plataforma origen no existe.
     */
    @Transactional
    public PlataformaResponse actualizar(Long id, PlataformaUpdateRequest request) {
        Plataforma plataforma = plataformaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Plataforma no encontrada"));

        if (request.codigo() != null && !request.codigo().isBlank()) {
            String nuevoCodigo = request.codigo().trim().toUpperCase(Locale.ROOT);
            if (plataformaRepository.existsByCodigoIgnoreCaseAndIdNot(nuevoCodigo, id)) {
                throw ApiException.conflict("Ya existe otra plataforma con el codigo '" + nuevoCodigo + "'");
            }
            plataforma.setCodigo(nuevoCodigo);
        }

        plataforma.setNombre(request.nombre().trim());

        if (request.plataformaOrigenId() != null) {
            if (request.plataformaOrigenId().equals(id)) {
                throw ApiException.badRequest("Una plataforma no puede asignarse a si misma como plataforma de origen");
            }
            Plataforma origen = plataformaRepository.findById(request.plataformaOrigenId())
                    .orElseThrow(() -> ApiException.badRequest("La plataforma de origen con ID " + request.plataformaOrigenId() + " no existe"));
            plataforma.setPlataformaOrigen(origen);
        } else {
            plataforma.setPlataformaOrigen(null);
        }

        Plataforma guardada = plataformaRepository.save(plataforma);
        log.info("Plataforma {} actualizada: {} ({})", id, guardada.getNombre(), guardada.getCodigo());
        return PlataformaResponse.fromEntity(guardada);
    }

    /**
     * Elimina una plataforma del catalogo si no tiene candidaturas vinculadas.
     *
     * @throws ApiException 404 si la plataforma no existe.
     * @throws ApiException 409 si existen candidaturas vinculadas a esta plataforma.
     */
    @Transactional
    public void eliminar(Long id) {
        Plataforma plataforma = plataformaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Plataforma no encontrada"));

        if (candidaturaRepository.existsByPlataformaId(id)) {
            throw ApiException.conflict(
                    "No se puede eliminar la plataforma '" + plataforma.getNombre() + "' porque tiene candidaturas asociadas"
            );
        }

        List<Plataforma> secundarias = plataformaRepository.findByPlataformaOrigenId(id);
        for (Plataforma sec : secundarias) {
            sec.setPlataformaOrigen(null);
            plataformaRepository.save(sec);
        }

        plataformaRepository.delete(plataforma);
        log.info("Plataforma {} ({}) eliminada", id, plataforma.getCodigo());
    }
}

