package com.candidatae.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa un portal o plataforma de empleo (LinkedIn, InfoJobs, Indeed, etc.).
 */
@Entity
@Table(name = "plataforma")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código identificador en mayúsculas (ej. LINKEDIN, INFOJOBS, INDEED).
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    /**
     * Nombre descriptivo de la plataforma (ej. LinkedIn, InfoJobs).
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Plataforma origen equivalente (ej. Glassdoor apunta a Indeed).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plataforma_origen_id")
    private Plataforma plataformaOrigen;
}
