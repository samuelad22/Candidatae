package com.candidatae.service;

import com.candidatae.domain.Empresa;
import com.candidatae.dto.EmpresaDto.EmpresaCreateRequest;
import com.candidatae.dto.EmpresaDto.EmpresaResponse;
import com.candidatae.dto.EmpresaDto.EmpresaUpdateRequest;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CandidaturaRepository;
import com.candidatae.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Servicio de negocio para la gestión y consulta de empresas contratantes (CRUD y filtros).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final CandidaturaRepository candidaturaRepository;

    /**
     * Devuelve el listado alfabético de empresas. Si se especifica un término de búsqueda,
     * filtra por coincidencia parcial insensible a mayúsculas/minúsculas.
     */
    @Transactional(readOnly = true)
    public List<EmpresaResponse> listar(String search) {
        List<Empresa> empresas;
        if (search != null && !search.trim().isBlank()) {
            String sanitized = search.trim();
            if (sanitized.length() > 100) {
                sanitized = sanitized.substring(0, 100);
            }
            empresas = empresaRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc(sanitized);
        } else {
            empresas = empresaRepository.findAllByOrderByNombreAsc();
        }
        return empresas.stream()
                .map(EmpresaResponse::fromEntity)
                .toList();
    }

    /**
     * Obtiene el detalle de una empresa por su ID.
     */
    @Transactional(readOnly = true)
    public EmpresaResponse obtenerPorId(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Empresa no encontrada"));
        return EmpresaResponse.fromEntity(empresa);
    }

    /**
     * Da de alta una nueva empresa con normalización automática de nombre.
     *
     * @throws ApiException 409 si ya existe una empresa con ese nombre normalizado.
     */
    @Transactional
    public EmpresaResponse crear(EmpresaCreateRequest request) {
        String nombreLimpio = request.nombre().trim();
        String normalizado = normalizarNombre(nombreLimpio);

        if (empresaRepository.existsByNombreNormalizado(normalizado)) {
            throw ApiException.conflict("Ya existe una empresa registrada con el nombre '" + nombreLimpio + "'");
        }

        Empresa empresa = Empresa.builder()
                .nombre(nombreLimpio)
                .nombreNormalizado(normalizado)
                .build();

        Empresa guardada = empresaRepository.save(empresa);
        log.info("Empresa creada con id {}: {}", guardada.getId(), guardada.getNombre());
        return EmpresaResponse.fromEntity(guardada);
    }

    /**
     * Actualiza el nombre de una empresa existente y recalcula su identificador normalizado.
     *
     * @throws ApiException 404 si la empresa no existe.
     * @throws ApiException 409 si el nuevo nombre colisiona con otra empresa ya registrada.
     */
    @Transactional
    public EmpresaResponse actualizar(Long id, EmpresaUpdateRequest request) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Empresa no encontrada"));

        String nuevoNombre = request.nombre().trim();
        String nuevoNormalizado = normalizarNombre(nuevoNombre);

        if (empresaRepository.existsByNombreNormalizadoAndIdNot(nuevoNormalizado, id)) {
            throw ApiException.conflict("Ya existe otra empresa registrada con el nombre '" + nuevoNombre + "'");
        }

        empresa.setNombre(nuevoNombre);
        empresa.setNombreNormalizado(nuevoNormalizado);
        Empresa guardada = empresaRepository.save(empresa);
        log.info("Empresa {} actualizada a '{}'", id, nuevoNombre);
        return EmpresaResponse.fromEntity(guardada);
    }

    /**
     * Elimina una empresa del catálogo si no tiene candidaturas asociadas.
     *
     * @throws ApiException 404 si la empresa no existe.
     * @throws ApiException 409 si existen candidaturas vinculadas a esta empresa.
     */
    @Transactional
    public void eliminar(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Empresa no encontrada"));

        if (candidaturaRepository.existsByEmpresaId(id)) {
            throw ApiException.conflict(
                    "No se puede eliminar la empresa '" + empresa.getNombre() + "' porque tiene candidaturas asociadas"
            );
        }

        empresaRepository.delete(empresa);
        log.info("Empresa {} eliminada", id);
    }

    /**
     * Normaliza el nombre comercial de una empresa: convierte a minúsculas,
     * colapsa espacios múltiples y elimina sufijos societarios comunes (S.L., S.A., LLC, Inc., etc.).
     */
    public String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }

        String limpio = nombre.toLowerCase(Locale.ROOT).trim();
        limpio = limpio.replaceAll("\\s+", " ");

        // Eliminar sufijos societarios comunes
        limpio = limpio.replaceAll("(?i)[,\\s\\-]+(?:s\\.?l\\.?u\\.?|s\\.?l\\.?|sl|s\\.?a\\.?u\\.?|s\\.?a\\.?|sa|s\\.?c\\.?|ltd\\.?|inc\\.?|llc\\.?|corp\\.?|gmbh)$", "");
        limpio = limpio.replaceAll("[,\\.\\-\\s]+$", "").trim();

        return limpio.isBlank() ? nombre.toLowerCase(Locale.ROOT).trim() : limpio;
    }
}

