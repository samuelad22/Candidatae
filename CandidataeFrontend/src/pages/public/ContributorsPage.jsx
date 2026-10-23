import React from 'react';
import contributorsData from '../../data/contributors.json';
import styles from '../../styles/public/ContributorsPage.module.css';

export default function ContributorsPage() {
    return (
        <div className={`container ${styles['page-wrapper']}`}>
            
            <header className={`text-center py-5 mx-auto position-relative ${styles['contributors-header']}`}>
                <div className={styles['header-glow']}></div>
                
                <span className={`badge rounded-pill px-3 py-2 mb-3 ${styles['badge-category']}`}>
                    <i className="fas fa-users me-2"></i>EQUIPO DE DESARROLLO
                </span>
                
                <h1 className="display-3 fw-bold  mt-2 mb-4">
                    Mentes detrás de <span className={styles['text-accent']}>CANDIDATAE</span>
                </h1>
                
                <p className={`lead fs-4 lh-base mx-auto ${styles['header-lead']}`}>
                    Un equipo multidisciplinar enfocado en transformar la búsqueda de empleo mediante automatización inteligente, arquitectura robusta y diseño de vanguardia.
                </p>
            </header>

            {/* Perfiles ordenados alfabéticamente */}
            <div className="row g-4 my-4">
                {contributorsData.map((contributor) => (
                    <div className="col-md-6 col-lg-4" key={contributor.id}>
                        <div className={`card h-100 p-4 rounded-4 position-relative overflow-hidden ${styles['contributor-card']}`}>
                            <div className={styles['card-glow']}></div>
                            
                            <div className="text-center position-relative z-2">
                                <div className={styles['avatar-container']}>
                                    <img 
                                        src={contributor.image} 
                                        alt={contributor.name} 
                                        className={styles['contributor-avatar']} 
                                    />
                                </div>
                                
                                <h3 className="h4 fw-bold  mt-3 mb-1">{contributor.name}</h3>
                                
                                <span className={`badge mb-3 ${styles['role-badge']}`}>
                                    {contributor.role}
                                </span>
                                
                                <p className={`fs-6 lh-relaxed mb-4 ${styles['contributor-bio']}`}>
                                    {contributor.bio}
                                </p>

                                <div className="d-flex justify-content-center gap-3 pt-3 border-top">
                                    <a 
                                        href={contributor.linkedin} 
                                        target="_blank" 
                                        rel="noopener noreferrer" 
                                        className={styles['social-link']}
                                        aria-label="LinkedIn"
                                    >
                                        <i className="fab fa-linkedin fa-lg"></i>
                                    </a>
                                    <a 
                                        href={contributor.github} 
                                        target="_blank" 
                                        rel="noopener noreferrer" 
                                        className={styles['social-link']}
                                        aria-label="GitHub"
                                    >
                                        <i className="fab fa-github fa-lg"></i>
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}