package com.candidatae.controller;

import com.candidatae.dto.PlataformaDto.PlataformaCreateRequest;
import com.candidatae.dto.PlataformaDto.PlataformaResponse;
import com.candidatae.dto.PlataformaDto.PlataformaUpdateRequest;
import com.candidatae.service.PlataformaService;
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
 * Controlador REST para el CRUD de Plataformas de empleo y alimentacion de selectores/filtros.
 */
@Tag(name = "Plataformas", description = "Endpoints de gestion y consulta del catalogo de plataformas de empleo")
@RestController
@RequestMapping({"/api/platforms", "/api/plataformas"})
@RequiredArgsConstructor
public class PlataformaController {

    private final PlataformaService plataformaService;

    /**
     * Devuelve el catalogo alfabetico de plataformas de empleo.
     */
    @Operation(summary = "Listar plataformas", description = "Devuelve el listado ordenado de plataformas para filtros y selectores")
    @GetMapping
    public ResponseEntity<List<PlataformaResponse>> listar(
            @RequestParam(required = false) @Size(max = 100, message = "El termino de busqueda no puede superar los 100 caracteres") String search
    ) {
        List<PlataformaResponse> plataformas = plataformaService.listar(search);
        return ResponseEntity.ok(plataformas);
    }

    /**
     * Obtiene el detalle de una plataforma por su identificador numerico.
     */
    @Operation(summary = "Obtener plataforma por ID", description = "Devuelve los datos de una plataforma especifica")
    @GetMapping("/{id}")
    public ResponseEntity<PlataformaResponse> obtenerPorId(@PathVariable Long id) {
        PlataformaResponse plataforma = plataformaService.obtenerPorId(id);
        return ResponseEntity.ok(plataforma);
    }

    /**
     * Obtiene el detalle de una plataforma por su codigo identificador (ej. LINKEDIN).
     */
    @Operation(summary = "Obtener plataforma por codigo", description = "Devuelve los datos de una plataforma por su codigo unico")
    @GetMapping("/code/{codigo}")
    public ResponseEntity<PlataformaResponse> obtenerPorCodigo(@PathVariable String codigo) {
        PlataformaResponse plataforma = plataformaService.obtenerPorCodigo(codigo);
        return ResponseEntity.ok(plataforma);
    }

    /**
     * Da de alta una nueva plataforma en el catalogo.
     */
    @Operation(summary = "Crear plataforma", description = "Registra una nueva plataforma de empleo en el sistema")
    @PostMapping
    public ResponseEntity<PlataformaResponse> crear(@Valid @RequestBody PlataformaCreateRequest request) {
        PlataformaResponse creada = plataformaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /**
     * Actualiza los datos de una plataforma existente.
     */
    @Operation(summary = "Actualizar plataforma", description = "Modifica los datos de una plataforma existente")
    @PutMapping("/{id}")
    public ResponseEntity<PlataformaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlataformaUpdateRequest request
    ) {
        PlataformaResponse actualizada = plataformaService.actualizar(id, request);
        return ResponseEntity.ok(actualizada);
    }

    /**
     * Elimina permanentemente una plataforma si no tiene candidaturas asociadas.
     */
    @Operation(summary = "Eliminar plataforma", description = "Elimina una plataforma si no tiene candidaturas vinculadas")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        plataformaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

