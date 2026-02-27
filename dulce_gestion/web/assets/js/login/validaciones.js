/**
 * Valida los campos del formulario de login.
 * Devuelve null si todo está correcto.
 * Devuelve un string con mensaje de error si hay problema.
 *
 * @param {string} correoOUsuario
 * @param {string} contrasena
 * @returns {string|null}
 */
export function validarFormularioLogin(correoOUsuario, contrasena) {

    const usuario = correoOUsuario?.trim() ?? "";
    const clave = contrasena?.trim() ?? "";

    if (usuario.length === 0) {
        return "Debe ingresar su usuario o correo.";
    }

    if (clave.length === 0) {
        return "Debe ingresar su contraseña.";
    }

    if (clave.length < 6) {
        return "La contraseña debe tener al menos 6 caracteres.";
    }

    return null; // No hay errores
}