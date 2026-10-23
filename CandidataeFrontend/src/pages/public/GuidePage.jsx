import { Link } from 'react-router-dom';
import laptopImg from '../../assets/img/example-laptop.jpeg';
import styles from '../../styles/public/GuidePage.module.css';

export default function GuidePage() {
    return (
        <div className={`container ${styles['page-wrapper']}`}>
            
            <header className={`text-center py-5 mx-auto position-relative ${styles['guide-header']}`}>
                <div className={styles['header-glow']}></div>
                
                <span className={`badge rounded-pill px-3 py-2 mb-3 ${styles['badge-category']}`}>
                    <i className="fas fa-person-chalkboard me-2"></i>GUÍA DE USUARIO
                </span>
                
                <h1 className="display-3 fw-bold mt-2 mb-4">
                    Tu proceso de selección, <br />
                    <span className={styles['text-accent']}>totalmente automatizado</span>
                </h1>
                
                <p className={`lead fs-4 lh-base mx-auto ${styles['header-lead']}`}>
                    Conecta tu Gmail y deja que <strong className="">CANDIDATAE</strong> escanee, interprete con IA y organice tus solicitudes de empleo en tiempo real sin mover un solo dedo.
                </p>
            </header>


            <div className="row g-5 my-4 align-items-center position-relative">
                
                {/* PASO 1 */}
                <div className="col-lg-6">
                    <div className={`p-4 p-md-5 rounded-4 position-relative overflow-hidden ${styles['step-card']}`}>
                        <div className={styles['step-number-bg']}>01</div>
                        
                        <div className="d-flex align-items-center gap-3 mb-4">
                            <div className={styles['icon-box']}>
                                <i className="fas fa-shield-alt fa-lg text-lime"></i>
                            </div>
                            <span className={styles['step-badge']}>PASO 01 · SINCRONIZACIÓN</span>
                        </div>

                        <h3 className="h2 fw-bold  mb-3">Conecta tu cuenta de Gmail</h3>
                        <p className={`fs-6 lh-relaxed mb-4 ${styles['step-text']}`}>
                            Autoriza el acceso seguro mediante OAuth2. Nuestro sistema filtrará únicamente las notificaciones de portales de empleo como LinkedIn, InfoJobs, Indeed, Tecnoempleo y Glassdoor.
                        </p>

                        <ul className="list-unstyled d-flex flex-column gap-3 m-0">
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Acceso limitado: Cero lectura de correos personales</span>
                            </li>
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Cifrado de extremo a extremo (Cumplimiento RGPD)</span>
                            </li>
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Revoca el permiso en cualquier momento con 1 clic</span>
                            </li>
                        </ul>
                    </div>
                </div>

                <div className="col-lg-6">
                    <div className={`p-3 rounded-4 position-relative ${styles['preview-container']}`}>
                        <div className={styles['preview-overlay']}></div>
                        <img 
                            src={laptopImg} 
                            alt="Conexión y sincronización con Gmail" 
                            className={`img-fluid rounded-3 ${styles['preview-image']}`} 
                        />
                    </div>
                </div>

                {/* PASO 2 */}
                <div className="col-lg-6 order-lg-2">
                    <div className={`p-4 p-md-5 rounded-4 position-relative overflow-hidden ${styles['step-card']}`}>
                        <div className={styles['step-number-bg']}>02</div>

                        <div className="d-flex align-items-center gap-3 mb-4">
                            <div className={styles['icon-box']}>
                                <i className="fas fa-brain fa-lg text-lime"></i>
                            </div>
                            <span className={styles['step-badge']}>PASO 02 · INTELIGENCIA ARTIFICIAL</span>
                        </div>

                        <h3 className="h2 fw-bold  mb-3">Interpretación con IA</h3>
                        <p className={`fs-6 lh-relaxed mb-4 ${styles['step-text']}`}>
                            Nuestro modelo de lenguaje (LLM) analiza el contenido de los mensajes entrantes para identificar el puesto, la empresa, la fecha y el estado exacto de tu solicitud.
                        </p>

                        <ul className="list-unstyled d-flex flex-column gap-3 m-0">
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Extracción automática de datos (puesto, empresa, salario)</span>
                            </li>
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Actualización instantánea de fases (Enviada → Entrevista)</span>
                            </li>
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Detección inteligente de duplicados entre plataformas</span>
                            </li>
                        </ul>
                    </div>
                </div>

                <div className="col-lg-6 order-lg-1">
                    <div className={`p-3 rounded-4 position-relative ${styles['preview-container']}`}>
                        <div className={styles['preview-overlay']}></div>
                        <img 
                            src={laptopImg} 
                            alt="Procesamiento y análisis inteligente por IA" 
                            className={`img-fluid rounded-3 ${styles['preview-image']}`} 
                        />
                    </div>
                </div>

                {/* PASO 3 */}
                <div className="col-lg-6">
                    <div className={`p-4 p-md-5 rounded-4 position-relative overflow-hidden ${styles['step-card']}`}>
                        <div className={styles['step-number-bg']}>03</div>

                        <div className="d-flex align-items-center gap-3 mb-4">
                            <div className={styles['icon-box']}>
                                <i className="fas fa-chart-line fa-lg text-lime"></i>
                            </div>
                            <span className={styles['step-badge']}>PASO 03 · VISUALIZACIÓN</span>
                        </div>

                        <h3 className="h2 fw-bold  mb-3">Control total en tu Dashboard</h3>
                        <p className={`fs-6 lh-relaxed mb-4 ${styles['step-text']}`}>
                            Consulta todas tus ofertas en un solo lugar. Analiza tu progreso mediante el embudo de conversión y aplica filtros por portal, estado o rango de fechas.
                        </p>

                        <ul className="list-unstyled d-flex flex-column gap-3 m-0">
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Embudo visual de conversión y tasa de respuesta</span>
                            </li>
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Añade notas, contactos y recordatorios por solicitud</span>
                            </li>
                            <li className="d-flex align-items-center gap-3">
                                <i className="fas fa-check-circle text-lime fa-lg"></i>
                                <span className={styles['step-text']}>Alertas de candidaturas sin movimiento prolongado</span>
                            </li>
                        </ul>
                    </div>
                </div>

                <div className="col-lg-6">
                    <div className={`p-3 rounded-4 position-relative ${styles['preview-container']}`}>
                        <div className={styles['preview-overlay']}></div>
                        <img 
                            src={laptopImg} 
                            alt="Dashboard interactivo de candidaturas" 
                            className={`img-fluid rounded-3 ${styles['preview-image']}`} 
                        />
                    </div>
                </div>

            </div>

            {/* Banner CTA Destacado */}
            <section className={`text-center my-5 p-5 rounded-4 position-relative overflow-hidden ${styles['cta-card']}`}>
                <div className={styles['cta-glow']}></div>
                <div className="position-relative z-2">
                    <h2 className="display-5 fw-bold  mb-3">
                        Olvídate de las hojas de cálculo
                    </h2>
                    <p className={`fs-5 mb-4 mx-auto ${styles['cta-description']}`}>
                        Empieza a rastrear automáticamente tus oportunidades de empleo hoy mismo.
                    </p>
                    <div className="d-flex justify-content-center gap-3">
                        <Link to="/registro" className="btn btn-primary fw-bold px-4 py-3">
                            Comenzar ahora gratis
                        </Link>
                        <Link to="/login" className="btn btn-secondary fw-bold px-4 py-3">
                            Iniciar sesión
                        </Link>
                    </div>
                </div>
            </section>
        </div>
    );
}