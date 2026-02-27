/**
 * Muestra un mensaje de error en el contenedor indicado.
 * @param {HTMLElement} contenedorError - Elemento donde se mostrará el mensaje.
 * @param {string} mensaje - Texto del mensaje de error.
 */
export function mostrarError(contenedorError, mensaje) {
    contenedorError.textContent = mensaje;
    contenedorError.hidden = false;
}

/**
 * Limpia y oculta el contenedor de error.
 * @param {HTMLElement} contenedorError
 */
export function limpiarError(contenedorError) {
    contenedorError.textContent = "";
    contenedorError.hidden = true;
}

/**
 * Cambia el estado visual del botón (cargando / normal).
 * @param {HTMLButtonElement} boton
 * @param {boolean} estaCargando
 */
export function establecerEstadoBoton(boton, estaCargando) {
    if (!boton) return;

    // Guardamos el texto original la primera vez
    if (!boton.dataset.textoOriginal) {
        boton.dataset.textoOriginal = boton.textContent;
    }

    boton.disabled = estaCargando;
    boton.textContent = estaCargando
        ? "Ingresando..."
        : boton.dataset.textoOriginal;
}