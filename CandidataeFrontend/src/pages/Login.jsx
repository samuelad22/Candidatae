import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { validateLogin } from '../utils/formValidations'
import ThemeToggle from '../components/layout/ThemeToggler'
import { loginUser } from '../services/authService'

export default function Login() {
    const navigate = useNavigate()

    const [formData, setFormData] = useState({
        email: '',
        password: '',
    })

    const [errors, setErrors] = useState({})
    const [serverError, setServerError] = useState('')
    const [isLoading, setIsLoading] = useState(false)

    const handleChange = (e) => {
        const { name, value } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }))
        if (errors[name]) {
            setErrors((prev) => ({ ...prev, [name]: '' }))
        }
        if (serverError) setServerError('')
    }

    const handleSubmit = async (e) => {
        e.preventDefault()

        const validationErrors = validateLogin(formData)
        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors)
            return
        }

        setIsLoading(true)
        setServerError('')

        try {
            await loginUser(formData)
            navigate('/')
        } catch (err) {
            setServerError(err.message || 'Error al iniciar sesión. Comprueba tus credenciales.')
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <div className="container bg-main">
            <ThemeToggle />
            <div className="card shadow-lg rounded-3 mx-auto login-form p-4">
                
                <div className="text-start">
                    <h2 className="fw-bold">Iniciar sesión</h2>
                    <p className="text-muted">Accede a tu cuenta para continuar</p>
                </div>

                {serverError && (
                    <div className="alert alert-danger py-2 small" role="alert">
                        {serverError}
                    </div>
                )}

                <form onSubmit={handleSubmit} noValidate>
                
                    <div className="my-4">
                        <label htmlFor="email" className="form-label fw-medium">Correo Electrónico</label>
                        <div className="input-group">
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

                    <button type="submit" className="btn btn-primary my-1 w-100" disabled={isLoading}>
                        {isLoading ? (
                            <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        ) : (
                            <i className="fa-solid fa-right-to-bracket me-2"></i>
                        )}
                        Iniciar sesión
                    </button>
                </form>

                <div className="text-center mt-3 pt-3 border-top">
                    <p className="text-muted">
                        ¿No tienes una cuenta?{' '}
                        <Link to="/register" className="text-primary text-decoration-none fw-semibold">
                            Regístrate aquí
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    )
}