const API_URL = ""

export async function fetchJson(endpoint, options = {}) {
    const token = localStorage.getItem('token')
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
    }

    const res = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers,
    })

    if (!res.ok) {
        if (res.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('userEmail')
            if (typeof window !== 'undefined' && window.location.pathname !== '/login' && window.location.pathname !== '/register') {
                window.location.href = '/login'
            }
        }

        let errorMessage = `Error ${res.status}: ${res.statusText}`
        try {
            const errorBody = await res.json()
            if (errorBody && errorBody.message) {
                errorMessage = errorBody.message
            } else if (errorBody && errorBody.error) {
                errorMessage = errorBody.error
            }
        } catch {
            // response was not JSON, fallback to status text
        }
        throw new Error(errorMessage)
    }

    if (res.status === 204) {
        return null
    }

    return res.json()
}
