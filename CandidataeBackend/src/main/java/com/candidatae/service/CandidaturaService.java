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
import com.candidatae.dto.CandidaturaUpdateRequest;
import com.candidatae.dto.PageResponse;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CandidaturaRepository;
import com.candidatae.repository.EmpresaRepository;
import com.candidatae.repository.PlataformaRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.CurrentUserProvider;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        // Comprobar si el usuario ya registró esa misma oferta
        if (candidaturaRepository.existsByUsuarioIdAndClaveDeduplicacion(usuarioId, claveDeduplicacion)) {
            throw ApiException.conflict("Ya tienes registrada una candidatura para este puesto, empresa y plataforma");
        }

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
     * Lista y filtra las candidaturas del usuario actualmente autenticado con paginación.
     */
    @Transactional(readOnly = true)
    public PageResponse<CandidaturaResponse> listar(
            String platform,
            String status,
            String company,
            LocalDate from,
            LocalDate to,
            String search,
            int page,
            int size
    ) {
        Long usuarioId = currentUserProvider.requireUserId();
        return listarParaUsuario(usuarioId, platform, status, company, from, to, search, page, size);
    }

    /**
     * Obtiene el listado paginado de candidaturas filtrado y ordenado para un usuario específico.
     */
    @Transactional(readOnly = true)
    public PageResponse<CandidaturaResponse> listarParaUsuario(
            Long usuarioId,
            String platform,
            String status,
            String company,
            LocalDate from,
            LocalDate to,
            String search,
            int page,
            int size
    ) {
        // Validación de paginación
        if (page < 0) {
            throw ApiException.badRequest("El número de página no puede ser negativo");
        }
        if (size < 1 || size > 100) {
            throw ApiException.badRequest("El tamaño de página debe estar entre 1 y 100");
        }

        // 1. Validación de plataforma si viene informada
        if (platform != null && !platform.isBlank()) {
            plataformaRepository.findByCodigoIgnoreCase(platform.trim())
                    .orElseThrow(() -> ApiException.badRequest("La plataforma '" + platform + "' no es válida"));
        }

        // 2. Validación y conversión de estado si viene informado
        EstadoCandidatura estadoEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                estadoEnum = EstadoCandidatura.fromString(status);
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("Valor '" + status + "' no válido para 'status'. Valores permitidos: [ENVIADA, EN_PROCESO, ENTREVISTA, OFERTA, RECHAZADA, DESCARTADA]");
            }
        }
        final EstadoCandidatura estadoFiltro = estadoEnum;

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
            if (estadoFiltro != null) {
                predicates.add(cb.equal(root.get("estadoActual"), estadoFiltro));
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
                String sanitizedSearch = search.trim();
                if (sanitizedSearch.length() > 100) {
                    sanitizedSearch = sanitizedSearch.substring(0, 100);
                }
                String searchPattern = "%" + escapeLikeWildcards(sanitizedSearch.toLowerCase(Locale.ROOT)) + "%";
                Predicate matchPuesto = cb.like(cb.lower(root.get("puesto")), searchPattern, '\\');
                Predicate matchEmpresa = cb.like(cb.lower(root.get("empresa").get("nombre")), searchPattern, '\\');
                Predicate matchUbicacion = cb.like(cb.lower(root.get("ubicacion")), searchPattern, '\\');

                predicates.add(cb.or(matchPuesto, matchEmpresa, matchUbicacion));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Orden descendente por fecha de último cambio y paginación
        Sort sort = Sort.by(
                Sort.Order.desc("fechaUltimoCambio"),
                Sort.Order.desc("fechaSolicitud"),
                Sort.Order.desc("id")
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Candidatura> candidaturasPage = candidaturaRepository.findAll(spec, pageable);

        return PageResponse.from(candidaturasPage.map(CandidaturaResponse::fromEntity));
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
     * Actualiza parcialmente los datos o el estado de una candidatura del usuario autenticado.
     */
    @Transactional
    public CandidaturaResponse actualizar(Long id, CandidaturaUpdateRequest request) {
        Long usuarioId = currentUserProvider.requireUserId();
        return actualizarParaUsuario(id, request, usuarioId);
    }

    /**
     * Lógica de actualización de candidatura asegurando aislamiento multiusuario,
     * recálculo de deduplicación y registro automático de transiciones de estado en el historial.
     */
    @Transactional
    public CandidaturaResponse actualizarParaUsuario(Long id, CandidaturaUpdateRequest request, Long usuarioId) {
        Candidatura candidatura = candidaturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> ApiException.notFound("Candidatura no encontrada"));

        boolean huboCambios = false;
        boolean recalcularClave = false;

        // 1. Modificación de empresa
        if (request.empresa() != null && !request.empresa().isBlank()) {
            Empresa nuevaEmpresa = buscarOCrearEmpresa(request.empresa());
            if (!nuevaEmpresa.equals(candidatura.getEmpresa())) {
                candidatura.setEmpresa(nuevaEmpresa);
                huboCambios = true;
                recalcularClave = true;
            }
        }

        // 2. Modificación de plataforma
        if (request.plataformaCodigo() != null) {
            if (request.plataformaCodigo().isBlank()) {
                if (candidatura.getPlataforma() != null) {
                    candidatura.setPlataforma(null);
                    huboCambios = true;
                    recalcularClave = true;
                }
            } else {
                Plataforma nuevaPlataforma = plataformaRepository.findByCodigoIgnoreCase(request.plataformaCodigo().trim())
                        .orElseThrow(() -> ApiException.badRequest("La plataforma '" + request.plataformaCodigo() + "' no es válida"));
                if (!nuevaPlataforma.equals(candidatura.getPlataforma())) {
                    candidatura.setPlataforma(nuevaPlataforma);
                    huboCambios = true;
                    recalcularClave = true;
                }
            }
        }

        // 3. Modificación de puesto
        if (request.puesto() != null && !request.puesto().isBlank()) {
            String nuevoPuesto = request.puesto().trim();
            if (!nuevoPuesto.equals(candidatura.getPuesto())) {
                candidatura.setPuesto(nuevoPuesto);
                huboCambios = true;
                recalcularClave = true;
            }
        }

        // 4. Modificación de ubicación
        if (request.ubicacion() != null) {
            String nuevaUbicacion = request.ubicacion().isBlank() ? null : request.ubicacion().trim();
            if (nuevaUbicacion != null ? !nuevaUbicacion.equals(candidatura.getUbicacion()) : candidatura.getUbicacion() != null) {
                candidatura.setUbicacion(nuevaUbicacion);
                huboCambios = true;
                recalcularClave = true;
            }
        }

        // 5. Modificación de modalidad
        if (request.modalidad() != null && request.modalidad() != candidatura.getModalidad()) {
            candidatura.setModalidad(request.modalidad());
            huboCambios = true;
        }

        // 6. Modificación de salarioTexto
        if (request.salarioTexto() != null) {
            String nuevoSalario = request.salarioTexto().isBlank() ? null : request.salarioTexto().trim();
            if (nuevoSalario != null ? !nuevoSalario.equals(candidatura.getSalarioTexto()) : candidatura.getSalarioTexto() != null) {
                candidatura.setSalarioTexto(nuevoSalario);
                huboCambios = true;
            }
        }

        // 7. Modificación de enlaceOferta
        if (request.enlaceOferta() != null) {
            String nuevoEnlace = request.enlaceOferta().isBlank() ? null : request.enlaceOferta().trim();
            if (nuevoEnlace != null ? !nuevoEnlace.equals(candidatura.getEnlaceOferta()) : candidatura.getEnlaceOferta() != null) {
                candidatura.setEnlaceOferta(nuevoEnlace);
                huboCambios = true;
            }
        }

        // 8. Modificación de fechaSolicitud
        if (request.fechaSolicitud() != null && !request.fechaSolicitud().equals(candidatura.getFechaSolicitud())) {
            candidatura.setFechaSolicitud(request.fechaSolicitud());
            huboCambios = true;
        }

        // 9. Recalcular clave de deduplicación si cambiaron empresa, puesto, ubicación o plataforma
        if (recalcularClave) {
            String claveDeduplicacion = construirClaveDeduplicacion(
                    candidatura.getEmpresa().getNombreNormalizado(),
                    candidatura.getPuesto(),
                    candidatura.getUbicacion(),
                    candidatura.getPlataforma()
            );
            if (!claveDeduplicacion.equals(candidatura.getClaveDeduplicacion())
                    && candidaturaRepository.existsByUsuarioIdAndClaveDeduplicacion(usuarioId, claveDeduplicacion)) {
                throw ApiException.conflict("Ya tienes registrada otra candidatura para este puesto, empresa y plataforma");
            }
            candidatura.setClaveDeduplicacion(claveDeduplicacion);
        }

        // 10. Regla crítica: Si estadoActual cambia, registrar nuevo HistorialEstado
        if (request.estadoActual() != null && request.estadoActual() != candidatura.getEstadoActual()) {
            EstadoCandidatura estadoViejo = candidatura.getEstadoActual();
            Instant ahora = Instant.now();

            HistorialEstado nuevoHistorial = HistorialEstado.builder()
                    .candidatura(candidatura)
                    .estadoAnterior(estadoViejo)
                    .estadoNuevo(request.estadoActual())
                    .fechaEvento(ahora)
                    .origen(OrigenEvento.MANUAL)
                    .build();

            candidatura.addHistorial(nuevoHistorial);
            candidatura.setEstadoActual(request.estadoActual());
            huboCambios = true;
        }

        // 11. Actualizar fechaUltimoCambio si hubo algún cambio
        if (huboCambios) {
            candidatura.setFechaUltimoCambio(Instant.now());
        }

        Candidatura guardada = candidaturaRepository.save(candidatura);
        log.info("Candidatura ID {} actualizada exitosamente para usuario ID {}", guardada.getId(), usuarioId);

        return CandidaturaResponse.fromEntity(guardada);
    }

    /**
     * Elimina una candidatura del usuario actualmente autenticado.
     */
    @Transactional
    public void eliminar(Long id) {
        Long usuarioId = currentUserProvider.requireUserId();
        eliminarParaUsuario(id, usuarioId);
    }

    /**
     * Elimina una candidatura asegurando el aislamiento multiusuario.
     */
    @Transactional
    public void eliminarParaUsuario(Long id, Long usuarioId) {
        Candidatura candidatura = candidaturaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> ApiException.notFound("Candidatura no encontrada"));

        candidaturaRepository.delete(candidatura);
        log.info("Candidatura ID {} eliminada exitosamente para usuario ID {}", id, usuarioId);
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

    /**
     * Escapa caracteres comodín de SQL LIKE (%, _, \\) para evitar búsquedas expansivas involuntarias.
     */
    public String escapeLikeWildcards(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Registra o actualiza en base de datos una candidatura detectada automáticamente desde un correo electrónico.
     */
    @Transactional
    public Candidatura registrarOActualizarDesdeCorreo(
            Long usuarioId,
            String plataformaCodigo,
            String estadoDetectado,
            String puesto,
            String empresaNombre,
            LocalDate fechaSolicitud
    ) {
        return registrarOActualizarDesdeCorreo(
                usuarioId, plataformaCodigo, estadoDetectado, puesto, empresaNombre, fechaSolicitud, Instant.now()
        );
    }

    /**
     * Registra o actualiza en base de datos una candidatura detectada desde un correo electrónico con su fecha/hora precisa.
     */
    @Transactional
    public Candidatura registrarOActualizarDesdeCorreo(
            Long usuarioId,
            String plataformaCodigo,
            String estadoDetectado,
            String puesto,
            String empresaNombre,
            LocalDate fechaSolicitud,
            Instant fechaEvento
    ) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        EstadoCandidatura nuevoEstado = mapearEstadoDetectado(estadoDetectado);
        String nombreEmpresaLimpio = empresaNombre != null && !empresaNombre.isBlank()
                ? empresaNombre.trim()
                : "Empresa Confidencial";
        if (nombreEmpresaLimpio.length() > 255) {
            nombreEmpresaLimpio = nombreEmpresaLimpio.substring(0, 255).trim();
        }
        Empresa empresa = buscarOCrearEmpresa(nombreEmpresaLimpio);

        Plataforma plataforma = null;
        if (plataformaCodigo != null && !plataformaCodigo.isBlank()) {
            String codigoPlataforma = plataformaCodigo.trim();
            if (codigoPlataforma.length() > 50) {
                codigoPlataforma = codigoPlataforma.substring(0, 50).trim();
            }
            plataforma = buscarOCrearPlataforma(codigoPlataforma);
        }

        String puestoLimpio = puesto != null && !puesto.isBlank() ? puesto.trim() : "Puesto de empleo";
        if (puestoLimpio.length() > 255) {
            puestoLimpio = puestoLimpio.substring(0, 255).trim();
        }
        String claveDeduplicacion = construirClaveDeduplicacion(
                empresa.getNombreNormalizado(),
                puestoLimpio,
                "sin-ubicacion",
                plataforma
        );

        Instant ahora = fechaEvento != null ? fechaEvento : Instant.now();
        Candidatura candidatura = candidaturaRepository
                .findByUsuarioIdAndClaveDeduplicacion(usuarioId, claveDeduplicacion)
                .orElse(null);

        if (candidatura == null) {
            candidatura = Candidatura.builder()
                    .usuario(usuario)
                    .empresa(empresa)
                    .plataforma(plataforma)
                    .puesto(puestoLimpio)
                    .fechaSolicitud(fechaSolicitud != null ? fechaSolicitud : LocalDate.now())
                    .estadoActual(nuevoEstado)
                    .fechaUltimoCambio(ahora)
                    .confianza(BigDecimal.valueOf(0.95))
                    .claveDeduplicacion(claveDeduplicacion)
                    .build();

            HistorialEstado primerHistorial = HistorialEstado.builder()
                    .candidatura(candidatura)
                    .estadoAnterior(null)
                    .estadoNuevo(nuevoEstado)
                    .fechaEvento(ahora)
                    .origen(OrigenEvento.AUTOMATICO)
                    .build();

            candidatura.addHistorial(primerHistorial);
            return candidaturaRepository.save(candidatura);
        } else {
            if (candidatura.getEstadoActual() != nuevoEstado) {
                EstadoCandidatura estadoViejo = candidatura.getEstadoActual();
                HistorialEstado transicion = HistorialEstado.builder()
                        .candidatura(candidatura)
                        .estadoAnterior(estadoViejo)
                        .estadoNuevo(nuevoEstado)
                        .fechaEvento(ahora)
                        .origen(OrigenEvento.AUTOMATICO)
                        .build();

                candidatura.addHistorial(transicion);
                candidatura.setEstadoActual(nuevoEstado);
                candidatura.setFechaUltimoCambio(ahora);
                return candidaturaRepository.save(candidatura);
            }
            return candidatura;
        }
    }

    private Plataforma buscarOCrearPlataforma(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        String cod = codigo.trim().toUpperCase(Locale.ROOT);
        return plataformaRepository.findByCodigoIgnoreCase(cod)
                .orElseGet(() -> {
                    String nombre = switch (cod) {
                        case "MANFRED" -> "Manfred";
                        case "TICJOB" -> "Ticjob";
                        case "GREENHOUSE" -> "Greenhouse";
                        case "LEVER" -> "Lever";
                        case "WORKABLE" -> "Workable";
                        case "ASHBY" -> "Ashby";
                        case "SMARTRECRUITERS" -> "SmartRecruiters";
                        case "DOMESTIKA" -> "Domestika";
                        case "WELCOMETOTHEJUNGLE" -> "Welcome to the Jungle";
                        case "CORREO_DIRECTO" -> "Correo Directo";
                        default -> cod.charAt(0) + cod.substring(1).toLowerCase(Locale.ROOT);
                    };
                    Plataforma nueva = Plataforma.builder().codigo(cod).nombre(nombre).build();
                    try {
                        return plataformaRepository.save(nueva);
                    } catch (Exception e) {
                        return plataformaRepository.findByCodigoIgnoreCase(cod).orElse(null);
                    }
                });
    }

    private EstadoCandidatura mapearEstadoDetectado(String estadoDetectado) {
        if (estadoDetectado == null) return EstadoCandidatura.ENVIADA;
        return switch (estadoDetectado.toUpperCase(Locale.ROOT)) {
            case "REJECTED" -> EstadoCandidatura.RECHAZADA;
            case "OFFER" -> EstadoCandidatura.OFERTA;
            case "INTERVIEW" -> EstadoCandidatura.ENTREVISTA;
            case "IN_PROGRESS" -> EstadoCandidatura.EN_PROCESO;
            default -> EstadoCandidatura.ENVIADA;
        };
    }
    }
