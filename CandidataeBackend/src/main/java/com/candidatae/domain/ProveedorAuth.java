package com.candidatae.domain;

/**
 * Proveedor con el que el usuario se autenticó en Candidatae.
 *
 * <p>El identificador del usuario es siempre su email: cuando accede con Google o
 * LinkedIn, el sistema solo toma el email de esa cuenta. Este enumerado deja
 * constancia de <em>cómo</em> se autenticó, para poder auditarlo y para que en el
 * futuro el login social no se mezcle con la autenticación por contraseña.</p>
 *
 * <p>Se persiste como texto ({@code @Enumerated(EnumType.STRING)}) en la columna
 * {@code auth_provider} de la tabla {@code users}.</p>
 */
public enum ProveedorAuth {

    /**
     * Email y contraseña. Es el único flujo implementado en el Sprint 1.
     */
    LOCAL,

    /**
     * Google OAuth2 (login social). Previsto en la Fase 1 del proyecto.
     */
    GOOGLE,

    /**
     * LinkedIn OAuth2 (login social). Previsto en la Fase 1 del proyecto.
     */
    LINKEDIN
}
