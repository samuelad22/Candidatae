package com.candidatae.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Entidad JPA que representa una bandeja de entrada conectada por el usuario.
 *
 * <p>Mapeada sobre la tabla {@code cuenta_correo} (ver la migración
 * {@code V2__cuenta_correo.sql}).</p>
 *
 * <p>Dos campos exigen atención especial:</p>
 * <ul>
 *   <li>{@link #refreshTokenCifrado}: es un secreto que da acceso de lectura a todo
 *       el buzón. Se guarda cifrado con AES-256-GCM (ver
 *       {@code com.candidatae.security.TokenCipherService}) y está
 *       excluido del {@code toString()} para que nunca acabe en un log.</li>
 *   <li>{@link #cursorSincronizacion}: marca hasta dónde se ha leído el buzón, de
 *       modo que la siguiente sincronización solo lee lo nuevo.</li>
 * </ul>
 */
@Entity
@Table(name = "cuenta_correo",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cuenta_correo_usuario_email",
                columnNames = {"usuario_id", "email_cuenta"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario propietario de la cuenta. Toda consulta a esta entidad debe ir
     * filtrada por este usuario (aislamiento de datos).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor", nullable = false, length = 20)
    @Builder.Default
    private ProveedorCorreo proveedor = ProveedorCorreo.GMAIL;

    /**
     * Email real de la bandeja conectada (el del que nos autoriza Google).
     */
    @Column(name = "email_cuenta", nullable = false, length = 255)
    private String emailCuenta;

    /**
     * Refresh token de Google, cifrado. NUNCA se expone por la API ni se registra en logs.
     */
    @ToString.Exclude
    @Column(name = "refresh_token_cifrado", columnDefinition = "bytea")
    private byte[] refreshTokenCifrado;

    /**
     * Punto hasta el que se ha leído el buzón (último {@code historyId} de Gmail).
     */
    @Column(name = "cursor_sincronizacion", length = 255)
    private String cursorSincronizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conexion", nullable = false, length = 50)
    @Builder.Default
    private EstadoConexion estadoConexion = EstadoConexion.PENDIENTE;

    @Column(name = "fecha_conexion")
    private Instant fechaConexion;

    @Column(name = "fecha_desconexion")
    private Instant fechaDesconexion;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "uid_validity")
    private Long uidValidity;

    @Column(name = "last_uid")
    private Long lastUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 50)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.IDLE;
}
