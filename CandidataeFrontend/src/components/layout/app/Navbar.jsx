import { NavLink, useNavigate } from 'react-router-dom'
import { logoutUser } from '../../../services/authService'
import ThemeToggle from '../ThemeToggler'

export default function Navbar() {
    const navigate = useNavigate()
    const userEmail = localStorage.getItem('userEmail') || 'Usuario'

    const handleLogout = () => {
        logoutUser()
        navigate('/login')
    }

    return (
        <nav className="navbar navbar-expand-lg navbar-dark bg-main border-bottom border-secondary border-opacity-25 py-2">
            <div className="container">
                <NavLink className="navbar-brand d-flex align-items-center gap-2 fw-bold text-uppercase" to="/dashboard">
                    <i className="fa-solid fa-briefcase text-accent fs-4"></i>
                    <span>Candidatae</span>
                </NavLink>

                {/* Enlaces principales */}
                <div className="navbar-nav mx-auto d-flex flex-row gap-4">
                    <NavLink to="/dashboard" className={({ isActive }) => `nav-link d-flex align-items-center gap-2 ${isActive ? 'active text-accent' : ''}`}>
                        <i className="fa-solid fa-house"></i> Inicio
                    </NavLink>
                    <NavLink to="/ofertas" className={({ isActive }) => `nav-link d-flex align-items-center gap-2 ${isActive ? 'active text-accent' : ''}`}>
                        <i className="fa-solid fa-briefcase"></i> Ofertas
                    </NavLink>
                    <NavLink to="/candidaturas" className={({ isActive }) => `nav-link d-flex align-items-center gap-2 ${isActive ? 'active text-accent' : ''}`}>
                        <i className="fa-solid fa-file-lines"></i> Candidaturas
                    </NavLink>
                    <NavLink to="/perfil" className={({ isActive }) => `nav-link d-flex align-items-center gap-2 ${isActive ? 'active text-accent' : ''}`}>
                        <i className="fa-solid fa-user"></i> Perfil
                    </NavLink>
                </div>

                {/* Info de usuario y ThemeToggle */}
                <div className="d-flex align-items-center gap-3">
                    <ThemeToggle />
                    <span className="small text-muted d-none d-md-inline">{userEmail}</span>
                    <button onClick={handleLogout} className="btn btn-sm btn-outline-danger rounded-circle" title="Cerrar sesión">
                        <i className="fa-solid fa-right-from-bracket"></i>
                    </button>
                </div>
            </div>
        </nav>
    )
}