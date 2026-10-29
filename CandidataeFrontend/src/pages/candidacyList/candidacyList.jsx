import { useEffect, useState, useCallback } from "react"
import { Link, useNavigate } from "react-router-dom"
import { getApplications } from "../../services/candidacyService"
import { sincronizarTodasLasCuentas } from "../../services/emailAccountService"
import { logoutUser } from "../../services/authService"
import CandidacyCard from "./candidacyCard"
import ThemeToggle from "../../components/layout/ThemeToggler"

function CandidacyList() {
    const navigate = useNavigate()
    const [candidacies, setCandidacies] = useState([])
    const [error, setError] = useState(null)
    const [loading, setLoading] = useState(true)
    const [syncing, setSyncing] = useState(false)
    const [syncMessage, setSyncMessage] = useState(null)

    const fetchCandidacies = useCallback(() => {
        setLoading(true)
        setError(null)
        getApplications()
            .then((data) => {
                const items = Array.isArray(data) ? data : (data?.content || [])
                setCandidacies(items)
            })
            .catch((requestError) => {
                setError(requestError)
            })
            .finally(() => {
                setLoading(false)
            })
    }, [])

    useEffect(() => {
        fetchCandidacies()
    }, [fetchCandidacies])

    const handleSync = async () => {
        setSyncing(true)
        setSyncMessage(null)
        try {
            const result = await sincronizarTodasLasCuentas()
            const totalCreated = result?.totalCandidaciesCreated || 0
            const totalUpdated = result?.totalCandidaciesUpdated || 0
            const totalScanned = result?.totalEmailsScanned || 0
            setSyncMessage(
                `Sincronización completada: ${totalScanned} correos analizados, ${totalCreated} candidaturas nuevas detectadas, ${totalUpdated} actualizaciones.`
            )
            fetchCandidacies()
        } catch (err) {
            setSyncMessage(`Aviso de sincronización: ${err.message || 'No se pudieron sincronizar las bandejas'}`)
        } finally {
            setSyncing(false)
        }
    }

    const handleLogout = () => {
        logoutUser()
        navigate('/login')
    }

    return (
        <div className="bg-main min-vh-100 d-flex flex-column">
            <header className="navbar navbar-expand-lg px-4 py-3 border-bottom d-flex justify-content-between align-items-center">
                <Link to="/" className="fs-4 fw-bold text-decoration-none text-primary">
                    Candidatae
                </Link>
                <div className="d-flex align-items-center gap-3">
                    <ThemeToggle />
                    <button onClick={handleLogout} className="btn btn-outline-secondary btn-sm">
                        Cerrar sesión
                    </button>
                </div>
            </header>

            <main className="container py-4 flex-grow-1">
                <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
                    <div>
                        <h2 className="fw-bold mb-1">Mis Candidaturas</h2>
                        <p className="text-muted small mb-0">
                            Listado y estado de tus postulaciones laborales
                        </p>
                    </div>
                    <div className="d-flex gap-2">
                        <button
                            onClick={handleSync}
                            disabled={syncing}
                            className="btn btn-primary d-flex align-items-center gap-2"
                        >
                            <i className={`fa fa-sync-alt ${syncing ? 'fa-spin' : ''}`}></i>
                            <span>{syncing ? 'Sincronizando correos...' : 'Sincronizar correos'}</span>
                        </button>
                    </div>
                </div>

                {syncMessage && (
                    <div className="alert alert-info alert-dismissible fade show" role="alert">
                        <i className="fa fa-info-circle me-2"></i>
                        {syncMessage}
                        <button
                            type="button"
                            className="btn-close"
                            onClick={() => setSyncMessage(null)}
                            aria-label="Cerrar"
                        ></button>
                    </div>
                )}

                {loading && (
                    <div className="text-center py-5">
                        <div className="spinner-border text-primary" role="status">
                            <span className="visually-hidden">Cargando...</span>
                        </div>
                        <p className="text-muted mt-2">Cargando candidaturas...</p>
                    </div>
                )}

                {error && (
                    <div className="alert alert-danger" role="alert">
                        <h5 className="alert-heading">Error al cargar candidaturas</h5>
                        <p className="mb-0">{error.message || 'No se pudieron recuperar las candidaturas.'}</p>
                    </div>
                )}

                {!loading && !error && candidacies.length === 0 && (
                    <div className="card text-center p-5 border-dashed">
                        <div className="my-3">
                            <i className="fa fa-briefcase fa-3x text-muted mb-3"></i>
                            <h4 className="fw-semibold">No hay candidaturas registradas</h4>
                            <p className="text-muted max-w-md mx-auto">
                                Conecta tu cuenta de Gmail u Outlook con una contraseña de aplicación y pulsa "Sincronizar correos" para importar automáticamente tus postulaciones.
                            </p>
                        </div>
                    </div>
                )}

                {!loading && candidacies.length > 0 && (
                    <div className="row g-3">
                        {candidacies.map((item) => (
                            <div key={item.id} className="col-12 col-md-6 col-lg-4">
                                <CandidacyCard
                                    puesto={item.puesto}
                                    empresa={item.empresaNombre || item.empresa || "Empresa Confidencial"}
                                    estado={item.estadoActual || item.estado || "ENVIADA"}
                                    fecha={item.fechaSolicitud || item.fecha || ""}
                                />
                            </div>
                        ))}
                    </div>
                )}
            </main>
        </div>
    )
}

export default CandidacyList
