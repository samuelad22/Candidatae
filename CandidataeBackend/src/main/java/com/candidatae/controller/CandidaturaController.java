package com.candidatae.controller;

import com.candidatae.dto.CandidaturaCreateRequest;
import com.candidatae.dto.CandidaturaResponse;
import com.candidatae.service.CandidaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
