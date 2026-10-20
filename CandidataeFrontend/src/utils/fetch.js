const API_URL = ""

export async function fetchJson(endopoint, options = {}){
    const res = await fetch(`${API_URL}${endpoint}`, {headers: {"Content-Type": "application/json", ...options.headers},
        ...options,})

        if(!res.ok){
            throw new Error(`Error ${res.status}: ${res.statusText}`)
        }
        return res.json();
    }

