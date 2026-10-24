package com.candidatae.controller;

import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.dto.CandidaturaCreateRequest;
import com.candidatae.dto.CandidaturaResponse;
import com.candidatae.service.CandidaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para la gestión de candidaturas de empleo.
 * Expone las operaciones CRUD sobre el recurso /api/applications.
 */
@Tag(name = "Candidaturas", description = "Operaciones sobre las solicitudes de empleo del usuario")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    /**
     * Da de alta manualmente una nueva candidatura con estado inicial ENVIADA.
     */
    @Operation(summary = "Crear candidatura", description = "Da de alta manualmente una nueva candidatura con estado inicial ENVIADA")
    @PostMapping
    public ResponseEntity<CandidaturaResponse> crear(@Valid @RequestBody CandidaturaCreateRequest request) {
        CandidaturaResponse response = candidaturaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista y filtra las candidaturas del usuario autenticado con filtros opcionales.
     */
    @Operation(summary = "Listar y filtrar candidaturas", description = "Devuelve el listado de candidaturas del usuario autenticado ordenadas por fecha de último cambio descendente")
    @GetMapping
    public ResponseEntity<List<CandidaturaResponse>> listar(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) EstadoCandidatura status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search
    ) {
        List<CandidaturaResponse> candidaturas = candidaturaService.listar(platform, status, company, from, to, search);
        return ResponseEntity.ok(candidaturas);
    }
}
