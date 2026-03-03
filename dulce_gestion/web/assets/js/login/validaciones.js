export function validarFormularioLogin(correo, contrasena) {

  const usuario = correo?.trim() ?? "";
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

  return null;
}