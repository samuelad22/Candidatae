import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { validateLogin } from '../utils/formValidations'
import ThemeToggle from '../components/layout/ThemeToggler'

export default function Login() {
    const navigate = useNavigate()

    const [formData, setFormData] = useState({
        email: '',
        password: '',
    })

  // Estado para guardar los mensajes de error
    const [errors, setErrors] = useState({})

    const handleChange = (e) => {
        const { name, value } = e.target
        setFormData((prev) => ({
        ...prev,
        [name]: value,
        }))
        // Limpiar el error del campo a medida que el usuario escribe
        if (errors[name]) {
        setErrors((prev) => ({ ...prev, [name]: '' }))
        }
    }

    const handleSubmit = (e) => {
        e.preventDefault()

        // Ejecutamos la validación importada de utils
        const validationErrors = validateLogin(formData)

        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors)
            return
        }

        console.log('Datos válidos enviados:', formData)
    }

    return (
        <div className="container bg-main">
            <ThemeToggle />
            <div className="card shadow-lg rounded-3 mx-auto w-50 p-4" >
                
                <div className="text-start">
                    <h2 className="fw-bold">Iniciar sesión</h2>
                    <p className="text-muted">Accede a tu cuenta para continuar</p>
                </div>

                <form onSubmit={handleSubmit} noValidate>
                
                    <div className="my-4">
                        <label htmlFor="email" className="form-label fw-medium">Correo Electrónico</label>
                        <div className="input-group">
                            {/* <span className="input-group-text bg-light">
                                <i className="fa-solid fa-envelope text-secondary"></i>
                            </span> */}
                            <input
                                type="email"
                                className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                                id="email" name="email" placeholder="ejemplo@correo.com" value={formData.email}
                                onChange={handleChange}
                            />
                        </div>
                        {errors.email && <div className="invalid-feedback d-block">{errors.email}</div>}
                    </div>


                    <div className="my-4">
                        <label htmlFor="password" className="form-label fw-medium">Contraseña</label>
                        <div className="input-group">
                            {/* <span className="input-group-text bg-light">
                                <i className="fa-solid fa-lock text-secondary"></i>
                            </span> */}
                            <input
                                type="password" className={`form-control ${errors.password ? 'is-invalid' : ''}`} id="password" name="password" placeholder="••••••••"
                                value={formData.password} onChange={handleChange}
                            />
                        </div>
                        {errors.password && <div className="invalid-feedback d-block">{errors.password}</div>}
                    </div>

                    <div className="d-flex justify-content-between align-items-center mb-4">
                        <div className="form-check">
                            <input type="checkbox" className="form-check-input" id="rememberMe" />
                            <label className="form-check-label text-secondary fw-medium small" htmlFor="rememberMe">
                                Recuérdame
                            </label>
                        </div>

                        <div>
                            <a href="/forgot-password" className="text-decoration-none text-primary fw-medium small">
                                ¿Olvidaste tu contraseña?
                            </a>
                        </div>
                    </div>

                    <button type="submit" className="btn btn-primary my-1 w-100">
                        <i className="fa-solid fa-right-to-bracket"></i> Iniciar sesión
                    </button>
                </form>

                <div className="text-center mt-3 pt-3 border-top">
                    <p className="text-muted">
                        ¿No tienes una cuenta?{' '}
                        <Link to="/registro" className="text-primary text-decoration-none fw-semibold">
                        Regístrate aquí
                        </Link>
                    </p>
                </div>

            </div>
        </div>
    )
}