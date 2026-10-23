import React from 'react'
import { NavLink } from 'react-router-dom'
import ThemeToggle from '../ThemeToggler'
import styles from '../../../styles/app/Navbar.module.css'

export default function Navbar() {
    return (
        <>
            <nav className={`navbar navbar-expand-lg d-none d-lg-flex ${styles['navbar-main']}`}>
                <div className="container d-flex justify-content-between align-items-center">
                    
                    <NavLink 
                        to="/panel" 
                        end
                        className={`text-decoration-none d-flex align-items-center gap-2 ${styles['brand-container']}`}
                    >
                        <div className={styles['logotype-icon']} role="img" aria-label="Candidatae Logotipo" />
                    </NavLink>

                    <div className="d-flex align-items-center gap-4">
                        <NavLink 
                            to="/panel" 
                            end 
                            className={({ isActive }) => `${styles['nav-link']} ${isActive ? styles.active : ''}`}
                        >
                            <i className="fa-solid fa-house me-2"></i>Inicio
                        </NavLink>

                        <NavLink 
                            to="/panel/ofertas" 
                            className={({ isActive }) => `${styles['nav-link']} ${isActive ? styles.active : ''}`}
                        >
                            <i className="fa-solid fa-briefcase me-2"></i>Ofertas
                        </NavLink>

                        <NavLink 
                            to="/panel/candidaturas" 
                            className={({ isActive }) => `${styles['nav-link']} ${isActive ? styles.active : ''}`}
                        >
                            <i className="fa-solid fa-file-lines me-2"></i>Candidaturas
                        </NavLink>

                        <NavLink 
                            to="/panel/perfil" 
                            className={({ isActive }) => `${styles['nav-link']} ${isActive ? styles.active : ''}`}
                        >
                            <i className="fa-solid fa-user me-2"></i>Perfil
                        </NavLink>
                    </div>

                    <div className="d-flex align-items-center gap-3">
                        <ThemeToggle />
                    </div>
                </div>
            </nav>

            <nav className={`d-lg-none ${styles['bottom-nav']}`}>
                <NavLink 
                    to="/panel" 
                    end 
                    className={({ isActive }) => `${styles['bottom-item']} ${isActive ? styles['bottom-active'] : ''}`}
                >
                    <i className="fa-solid fa-house"></i>
                    <span>Inicio</span>
                </NavLink>

                <NavLink 
                    to="/panel/ofertas" 
                    className={({ isActive }) => `${styles['bottom-item']} ${isActive ? styles['bottom-active'] : ''}`}
                >
                    <i className="fa-solid fa-briefcase"></i>
                    <span>Ofertas</span>
                </NavLink>

                <NavLink 
                    to="/panel/candidaturas" 
                    className={({ isActive }) => `${styles['bottom-item']} ${isActive ? styles['bottom-active'] : ''}`}
                >
                    <i className="fa-solid fa-file-lines"></i>
                    <span>Candidaturas</span>
                </NavLink>

                <NavLink 
                    to="/panel/perfil" 
                    className={({ isActive }) => `${styles['bottom-item']} ${isActive ? styles['bottom-active'] : ''}`}
                >
                    <i className="fa-solid fa-user"></i>
                    <span>Perfil</span>
                </NavLink>
            </nav>
        </>
    )
}