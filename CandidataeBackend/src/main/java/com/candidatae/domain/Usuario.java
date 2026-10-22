package com.candidatae.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Entidad JPA que representa a un usuario de Candidatae.
 *
 * <p>El identificador de sesión del usuario es su <strong>email</strong>: es el
 * "subject" del JWT y es lo que lo identifica aunque acceda con Google o LinkedIn.
 * Esta tabla guarda, además, su contraseña (hasheada), el proveedor con el que se
 * autenticó y su estado.</p>
 *
 * <p><strong>Por qué la tabla se llama {@code users} y no {@code usuario}:</strong>
 * la Fuente de Verdad del proyecto usa el término "usuario" para el concepto, y así se
 * llama la clase. Pero la tabla física se mantiene como {@code users} (decisión del
 * equipo) para no rehacer el esquema ya desplegado en los entornos de desarrollo.
 * La tabla "usuario" no era viable en su momento porque {@code user} es palabra
 * reservada en PostgreSQL; en cualquier caso, la nomenclatura física de las tablas
 * es una decisión de infraestructura y la de las clases es del modelo de dominio.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email del usuario: su identificador de sesión. Siempre en minúsculas.
     */
    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Contraseña hasheada con BCrypt. Es {@code null} si el usuario solo accede
     * mediante OAuth2 (Google o LinkedIn).
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Nombre visible del usuario.
     */
    @Size(max = 100)
    private String name;

    /**
     * Proveedor con el que se autenticó. Ver {@link ProveedorAuth}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private ProveedorAuth proveedorAuth = ProveedorAuth.LOCAL;

    /**
     * Fecha de alta en Candidatae. La gestiona la propia entidad.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Si el usuario está dado de baja. Un usuario inactivo no puede autenticarse.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.proveedorAuth == null) {
            this.proveedorAuth = ProveedorAuth.LOCAL;
        }
        normalizeEmail();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeEmail();
    }

    private void normalizeEmail() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
