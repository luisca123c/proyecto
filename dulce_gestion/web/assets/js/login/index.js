// ==============================
// IMPORTACIONES
// ==============================

import { mostrarError, limpiarError, establecerEstadoBoton } from "./dom.js";
import { validarFormularioLogin } from "./validaciones.js";


// ==============================
// CONSTANTES Y VARIABLES
// ==============================

const formularioLogin = document.getElementById("formularioLogin");
const campoCorreo = document.getElementById("correo");
const campoContrasena = document.getElementById("contrasena");
const contenedorError = document.getElementById("contenedorErrorLogin");
const botonLogin = document.getElementById("btnLogin");
const datosLogin = document.getElementById("datosLogin");


// ==============================
// FUNCIONES Y MÉTODOS
// ==============================

/**
 * Muestra error enviado desde el servidor si existe.
 */
function mostrarErrorServidor() {

  const mensajeErrorServidor =
    datosLogin?.dataset?.mensajeError?.trim();

  if (mensajeErrorServidor) {
    mostrarError(contenedorError, mensajeErrorServidor);
  }
}


/**
 * Maneja la validación del formulario antes de enviarlo.
 */
function manejarEnvioFormulario(evento) {

  limpiarError(contenedorError);

  const mensajeError = validarFormularioLogin(
    campoCorreo.value,
    campoContrasena.value
  );

  if (mensajeError) {
    evento.preventDefault();
    mostrarError(contenedorError, mensajeError);
    return;
  }

  establecerEstadoBoton(botonLogin, true);
}


/**
 * Limpia errores cuando el usuario escribe.
 */
function configurarEventosInput() {

  [campoCorreo, campoContrasena].forEach((campo) => {
    campo.addEventListener("input", () => {
      limpiarError(contenedorError);
    });
  });
}


// ==============================
// EVENTOS
// ==============================

document.addEventListener("DOMContentLoaded", () => {

  if (!formularioLogin) return;

  mostrarErrorServidor();

  formularioLogin.addEventListener("submit", manejarEnvioFormulario);

  configurarEventosInput();
});