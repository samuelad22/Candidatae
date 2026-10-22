export const validateEmail = (email) => {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}

export const validateLogin = ({ email, password }) => {
    const errors = {};

    if (!email) {
        errors.email = 'El correo electrónico es obligatorio.';
    } else if (!validateEmail(email)) {
        errors.email = 'El formato del correo electrónico no es válido.';
    }

    if (!password) {
        errors.password = 'La contraseña es obligatoria.';
    } else if (password.length < 6) {
        errors.password = 'La contraseña debe tener al menos 6 caracteres.';
    }

    return errors;
}

export const validateRegister = ({ name, email, password, confirmPassword }) => {
    const errors = {};

    if (!name || name.trim() === '') {
        errors.name = 'El nombre completo es obligatorio.';
    }

    const loginErrors = validateLogin({ email, password });
    Object.assign(errors, loginErrors);

    if (!confirmPassword) {
        errors.confirmPassword = 'Por favor, confirma tu contraseña.';
    } else if (password !== confirmPassword) {
        errors.confirmPassword = 'Las contraseñas no coinciden.';
    }

    return errors;
}