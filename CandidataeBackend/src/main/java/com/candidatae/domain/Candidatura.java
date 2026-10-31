package com.candidatae.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA principal que representa una candidatura o solicitud de empleo.
 */
@Entity
@Table(name = "candidatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario propietario de la candidatura para aislamiento multiusuario.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Empresa a la que se postula la candidatura.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /**
     * Plataforma o portal de empleo de origen (LinkedIn, InfoJobs, etc.).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plataforma_id")
    private Plataforma plataforma;

    /**
     * Título o puesto de trabajo ofertado.
     */
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String puesto;

    /**
     * Ubicación geográfica o ciudad.
     */
    @Size(max = 255)
    @Column(length = 255)
    private String ubicacion;

    /**
     * Modalidad de trabajo (PRESENCIAL, HIBRIDO, REMOTO).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Modalidad modalidad;

    /**
     * Salario ofrecido en texto libre si consta (ej. 35.000€ - 42.000€).
     */
    @Size(max = 100)
    @Column(name = "salario_texto", length = 100)
    private String salarioTexto;

    /**
     * Enlace web original a la oferta de empleo.
     */
    @Column(name = "enlace_oferta", columnDefinition = "TEXT")
    private String enlaceOferta;

    /**
     * Fecha en que se envió la solicitud.
     */
    @NotNull
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;

    /**
     * Estado actual en la máquina de estados. Inicialmente ENVIADA.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 50)
    @Builder.Default
    private EstadoCandidatura estadoActual = EstadoCandidatura.ENVIADA;

    /**
     * Momento del último cambio o actualización de estado.
     */
    @NotNull
    @Column(name = "fecha_ultimo_cambio", nullable = false)
    private Instant fechaUltimoCambio;

    /**
     * Grado de confianza si los datos se extrajeron automáticamente (0.00 a 1.00).
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal confianza;

    /**
     * Clave normalizada para deduplicación de candidaturas.
     */
    @Size(max = 255)
    @Column(name = "clave_deduplicacion", length = 255)
    private String claveDeduplicacion;

    /**
     * Si fue fusionada manualmente con otra, referencia a la candidatura resultante.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fusionada_en_id")
    private Candidatura fusionadaEn;

    /**
     * Historial de estados por los que ha pasado esta candidatura.
     */
    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "candidatura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HistorialEstado> historiales = new ArrayList<>();

    /**
     * Contactos vinculados a esta candidatura.
     */
    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "candidatura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Contacto> contactos = new ArrayList<>();

    /**
     * Notas personales agregadas a esta candidatura.
     */
    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "candidatura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Nota> notas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.fechaUltimoCambio == null) {
            this.fechaUltimoCambio = now;
        }
        if (this.fechaSolicitud == null) {
            this.fechaSolicitud = LocalDate.now();
        }
        if (this.estadoActual == null) {
            this.estadoActual = EstadoCandidatura.ENVIADA;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaUltimoCambio = Instant.now();
    }

    public void addHistorial(HistorialEstado historial) {
        if (this.historiales == null) {
            this.historiales = new ArrayList<>();
        }
        this.historiales.add(historial);
        historial.setCandidatura(this);
    }

    public void addContacto(Contacto contacto) {
        if (this.contactos == null) {
            this.contactos = new ArrayList<>();
        }
        this.contactos.add(contacto);
        contacto.setCandidatura(this);
    }

    public void addNota(Nota nota) {
        if (this.notas == null) {
            this.notas = new ArrayList<>();
        }
        this.notas.add(nota);
        nota.setCandidatura(this);
    }
}
