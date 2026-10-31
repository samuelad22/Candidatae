import { fetchJson } from '../utils/fetch'

/**
 * Lista las cuentas de correo vinculadas al usuario autenticado.
 */
export async function listarCuentasCorreo() {
    return fetchJson('/api/email-accounts')
}

/**
 * Vincula una cuenta de correo mediante IMAP con Contraseña de Aplicación de 16 caracteres.
 * @param {Object} data - { email: string, appPassword: string, provider: 'GMAIL' | 'OUTLOOK' }
 */
export async function conectarCuentaImap(data) {
    return fetchJson('/api/email-accounts/imap', {
        method: 'POST',
        body: JSON.stringify(data),
    })
}

/**
 * Ejecuta la sincronización manual bajo demanda de una cuenta específica.
 */
export async function sincronizarCuenta(id) {
    return fetchJson(`/api/email-accounts/${id}/sync`, {
        method: 'POST',
    })
}

/**
 * Sincroniza todas las cuentas activas del usuario en una sola llamada agregada.
 */
export async function sincronizarTodasLasCuentas() {
    return fetchJson('/api/email-accounts/sync', {
        method: 'POST',
    })
}

/**
 * Inicia el flujo alternativo de vinculación OAuth2 de Gmail.
 */
export async function autorizarGmailOAuth() {
    return fetchJson('/api/email-accounts/gmail', {
        method: 'POST',
    })
}

/**
 * Inicia el flujo alternativo de vinculación OAuth2 de Outlook / Microsoft.
 */
export async function autorizarOutlookOAuth() {
    return fetchJson('/api/email-accounts/outlook', {
        method: 'POST',
    })
}

/**
 * Desconecta lógicamente una cuenta de correo.
 */
export async function desconectarCuentaCorreo(id) {
    return fetchJson(`/api/email-accounts/${id}`, {
        method: 'DELETE',
    })
}
