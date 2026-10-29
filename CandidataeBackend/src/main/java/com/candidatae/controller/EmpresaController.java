package com.candidatae.controller;

import com.candidatae.dto.EmpresaDto.EmpresaCreateRequest;
import com.candidatae.dto.EmpresaDto.EmpresaResponse;
import com.candidatae.dto.EmpresaDto.EmpresaUpdateRequest;
import com.candidatae.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para el CRUD de Empresas y consulta para filtros de candidaturas.
 */
@Tag(name = "Empresas", description = "Endpoints de gestión y consulta de empresas (CRUD y filtros de candidaturas)")
@RestController
@RequestMapping({"/api/companies", "/api/empresas"})
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    /**
     * Devuelve el listado alfabético de empresas registradas (para selectores y filtros).
     */
    @Operation(summary = "Listar empresas", description = "Devuelve el listado alfabético de empresas. Permite búsqueda parcial por nombre para autocompletado y filtros de candidaturas")
    @GetMapping
    public ResponseEntity<List<EmpresaResponse>> listar(
            @RequestParam(required = false) @Size(max = 100, message = "El término de búsqueda no puede superar los 100 caracteres") String search
    ) {
        List<EmpresaResponse> empresas = empresaService.listar(search);
        return ResponseEntity.ok(empresas);
    }

    /**
     * Obtiene el detalle de una empresa por su identificador.
     */
    @Operation(summary = "Obtener empresa por ID", description = "Devuelve los datos de una empresa específica")
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponse> obtenerPorId(@PathVariable Long id) {
        EmpresaResponse empresa = empresaService.obtenerPorId(id);
        return ResponseEntity.ok(empresa);
    }

    /**
     * Da de alta una nueva empresa en el sistema.
     */
    @Operation(summary = "Crear empresa", description = "Registra una nueva empresa con normalización automática de nombre")
    @PostMapping
    public ResponseEntity<EmpresaResponse> crear(@Valid @RequestBody EmpresaCreateRequest request) {
        EmpresaResponse creada = empresaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /**
     * Actualiza el nombre de una empresa existente.
     */
    @Operation(summary = "Actualizar empresa", description = "Actualiza el nombre comercial y re-normaliza el identificador de la empresa")
    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EmpresaUpdateRequest request
    ) {
        EmpresaResponse actualizada = empresaService.actualizar(id, request);
        return ResponseEntity.ok(actualizada);
    }

    /**
     * Elimina permanentemente una empresa si no tiene candidaturas vinculadas.
     */
    @Operation(summary = "Eliminar empresa", description = "Elimina permanentemente una empresa del catálogo si no tiene candidaturas asociadas")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        empresaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

