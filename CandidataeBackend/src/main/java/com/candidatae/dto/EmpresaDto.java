package com.candidatae.dto;

import com.candidatae.domain.Empresa;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Objects para la gestión y consulta de Empresas.
 */
public final class EmpresaDto {

    private EmpresaDto() {
    }

    /**
     * Respuesta pública con los datos de una empresa.
     */
    public record EmpresaResponse(
            Long id,
            String nombre,
            String nombreNormalizado
    ) {
        public static EmpresaResponse fromEntity(Empresa empresa) {
            return new EmpresaResponse(
                    empresa.getId(),
                    empresa.getNombre(),
                    empresa.getNombreNormalizado()
            );
        }
    }

    /**
     * Petición para dar de alta una nueva empresa.
     */
    public record EmpresaCreateRequest(
            @NotBlank(message = "El nombre de la empresa es obligatorio")
            @Size(max = 255, message = "El nombre de la empresa no puede superar los 255 caracteres")
            @JsonAlias({"name", "company", "empresa", "companyName"})
            String nombre
    ) {
    }

    /**
     * Petición para modificar los datos de una empresa existente.
     */
    public record EmpresaUpdateRequest(
            @NotBlank(message = "El nombre de la empresa es obligatorio")
            @Size(max = 255, message = "El nombre de la empresa no puede superar los 255 caracteres")
            @JsonAlias({"name", "company", "empresa", "companyName"})
            String nombre
    ) {
    }
}

