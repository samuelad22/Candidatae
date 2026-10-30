import { fetchJson } from "../utils/fetch";

const BASE_URL = "/api/applications";

/**
 * Obtiene el listado de candidaturas del usuario autenticado con filtros opcionales.
 */
export async function getApplications(filters = {}) {
    const params = new URLSearchParams();
    if (filters.status) params.append("status", filters.status);
    if (filters.company) params.append("company", filters.company);
    if (filters.platform) params.append("platform", filters.platform);
    if (filters.from) params.append("from", filters.from);
    if (filters.to) params.append("to", filters.to);
    if (filters.search) params.append("search", filters.search);
    if (filters.page !== undefined && filters.page !== null) params.append("page", filters.page);
    if (filters.size !== undefined && filters.size !== null) params.append("size", filters.size);

    const query = params.toString() ? `?${params.toString()}` : "";
    return fetchJson(`${BASE_URL}${query}`);
}

/**
 * Detalle completo de una candidatura por su ID.
 */
export async function getApplicationById(id) {
    return fetchJson(`${BASE_URL}/${id}`);
}

/**
 * Da de alta manualmente una nueva candidatura.
 */
export async function createApplication(applicationData) {
    return fetchJson(BASE_URL, {
        method: "POST",
        body: JSON.stringify(applicationData),
    });
}

/**
 * Actualiza una candidatura (PUT/PATCH).
 */
export async function updateApplication(id, updateData) {
    return fetchJson(`${BASE_URL}/${id}`, {
        method: "PUT",
        body: JSON.stringify(updateData),
    });
}

/**
 * Elimina una candidatura.
 */
export async function deleteApplication(id) {
    return fetchJson(`${BASE_URL}/${id}`, {
        method: "DELETE",
    });
}

// Alias de retrocompatibilidad para los componentes existentes
export const getElementos = (filters) => getApplications(filters);
