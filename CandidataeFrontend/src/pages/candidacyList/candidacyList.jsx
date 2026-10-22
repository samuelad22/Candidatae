import { useEffect, useState } from "react"
import { getElementos } from "../../services/candidacyService"
import CandidacyCard from "./CandidacyCard"

function CandidacyList() {
    const [candidacies, setCandidacies] = useState([])
    const [error, setError] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let cancelado = false

        getElementos()
            .then((data) => {
                if (!cancelado) setCandidacies(Array.isArray(data) ? data : [])
            })
            .catch((requestError) => {
                if (!cancelado) setError(requestError)
            })
            .finally(() => {
                if (!cancelado) setLoading(false)
            })

        return () => {
            cancelado = true
        }
    }, [])

    if (loading) return <p>Cargando candidaturas...</p>
    if (error) return <p role="alert">No se pudieron cargar las candidaturas.</p>
    if (candidacies.length === 0) return <p>No hay candidaturas todavía.</p>

    return (
        <section>
            {candidacies.map(({ id, puesto, empresa, estado, fecha }) => (
                <CandidacyCard
                    key={id}
                    puesto={puesto}
                    empresa={empresa}
                    estado={estado}
                    fecha={fecha}
                />
            ))}
        </section>
    )
}

export default CandidacyList