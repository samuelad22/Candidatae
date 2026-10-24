package com.candidatae.service;

import com.candidatae.domain.Candidatura;
import com.candidatae.domain.Empresa;
import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.HistorialEstado;
import com.candidatae.domain.OrigenEvento;
import com.candidatae.domain.Plataforma;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CandidaturaCreateRequest;
import com.candidatae.dto.CandidaturaResponse;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CandidaturaRepository;
import com.candidatae.repository.EmpresaRepository;
import com.candidatae.repository.PlataformaRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
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
