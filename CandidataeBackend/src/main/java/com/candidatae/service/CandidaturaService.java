package com.candidatae.service;

import com.candidatae.domain.Candidatura;
import com.candidatae.domain.Empresa;
import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.HistorialEstado;
import com.candidatae.domain.OrigenEvento;
import com.candidatae.domain.Plataforma;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CandidaturaCreateRequest;
import com.candidatae.dto.CandidaturaDetailResponse;
import com.candidatae.dto.CandidaturaResponse;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CandidaturaRepository;
import com.candidatae.repository.EmpresaRepository;
import com.candidatae.repository.PlataformaRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.CurrentUserProvider;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Servicio de lógica de negocio para la gestión de candidaturas.
 * Aplica deduplicación, normalización de empresas y registro de transiciones de estado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidaturaService {

    private final CandidaturaRepository candidaturaRepository;
    private final EmpresaRepository empresaRepository;
    private final PlataformaRepository plataformaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Da de alta una nueva candidatura para el usuario actualmente autenticado.
     */
    @Transactional
    public CandidaturaResponse crear(CandidaturaCreateRequest request) {
        Long usuarioId = currentUserProvider.requireUserId();
        return crearParaUsuario(request, usuarioId);
    }

    /**
     * Lógica de creación de candidatura asegurando el aislamiento por usuarioId.
     */
    @Transactional
    public CandidaturaResponse crearParaUsuario(CandidaturaCreateRequest request, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        // 1. Buscar o crear la empresa por su nombre normalizado
        Empresa empresa = buscarOCrearEmpresa(request.empresa());

        // 2. Si viene informada la plataforma, buscarla por su código
        Plataforma plataforma = null;
        if (request.plataformaCodigo() != null && !request.plataformaCodigo().isBlank()) {
            plataforma = plataformaRepository.findByCodigoIgnoreCase(request.plataformaCodigo().trim())
                    .orElseThrow(() -> ApiException.badRequest("La plataforma '" + request.plataformaCodigo() + "' no es válida"));
        }

        // 3. Construir la clave de deduplicación
        String claveDeduplicacion = construirClaveDeduplicacion(
                empresa.getNombreNormalizado(),
                request.puesto(),
                request.ubicacion(),
                plataforma
        );

        // 4. Crear la entidad Candidatura con estado inicial ENVIADA
        Instant ahora = Instant.now();
        Candidatura candidatura = Candidatura.builder()
                .usuario(usuario)
                .empresa(empresa)
                .plataforma(plataforma)
                .puesto(request.puesto().trim())
                .ubicacion(request.ubicacion() != null && !request.ubicacion().isBlank() ? request.ubicacion().trim() : null)
                .modalidad(request.modalidad())
                .salarioTexto(request.salarioTexto() != null && !request.salarioTexto().isBlank() ? request.salarioTexto().trim() : null)
                .enlaceOferta(request.enlaceOferta() != null && !request.enlaceOferta().isBlank() ? request.enlaceOferta().trim() : null)
                .fechaSolicitud(request.fechaSolicitud())
                .estadoActual(EstadoCandidatura.ENVIADA)
                .fechaUltimoCambio(ahora)
                .claveDeduplicacion(claveDeduplicacion)
                .build();

        // 5. Registrar el primer evento en HistorialEstado
        HistorialEstado primerHistorial = HistorialEstado.builder()
                .candidatura(candidatura)
                .estadoAnterior(null)
                .estadoNuevo(EstadoCandidatura.ENVIADA)
                .fechaEvento(ahora)
                .origen(OrigenEvento.MANUAL)
                .build();

        candidatura.addHistorial(primerHistorial);

        Candidatura guardada = candidaturaRepository.save(candidatura);
        log.info("Candidatura ID {} creada exitosamente para usuario ID {}", guardada.getId(), usuarioId);

        return CandidaturaResponse.fromEntity(guardada);
    }

    /**
     * Lista y filtra las candidaturas del usuario actualmente autenticado.
     */
    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listar(
            String platform,
            EstadoCandidatura status,
            String company,
            LocalDate from,
            LocalDate to,
            String search
    ) {
        Long usuarioId = currentUserProvider.requireUserId();
        return listarParaUsuario(usuarioId, platform, status, company, from, to, search);
    }

    /**
     * Obtiene el listado de candidaturas filtrado y ordenado para un usuario específico.
     */
    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listarParaUsuario(
            Long usuarioId,
            String platform,
            EstadoCandidatura status,
            String company,
            LocalDate from,
            LocalDate to,
            String search
    ) {
        Specification<Candidatura> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro obligatorio por usuario autenticado (aislamiento multiusuario)
            predicates.add(cb.equal(root.get("usuario").get("id"), usuarioId));

            // 2. Filtro opcional por código de plataforma (insensible a mayúsculas)
            if (platform != null && !platform.isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(root.get("plataforma").get("codigo")),
                        platform.trim().toUpperCase(Locale.ROOT)
                ));
            }

            // 3. Filtro opcional por estado actual de la candidatura
            if (status != null) {
                predicates.add(cb.equal(root.get("estadoActual"), status));
            }

            // 4. Filtro opcional por nombre de empresa (búsqueda parcial insensible a mayúsculas)
            if (company != null && !company.isBlank()) {
                String companyPattern = "%" + company.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("empresa").get("nombre")), companyPattern));
            }

            // 5. Filtro opcional por rango de fechas de solicitud (from y to)
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaSolicitud"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaSolicitud"), to));
            }

            // 6. Filtro opcional de texto libre en puesto, nombre de empresa o ubicación
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate matchPuesto = cb.like(cb.lower(root.get("puesto")), searchPattern);
                Predicate matchEmpresa = cb.like(cb.lower(root.get("empresa").get("nombre")), searchPattern);
                Predicate matchUbicacion = cb.like(cb.lower(root.get("ubicacion")), searchPattern);

                predicates.add(cb.or(matchPuesto, matchEmpresa, matchUbicacion));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Orden descendente por fecha de último cambio
        Sort sort = Sort.by(Sort.Direction.DESC, "fechaUltimoCambio");
        List<Candidatura> candidaturas = candidaturaRepository.findAll(spec, sort);

        return candidaturas.stream()
                .map(CandidaturaResponse::fromEntity)
                .toList();
    }

    /**
     * Obtiene el detalle de una candidatura por su ID asegurando que pertenece al usuario autenticado.
     */
    @Transactional(readOnly = true)
    public CandidaturaDetailResponse obtenerDetalle(Long id) {
        Long usuarioId = currentUserProvider.requireUserId();
        return obtenerDetalleParaUsuario(id, usuarioId);
    }

    /**
     * Busca y devuelve el detalle de una candidatura para un usuario especifico.
     */
    @Transactional(readOnly = true)
    public CandidaturaDetailResponse obtenerDetalleParaUsuario(Long id, Long usuarioId) {
        Candidatura candidatura = candidaturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> ApiException.notFound("Candidatura no encontrada"));

        return CandidaturaDetailResponse.fromEntity(candidatura);
    }

    /**
     * Busca una empresa existente por nombre normalizado o crea una nueva si no existe.
     */
    private Empresa buscarOCrearEmpresa(String nombreOriginal) {
        String normalizado = normalizarNombreEmpresa(nombreOriginal);

        return empresaRepository.findByNombreNormalizado(normalizado)
                .orElseGet(() -> empresaRepository.save(
                        Empresa.builder()
                                .nombre(nombreOriginal.trim())
                                .nombreNormalizado(normalizado)
                                .build()
                ));
    }

    /**
     * Normaliza el nombre de la empresa: minúsculas, espacios colapsados y sin sufijos legales (S.L., S.A., etc.).
     */
    public String normalizarNombreEmpresa(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }

        String limpio = nombre.toLowerCase(Locale.ROOT).trim();
        limpio = limpio.replaceAll("\\s+", " ");

        // Eliminar sufijos societarios comunes al final del nombre
        limpio = limpio.replaceAll("(?i)[,\\s\\-]+(?:s\\.?l\\.?u\\.?|s\\.?l\\.?|sl|s\\.?a\\.?u\\.?|s\\.?a\\.?|sa|s\\.?c\\.?|ltd\\.?|inc\\.?|llc\\.?|corp\\.?|gmbh)$", "");
        limpio = limpio.replaceAll("[,\\.\\-\\s]+$", "").trim();

        return limpio.isBlank() ? nombre.toLowerCase(Locale.ROOT).trim() : limpio;
    }

    /**
     * Construye la clave de deduplicación: empresa + puesto + ubicación (+ plataforma de origen).
     * Si la plataforma tiene plataformaOrigen (ej. Glassdoor -> Indeed), comparten origen.
     */
    public String construirClaveDeduplicacion(String empresaNormalizada, String puesto, String ubicacion, Plataforma plataforma) {
        String emp = normalizarSlug(empresaNormalizada);
        String pue = normalizarSlug(puesto);
        String ubi = normalizarSlug(ubicacion);
        if (ubi.isBlank()) {
            ubi = "sin-ubicacion";
        }

        String origenPlataforma = "";
        if (plataforma != null) {
            if (plataforma.getPlataformaOrigen() != null) {
                origenPlataforma = normalizarSlug(plataforma.getPlataformaOrigen().getCodigo());
            } else {
                origenPlataforma = normalizarSlug(plataforma.getCodigo());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(emp).append(":").append(pue).append(":").append(ubi);
        if (!origenPlataforma.isBlank()) {
            sb.append(":").append(origenPlataforma);
        }

        String clave = sb.toString();
        if (clave.length() > 255) {
            clave = clave.substring(0, 255);
        }
        return clave;
    }

    /**
     * Convierte un texto a slug limpio en minúsculas y sin acentos.
     */
    public String normalizarSlug(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
