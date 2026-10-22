package com.candidatae.domain;

/**
 * Proveedor de la bandeja de correo conectada.
 *
 * <p>En la versión inicial solo se soporta Gmail, pero el modelo deja la puerta
 * abierta a otros proveedores (Outlook, IMAP...) sin cambiar el esquema: la columna
 * {@code proveedor} de {@code cuenta_correo} es un VARCHAR.</p>
 */
public enum ProveedorCorreo {

    GMAIL
}
