package com.candidatae.dto;

import com.candidatae.domain.Modalidad;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Petición de entrada para dar de alta manualmente una nueva candidatura.
 * Contiene los datos de la oferta y la empresa introducidos por el usuario.
 */
public record CandidaturaCreateRequest(

        @NotBlank(message = "El nombre de la empresa es obligatorio")
        @Size(max = 255, message = "El nombre de la empresa no puede superar los 255 caracteres")
        @JsonAlias({"empresaNombre", "nombreEmpresa"})
        String empresa,

        @NotBlank(message = "El puesto es obligatorio")
        @Size(max = 255, message = "El puesto no puede superar los 255 caracteres")
        String puesto,

        @Size(max = 50, message = "El código de plataforma no puede superar los 50 caracteres")
        String plataformaCodigo,

        @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres")
        String ubicacion,

        Modalidad modalidad,

        @Size(max = 100, message = "El salario no puede superar los 100 caracteres")
        String salarioTexto,

        String enlaceOferta,

        @NotNull(message = "La fecha de solicitud es obligatoria")
        LocalDate fechaSolicitud
) {
}
