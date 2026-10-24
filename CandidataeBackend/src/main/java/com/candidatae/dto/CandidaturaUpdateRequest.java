package com.candidatae.dto;

import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.Modalidad;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Petición de entrada para actualizar parcialmente una candidatura existente (PATCH).
 * Todos los campos son opcionales; solo los presentes (no nulos) se modificarán.
 */
public record CandidaturaUpdateRequest(

        @Size(max = 255, message = "El nombre de la empresa no puede superar los 255 caracteres")
        @JsonAlias({"empresaNombre", "nombreEmpresa"})
        String empresa,

        @Size(max = 50, message = "El código de plataforma no puede superar los 50 caracteres")
        String plataformaCodigo,

        @Size(max = 255, message = "El puesto no puede superar los 255 caracteres")
        String puesto,

        @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres")
        String ubicacion,

        Modalidad modalidad,

        @Size(max = 100, message = "El salario no puede superar los 100 caracteres")
        String salarioTexto,

        String enlaceOferta,

        LocalDate fechaSolicitud,

        EstadoCandidatura estadoActual
) {
}
