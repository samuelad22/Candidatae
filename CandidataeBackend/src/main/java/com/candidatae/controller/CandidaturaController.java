package com.candidatae.controller;

import com.candidatae.dto.CandidaturaCreateRequest;
import com.candidatae.dto.CandidaturaDetailResponse;
import com.candidatae.dto.CandidaturaResponse;
import com.candidatae.dto.CandidaturaUpdateRequest;
import com.candidatae.dto.PageResponse;
import com.candidatae.service.CandidaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Candidatura creada exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o plataforma no soportada"),
                        @ApiResponse(responseCode = "409", description = "Candidatura duplicada para la misma empresa, puesto y plataforma")
        })
        @PostMapping
        public ResponseEntity<CandidaturaResponse> crear(@Valid @RequestBody CandidaturaCreateRequest request) {
                CandidaturaResponse response = candidaturaService.crear(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Lista y filtra las candidaturas del usuario autenticado con filtros
         * opcionales y paginación.
         */
        @Operation(summary = "Listar y filtrar candidaturas", description = "Devuelve el listado paginado de candidaturas del usuario autenticado ordenadas por fecha de último cambio descendente")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Listado de candidaturas obtenido correctamente"),
                        @ApiResponse(responseCode = "400", description = "Parámetros de filtrado inválidos (estado o plataforma no válidos)")
        })
        @GetMapping
        public ResponseEntity<PageResponse<CandidaturaResponse>> listar(
                        @Parameter(description = "Código de la plataforma de origen. Valores permitidos: LINKEDIN, INFOJOBS, INDEED, TECNOEMPLEO, GLASSDOOR", example = "LINKEDIN") @RequestParam(required = false) String platform,

                        @Parameter(description = "Estado de la candidatura. Valores permitidos: ENVIADA, EN_PROCESO, ENTREVISTA, OFERTA, RECHAZADA, DESCARTADA", example = "ENTREVISTA") @RequestParam(required = false) String status,

                        @Parameter(description = "Nombre o fragmento del nombre de la empresa", example = "Google") @RequestParam(required = false) String company,

                        @Parameter(description = "Fecha inicial de solicitud (formato ISO YYYY-MM-DD)", example = "2026-10-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

                        @Parameter(description = "Fecha final de solicitud (formato ISO YYYY-MM-DD)", example = "2026-10-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

                        @Parameter(description = "Búsqueda por texto libre en empresa, puesto o ubicación", example = "Backend") @RequestParam(required = false) String search,

                        @Parameter(description = "Número de página (iniciando en 0)", example = "0") @RequestParam(defaultValue = "0") int page,

                        @Parameter(description = "Tamaño de página (entre 1 y 100)", example = "20") @RequestParam(defaultValue = "20") int size) {
                PageResponse<CandidaturaResponse> candidaturas = candidaturaService.listar(platform, status, company,
                                from, to, search, page, size);
                return ResponseEntity.ok(candidaturas);
        }

        /**
         * Obtiene el detalle completo de una candidatura por su ID.
         */
        @Operation(summary = "Ver detalle de candidatura", description = "Devuelve los datos de una candidatura junto con su historial de estados, notas y contactos")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Detalle de la candidatura obtenido correctamente"),
                        @ApiResponse(responseCode = "404", description = "Candidatura no encontrada o no pertenece al usuario autenticado")
        })
        @GetMapping("/{id}")
        public ResponseEntity<CandidaturaDetailResponse> obtenerDetalle(@PathVariable Long id) {
                CandidaturaDetailResponse detalle = candidaturaService.obtenerDetalle(id);
                return ResponseEntity.ok(detalle);
        }

        /**
         * Actualiza los datos o el estado de una candidatura existente.
         */
        @Operation(summary = "Editar candidatura", description = "Actualiza los campos proporcionados de una candidatura. Si cambia el estado, se registra en el historial")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Candidatura actualizada exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o plataforma no soportada"),
                        @ApiResponse(responseCode = "404", description = "Candidatura no encontrada o no pertenece al usuario autenticado"),
                        @ApiResponse(responseCode = "409", description = "Ya tienes registrada otra candidatura para este puesto, empresa y plataforma")
        })
        @PutMapping("/{id}")
        public ResponseEntity<CandidaturaResponse> actualizar(
                        @PathVariable Long id,
                        @Valid @RequestBody CandidaturaUpdateRequest request) {
                CandidaturaResponse response = candidaturaService.actualizar(id, request);
                return ResponseEntity.ok(response);
        }

        /**
         * Elimina permanentemente una candidatura del usuario autenticado.
         */
        @Operation(summary = "Eliminar candidatura", description = "Elimina permanentemente una candidatura y sus datos asociados (historial, notas, contactos) si pertenece al usuario")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Candidatura eliminada exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Candidatura no encontrada o no pertenece al usuario autenticado")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> eliminar(@PathVariable Long id) {
                candidaturaService.eliminar(id);
                return ResponseEntity.noContent().build();
        }
}
