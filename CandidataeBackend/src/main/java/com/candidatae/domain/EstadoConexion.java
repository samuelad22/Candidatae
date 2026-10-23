package com.candidatae.domain;

/**
 * Estado de la conexión entre una cuenta de correo y Candidatae.
 *
 * <p>Se persiste como texto ({@code @Enumerated(EnumType.STRING)}) para que el valor
 * almacenado siga siendo legible en la base de datos y en los logs.</p>
 */
public enum EstadoConexion {

    /**
     * La cuenta se ha creado manualmente (o el flujo OAuth aún no ha terminado)
     * y todavía no tiene token de acceso.
     */
    PENDIENTE,

    /**
     * Vinculada correctamente y con un refresh token válido.
     */
    ACTIVA,

    /**
     * Google ha revocado el token (el usuario quitó el acceso o se revocó la
     * conceded consent). La cuenta sigue siendo del usuario, pero hay que
     * volver a vincularla.
     */
    EXPIRADA,

    /**
     * La última operación (normalmente una sincronización) ha fallado.
     * El token puede seguir siendo válido: el error es de la operación, no del permiso.
     */
    CON_ERROR,

    /**
     * El usuario ha desconectado la cuenta. La fila se conserva para no perder
     * el histórico de sincronizaciones ni de correos procesados.
     */
    DESCONECTADA
}
