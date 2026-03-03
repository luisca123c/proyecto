export function mostrarError(contenedor, mensaje) {
  contenedor.textContent = mensaje;
  contenedor.hidden = false;
}

export function limpiarError(contenedor) {
  contenedor.textContent = "";
  contenedor.hidden = true;
}

export function establecerEstadoBoton(boton, estaCargando) {

  if (!boton) return;

  if (!boton.dataset.textoOriginal) {
    boton.dataset.textoOriginal = boton.textContent;
  }

  boton.disabled = estaCargando;
  boton.textContent = estaCargando
    ? "Ingresando..."
    : boton.dataset.textoOriginal;
}