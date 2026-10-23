import { useState } from 'react'
import { NavLink, Link } from 'react-router-dom'
import ThemeToggle from '../ThemeToggler'
import styles from './Navbar.module.css'

export default function Navbar() {
    const [isOpen, setIsOpen] = useState(false)
    const closeNavbar = () => setIsOpen(false)

    return (
        <>
            <nav className={`navbar navbar-expand-lg navbar-dark border-bottom border-secondary border-opacity-25 px-3 px-lg-4 ${styles['navbar-main']}`}>
                <div className="container d-flex justify-content-between align-items-center">
                    
                    {/* LOGOTYPE */}
                    <NavLink 
                        to="/" 
                        onClick={closeNavbar}
                        className={`text-decoration-none d-flex align-items-center gap-3 ${styles['brand-container']}`}
                    >
                        <div className={styles['logotype-icon']} role="img" aria-label="Candidatae Logotipo" />
                    </NavLink>

                    {/* MENU */}
                    <div className="d-none d-lg-flex align-items-center gap-5">
                        <NavLink 
                            end 
                            to="/" 
                            className={({ isActive }) => `fw-semibold ${styles['nav-link']} ${isActive ? styles.active : ''}`}
                        >
                            Inicio
                        </NavLink>
                        
                        <NavLink 
                            to="/guia" 
                            className={({ isActive }) => `fw-semibold ${styles['nav-link']} ${isActive ? styles.active : ''}`}
                        >
                            Cómo usar
                        </NavLink>

                        <NavLink 
                            to="/colaboradores" 
                            className={({ isActive }) => `fw-semibold ${styles['nav-link']} ${isActive ? styles.active : ''}`}
                        >
                            Colaboradores
                        </NavLink>
                    </div>

                    {/* ACTIONS RIGHT */}
                    <div className="d-none d-lg-flex align-items-center gap-3">
                        <ThemeToggle />
                        <Link to="/login" className="btn btn-tertiary rounded-pill px-4 py-2 fw-semibold">
                            Iniciar sesión
                        </Link>
                    </div>

                    {/* BURGER BUTTON PARA MÓVIL */}
                    {!isOpen && (
                        <button
                            className={`btn border-0 p-0 shadow-none d-lg-none ${styles['hamburger-btn']}`}
                            onClick={() => setIsOpen(true)}
                            aria-label="Abrir Menú"
                        >
                            <span className={styles.bar}></span>
                            <span className={styles.bar}></span>
                            <span className={styles.bar}></span>
                        </button>
                    )}
                </div>
            </nav>

            {/* BACKDROP MÓVIL */}
            <div 
                className={`${styles.backdrop} ${isOpen ? styles['show-backdrop'] : ''}`}
                onClick={closeNavbar}
            ></div>

            {/* DRAWER MÓVIL */}
            <div className={`${styles.drawer} ${isOpen ? styles['show-drawer'] : ''}`}>
                <div className={`p-4 border-bottom position-relative d-flex flex-column align-items-start ${styles['drawer-header']}`}>
                    <button
                        className={`btn border-0 p-0 shadow-none ${styles['close-x-btn']}`}
                        onClick={closeNavbar}
                        aria-label="Cerrar Menú"
                    >
                        <span className={styles['x-bar-1']}></span>
                        <span className={styles['x-bar-2']}></span>
                    </button>
                    <NavLink 
                        to="/" 
                        onClick={closeNavbar}
                        className={`text-decoration-none d-flex align-items-center gap-3 ${styles['brand-container']}`}
                    >
                        <div className={styles['logotype-icon']} role="img" aria-label="Candidatae Logotipo" />
                    </NavLink>
                </div>

                <div className="d-flex flex-column p-3 gap-2">
                    <NavLink 
                        end 
                        to="/" 
                        onClick={closeNavbar}
                        className={({ isActive }) => `p-2 text-decoration-none fw-semibold rounded ${isActive ? styles['drawer-active'] : 'text-body'}`}
                    >
                        Inicio
                    </NavLink>
                    <NavLink 
                        to="/como-usar" 
                        onClick={closeNavbar}
                        className={({ isActive }) => `p-2 text-decoration-none fw-semibold rounded ${isActive ? styles['drawer-active'] : 'text-body'}`}
                    >
                        Cómo usar
                    </NavLink>
                    <NavLink 
                        to="/colaboradores" 
                        onClick={closeNavbar}
                        className={({ isActive }) => `p-2 text-decoration-none fw-semibold rounded ${isActive ? styles['drawer-active'] : 'text-body'}`}
                    >
                        Colaboradores
                    </NavLink>
                </div>

                <div className="p-4 border-top mt-auto d-flex flex-column gap-3">
                    <Link to="/login" onClick={closeNavbar} className="btn btn-primary w-100 rounded-pill py-2 fw-semibold">
                        Iniciar sesión
                    </Link>
                </div>
            </div>
        </>
    )
}