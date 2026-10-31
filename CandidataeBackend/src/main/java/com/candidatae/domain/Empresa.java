package com.candidatae.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa una empresa contratante.
 */
@Entity
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre comercial original tal como se ingresó (ej. Acme Spain S.L.).
     */
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String nombre;

    /**
     * Nombre normalizado para búsqueda y deduplicación (ej. acme).
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "nombre_normalizado", nullable = false, unique = true, length = 255)
    private String nombreNormalizado;
}
