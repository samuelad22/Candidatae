export const validateEmail = (email) => {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}

export const validateLogin = ({ email, password }) => {
    const errors = {};

    if (!email) {
        errors.email = 'El correo electrónico es obligatorio.'
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