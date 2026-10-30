import { fetchJson } from '../utils/fetch'

export async function loginUser(credentials) {
    const data = await fetchJson('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
    })
    
    if (data.token) {
        localStorage.setItem('token', data.token)
        localStorage.setItem('userEmail', data.email)
    }
    return data
}

export async function registerUser(userData) {
    const data = await fetchJson('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(userData),
    })
    
    if (data.token) {
        localStorage.setItem('token', data.token)
        localStorage.setItem('userEmail', data.email)
    }
    return data
}

export function logoutUser() {
    localStorage.removeItem('token')
    localStorage.removeItem('userEmail')
}

export function getToken() {
    return localStorage.getItem('token')
}