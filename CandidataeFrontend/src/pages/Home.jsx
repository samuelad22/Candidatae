import { Link, useNavigate } from 'react-router-dom'
import ThemeToggle from '../components/layout/ThemeToggler'
import { getToken, logoutUser } from '../services/authService'

export default function Home() {
    const navigate = useNavigate()
    const token = getToken()
    const userEmail = localStorage.getItem('userEmail')

    const handleLogout = () => {
        logoutUser()
        navigate('/login')
    }

    return (
        <div className="bg-main min-vh-100 d-flex flex-column">
            <header className="navbar navbar-expand-lg px-4 py-3 border-bottom d-flex justify-content-between align-items-center">
                <div className="d-flex align-items-center gap-2">
                    <span className="fs-4 fw-bold text-primary">Candidatae</span>
                </div>
                <div className="d-flex align-items-center gap-3">
                    <ThemeToggle />
                    {token ? (
                        <>
                            <span className="text-muted small d-none d-sm-inline">{userEmail}</span>
                            <Link to="/dashboard" className="btn btn-primary btn-sm">
                                Mis Candidaturas
                            </Link>
                            <button onClick={handleLogout} className="btn btn-outline-secondary btn-sm">
                                Cerrar sesión
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className="btn btn-outline-primary btn-sm">
                                Iniciar sesión
                            </Link>
                            <Link to="/register" className="btn btn-primary btn-sm">
                                Registrarse
                            </Link>
                        </>
                    )}
                </div>
            </header>

            <main className="container my-auto py-5 text-center">
                <div className="row justify-content-center">
                    <div className="col-12 col-md-9 col-lg-8">
                        <span className="badge bg-primary-subtle text-primary mb-3 px-3 py-2 rounded-pill">
                            Gestión Inteligente de Empleo
                        </span>
                        <h1 className="display-4 fw-bold mb-3">
                            Tus candidaturas de empleo, sincronizadas automáticamente
                        </h1>
                        <p className="lead text-muted mb-4">
                            Candidatae centraliza tus postulaciones de LinkedIn, InfoJobs, Indeed y más conectando tus buzones de Gmail y Outlook mediante contraseñas seguras de aplicación.
                        </p>
                        <div className="d-flex justify-content-center gap-3 flex-wrap">
                            {token ? (
                                <Link to="/dashboard" className="btn btn-primary btn-lg px-4">
                                    Ir al Panel de Candidaturas
                                </Link>
                            ) : (
                                <>
                                    <Link to="/register" className="btn btn-primary btn-lg px-4">
                                        Crear cuenta gratis
                                    </Link>
                                    <Link to="/login" className="btn btn-outline-secondary btn-lg px-4">
                                        Acceder
                                    </Link>
                                </>
                            )}
                        </div>
                    </div>
                </div>
            </main>

            <footer className="py-3 text-center text-muted small border-top">
                &copy; {new Date().getFullYear()} Candidatae. Plataforma abierta de seguimiento de empleo.
            </footer>
        </div>
    )
}
