import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function Register() {
    const navigate = useNavigate()

    const [formData, setFormData] = useState({
        name: '',
        email: '',
        password: '',
        confirmPassword: '',
    })

    const handleChange = (e) => {
        const { name, value } = e.target
        setFormData((prev) => ({
        ...prev,
        [name]: value,
        }))
    }

    const handleSubmit = (e) => {
        e.preventDefault()

        if (formData.password !== formData.confirmPassword) {

            return
        }

        console.log('Registro enviado:', formData)

        // Redirigir al login tras un registro exitoso
        // navigate('/login')
    }

    return (
        <div className="container d-flex justify-content-center align-items-center vh-100">
            <div className="card shadow-lg p-4 rounded-3" style={{ maxWidth: '420px', width: '100%' }}>
                
                <div className="text-center mb-4">
                    <i className="fa-solid fa-user-plus fa-3x text-primary mb-2"></i>
                    <h2 className="fw-bold">Crear Cuenta</h2>
                    <p className="text-muted">Empieza a llevar el control de tus postulaciones</p>
                </div>

                <form onSubmit={handleSubmit}>
                
                    <div className="mb-3">
                        <label htmlFor="name" className="form-label fw-semibold">
                        Nombre Completo
                        </label>
                        <div className="input-group">
                        <span className="input-group-text bg-light">
                            <i className="fa-solid fa-user text-secondary"></i>
                        </span>
                        <input
                            type="text"
                            className="form-control"
                            id="name"
                            name="name"
                            placeholder="Tu nombre"
                            value={formData.name}
                            onChange={handleChange}
                            required
                        />
                        </div>
                    </div>

                    {/* Email */}
                    <div className="mb-3">
                        <label htmlFor="email" className="form-label fw-semibold">
                        Correo Electrónico
                        </label>
                        <div className="input-group">
                        <span className="input-group-text bg-light">
                            <i className="fa-solid fa-envelope text-secondary"></i>
                        </span>
                        <input
                            type="email"
                            className="form-control"
                            id="email"
                            name="email"
                            placeholder="ejemplo@correo.com"
                            value={formData.email}
                            onChange={handleChange}
                            required
                        />
                        </div>
                    </div>

                    {/* Contraseña */}
                    <div className="mb-3">
                        <label htmlFor="password" className="form-label fw-semibold">
                        Contraseña
                        </label>
                        <div className="input-group">
                        <span className="input-group-text bg-light">
                            <i className="fa-solid fa-lock text-secondary"></i>
                        </span>
                        <input
                            type="password"
                            className="form-control"
                            id="password"
                            name="password"
                            placeholder="••••••••"
                            value={formData.password}
                            onChange={handleChange}
                            required
                        />
                        </div>
                    </div>

                    {/* Confirmar Contraseña */}
                    <div className="mb-3">
                        <label htmlFor="confirmPassword" className="form-label fw-semibold">
                        Confirmar Contraseña
                        </label>
                        <div className="input-group">
                        <span className="input-group-text bg-light">
                            <i className="fa-solid fa-shield-halved text-secondary"></i>
                        </span>
                        <input
                            type="password"
                            className="form-control"
                            id="confirmPassword"
                            name="confirmPassword"
                            placeholder="••••••••"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            required
                        />
                        </div>
                    </div>

                    <button type="submit" className="btn btn-primary w-100 py-2 mt-3 fw-bold">
                        <i className="fa-solid fa-user-check me-2"></i> Registrarse
                    </button>
                    </form>

                <div className="text-center mt-4 pt-3 border-top">
                <p className="mb-0 text-muted">
                    ¿Ya tienes una cuenta?{' '}
                    <Link to="/login" className="text-primary text-decoration-none fw-semibold">
                    Inicia sesión aquí
                    </Link>
                </p>
                </div>

            </div>
        </div>
    )
}