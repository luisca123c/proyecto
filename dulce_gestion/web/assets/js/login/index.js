import { mostrarError, limpiarError, establecerEstadoBoton } from "./dom.js";
import { validarFormularioLogin } from "./validaciones.js";

// ── Referencias al DOM ────────────────────────────────────────
const formularioLogin = document.getElementById("formularioLogin");
const campoCorreo     = document.getElementById("correo");
const campoContrasena = document.getElementById("contrasena");
const contenedorError = document.getElementById("contenedorErrorLogin");
const botonLogin      = document.getElementById("btnLogin");

// ── Funciones ─────────────────────────────────────────────────

/**
 * Valida en cliente antes de enviar al servidor.
 * Si hay error lo muestra y cancela el submit.
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

    // Pasa validación → mostrar estado cargando
    establecerEstadoBoton(botonLogin, true);
}

/**
 * Limpia el error cuando el usuario empieza a corregir los campos.
 */
function configurarLimpiezaErrores() {
    [campoCorreo, campoContrasena].forEach((campo) => {
        campo.addEventListener("input", () => limpiarError(contenedorError));
    });
}

// ── Eventos ───────────────────────────────────────────────────

document.addEventListener("DOMContentLoaded", () => {
    if (!formularioLogin) return;

    formularioLogin.addEventListener("submit", manejarEnvioFormulario);
    configurarLimpiezaErrores();
});
