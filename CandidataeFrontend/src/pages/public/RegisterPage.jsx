import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { validateRegister } from '../../utils/formValidations'
import { registerUser } from '../../services/authService'

export default function RegisterPage() {
    const navigate = useNavigate()

    const [formData, setFormData] = useState({
        name: '',
        email: '',
        password: '',
        confirmPassword: '',
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

        const validationErrors = validateRegister(formData)
        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors)
            return
        }

        setIsLoading(true)
        setServerError('')

        try {
            const { confirmPassword, ...registrationData } = formData
            await registerUser(registrationData)
            navigate('/')
        } catch (err) {
            setServerError(err.message || 'Error al registrar la cuenta. Es posible que el email ya esté en uso.')
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <div className="bg-main my-4">
            <div className="card shadow-lg rounded-3 mx-auto login-form p-4">
                
                <div className="text-start">
                    <h2 className="fw-bold">Crear cuenta</h2>
                    <p className="text-muted">Empieza a centralizar tus solicitudes de empleo</p>
                </div>

                {serverError && (
                    <div className="alert alert-danger py-2 small" role="alert">
                        {serverError}
                    </div>
                )}

                <form onSubmit={handleSubmit} noValidate>
                
                    <div className="my-3">
                        <label htmlFor="name" className="form-label fw-medium">Nombre completo</label>
                        <div className="input-group">
                            <input
                                type="text"
                                className={`form-control ${errors.name ? 'is-invalid' : ''}`}
                                id="name" name="name" placeholder="Tu nombre" value={formData.name}
                                onChange={handleChange}
                            />
                        </div>
                        {errors.name && <div className="invalid-feedback d-block">{errors.name}</div>}
                    </div>

                    <div className="my-3">
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

                    <div className="my-3">
                        <label htmlFor="password" className="form-label fw-medium">Contraseña</label>
                        <div className="input-group">
                            <input
                                type="password" className={`form-control ${errors.password ? 'is-invalid' : ''}`} id="password" name="password" placeholder="••••••••"
                                value={formData.password} onChange={handleChange}
                            />
                        </div>
                        {errors.password && <div className="invalid-feedback d-block">{errors.password}</div>}
                    </div>

                    <div className="my-3">
                        <label htmlFor="confirmPassword" className="form-label fw-medium">Confirmar contraseña</label>
                        <div className="input-group">
                            <input
                                type="password" className={`form-control ${errors.confirmPassword ? 'is-invalid' : ''}`} id="confirmPassword" name="confirmPassword" placeholder="••••••••"
                                value={formData.confirmPassword} onChange={handleChange}
                            />
                        </div>
                        {errors.confirmPassword && <div className="invalid-feedback d-block">{errors.confirmPassword}</div>}
                    </div>

                    <button type="submit" className="btn btn-primary my-3 w-100" disabled={isLoading}>
                        {isLoading ? (
                            <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                        ) : (
                            <i className="fa-solid fa-user-plus me-2"></i>
                        )}
                        Registrarse
                    </button>
                </form>

                <div className="text-center mt-3 pt-3 border-top">
                    <p className="text-muted">
                        ¿Ya tienes una cuenta?{' '}
                        <Link to="/login" className="text-primary text-decoration-none fw-semibold">
                            Inicia sesión
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    )
}