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
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa un contacto asociado a una candidatura específica.
 */
@Entity
@Table(name = "contacto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;

    @Size(max = 150)
    @Column(length = 150)
    private String nombre;

    @Size(max = 255)
    @Column(length = 255)
    private String email;

    @Size(max = 100)
    @Column(length = 100)
    private String rol;
}
