import { fetchJson } from '../utils/fetch'
import { getToken } from './authService'

function getAuthHeaders() {
    const token = getToken()
    return {
        Authorization: `Bearer ${token}`
    }
}

export async function listarCuentasCorreo() {
    return fetchJson('/api/email-accounts', {
        headers: getAuthHeaders()
    })
}

export async function autorizarGmailOAuth() {
    return fetchJson('/api/email-accounts/gmail', {
        method: 'POST',
        headers: getAuthHeaders()
    })
}

export async function desconectarCuentaCorreo(id) {
    const res = await fetch(`/api/email-accounts/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
    })
    if (!res.ok) {
        throw new Error(`Error al desconectar la cuenta (${res.status})`)
    }
}