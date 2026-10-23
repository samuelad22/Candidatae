import { Link } from 'react-router-dom'
import laptopMockup from '../../../assets/img/example-laptop.png' // Tu imagen de la laptop con transparencia/fondo oscuro
import styles from './Hero.module.css'

export default function Hero() {
    return (
        <section className={`${styles['hero-section']}`}>
            <div className="py-lg-4">
                <div className="row align-items-center g-4">
                    
                    <div className="col-lg-6 z-2">
                        {/* Badge */}
                        <div className={`d-inline-flex align-items-center px-3 py-1 mb-4 rounded-pill ${styles['hero-badge']}`}>
                            <span className="small fw-medium">Gestor de solicitudes de empleo</span>
                        </div>

                        {/* TITLE */}
                        <h1 className={`display-4 fw-bold mb-3 ${styles['hero-title']}`}>
                            Todas tus candidaturas <br className="d-none d-md-block" />
                            <span className={styles['text-accent']}>en un solo lugar</span>
                        </h1>

                        {/* SUBTITLE */}
                        <p className={`lead mb-4 ${styles['hero-description']}`}>
                            <strong>CANDIDATAE</strong> lee tus correos de empleo, interpreta la información con inteligencia artificial y organiza todo tu proceso en un único dashboard.
                        </p>

                        {/* ACTIONS */}
                        <div className="d-flex flex-wrap align-items-center gap-3 mb-5">
                            <Link to="/register" className="btn btn-primary rounded-pill px-4 py-3 fw-semibold d-inline-flex align-items-center gap-2">
                                <i className="fa-solid fa-user-plus"></i>
                                <span>Comenzar ahora</span>
                                <i className="fa-solid fa-arrow-right ms-1"></i>
                            </Link>

                            <Link to="/como-usar" className="btn btn-tertiary rounded-pill px-4 py-3 fw-semibold d-inline-flex align-items-center gap-2">
                                <span>Ver cómo funciona</span>
                            </Link>
                        </div>

                        <div className="pt-3">
                            <div className="d-flex align-items-center gap-4 text-center">
                                <div className={styles['platform-item']}>
                                    <i className="fa-brands fa-linkedin fs-3 mb-1"></i>
                                    <span className="d-block text-muted">LinkedIn</span>
                                </div>
                                <div className={styles['platform-item']}>
                                    <i className="fa-solid fa-briefcase fs-3 mb-1"></i>
                                    <span className="d-block text-muted">InfoJobs</span>
                                </div>
                                <div className={styles['platform-item']}>
                                    <i className="fa-solid fa-circle-info fs-3 mb-1"></i>
                                    <span className="d-block text-muted">Indeed</span>
                                </div>
                                <div className={styles['platform-item']}>
                                    <i className="fa-solid fa-code fs-3 mb-1"></i>
                                    <span className="d-block text-muted">Tecnoempleo</span>
                                </div>
                                <div className={styles['platform-item']}>
                                    <i className="fa-solid fa-building fs-3 mb-1"></i>
                                    <span className="d-block text-muted">Glassdoor</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="col-lg-6 position-relative z-1">
                        <div className={styles['laptop-container']}>
                            <img 
                                src={laptopMockup} 
                                alt="Candidatae Dashboard en Portátil" 
                                className={`img-fluid ${styles['laptop-image']}`}
                            />
                        </div>
                    </div>

                </div>
            </div>
        </section>
    )
}