import { fetchJson } from '../utils/fetch';

/**
 * Consulta el estado del consentimiento de términos y condiciones del usuario en backend.
 */
export async function getTermsStatus() {
    return fetchJson('/api/users/terms');
}

/**
 * Registra la aceptación o denegación de términos y condiciones en la base de datos (RGPD Art. 5.2).
 */
export async function updateTermsAcceptance(accepted, version = '1.0') {
    return fetchJson('/api/users/terms', {
        method: 'POST',
        body: JSON.stringify({ accepted, version }),
    });
}
