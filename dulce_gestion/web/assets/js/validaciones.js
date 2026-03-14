/**
 * validaciones.js
 * Validaciones en tiempo real para campos de correo, teléfono y nombre.
 * Se aplica automáticamente a cualquier form de la página que contenga
 * inputs con name="correo", name="telefono" o name="nombreCompleto".
 */

document.addEventListener('DOMContentLoaded', function () {

  /* ── Utilidad: mostrar / ocultar mensaje de error bajo un input ── */
  function mostrarError(input, mensaje) {
    let span = input.parentElement.querySelector('.val-error');
    if (!span) {
      span = document.createElement('span');
      span.className = 'val-error';
      span.style.cssText = 'display:block;color:#e53935;font-size:12px;margin-top:4px;';
      input.parentElement.appendChild(span);
    }
    span.textContent = mensaje;
    input.style.borderColor = '#e53935';
  }

  function limpiarError(input) {
    const span = input.parentElement.querySelector('.val-error');
    if (span) span.textContent = '';
    input.style.borderColor = '';
  }

  /* ══════════════════════════════════════════════════════════════
     NOMBRE COMPLETO — no permite dígitos
  ══════════════════════════════════════════════════════════════ */
  document.querySelectorAll('input[name="nombreCompleto"]').forEach(function (input) {
    if (input.type === 'hidden') return;

    input.addEventListener('input', function () {
      const valorOriginal = this.value;
      // Eliminar cualquier dígito del valor en tiempo real
      const valorLimpio = valorOriginal.replace(/[0-9]/g, '');

      if (valorOriginal !== valorLimpio) {
        this.value = valorLimpio;
        mostrarError(this, 'El nombre no puede contener números.');
      } else {
        limpiarError(this);
      }
    });

    input.addEventListener('blur', function () {
      if (this.value.trim() === '') {
        limpiarError(this);
        return;
      }
      if (/[0-9]/.test(this.value)) {
        mostrarError(this, 'El nombre no puede contener números.');
      } else {
        limpiarError(this);
      }
    });
  });

  /* ══════════════════════════════════════════════════════════════
     TELÉFONO — solo dígitos, máximo 10 caracteres
  ══════════════════════════════════════════════════════════════ */
  document.querySelectorAll('input[name="telefono"]').forEach(function (input) {

    // Bloquear teclas no numéricas en tiempo real (permite Backspace, Delete, Tab, flechas)
    input.addEventListener('keydown', function (e) {
      const permitidas = ['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight', 'Home', 'End'];
      if (permitidas.includes(e.key)) return;
      // Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X
      if (e.ctrlKey || e.metaKey) return;
      // Bloquear cualquier carácter que no sea dígito
      if (!/^\d$/.test(e.key)) {
        e.preventDefault();
        mostrarError(this, 'El teléfono solo acepta números.');
      }
    });

    // Limpiar lo que no sea dígito si se pega texto
    input.addEventListener('input', function () {
      const valorOriginal = this.value;
      const valorLimpio   = valorOriginal.replace(/\D/g, '').slice(0, 10);

      if (valorOriginal !== valorLimpio) {
        this.value = valorLimpio;
      }

      if (this.value.length > 0 && this.value.length < 7) {
        mostrarError(this, 'El teléfono debe tener al menos 7 dígitos.');
      } else {
        limpiarError(this);
      }
    });

    input.addEventListener('blur', function () {
      if (this.value.trim() === '') { limpiarError(this); return; }
      if (this.value.length < 7) {
        mostrarError(this, 'El teléfono debe tener al menos 7 dígitos.');
      } else {
        limpiarError(this);
      }
    });
  });

  /* ══════════════════════════════════════════════════════════════
     CORREO — formato válido de email
  ══════════════════════════════════════════════════════════════ */
  const regexCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

  document.querySelectorAll('input[name="correo"]').forEach(function (input) {

    input.addEventListener('blur', function () {
      if (this.value.trim() === '') { limpiarError(this); return; }
      if (!regexCorreo.test(this.value.trim())) {
        mostrarError(this, 'Escribe un correo válido (ej: usuario@dominio.com).');
      } else {
        limpiarError(this);
      }
    });

    input.addEventListener('input', function () {
      // Limpiar el error mientras el usuario corrige
      if (regexCorreo.test(this.value.trim())) {
        limpiarError(this);
      }
    });
  });

  /* ══════════════════════════════════════════════════════════════
     BLOQUEO EN SUBMIT — impedir envío si hay errores activos
  ══════════════════════════════════════════════════════════════ */
  document.querySelectorAll('form').forEach(function (form) {
    form.addEventListener('submit', function (e) {

      let hayErrores = false;

      // Validar nombre
      form.querySelectorAll('input[name="nombreCompleto"]').forEach(function (input) {
        if (input.type === 'hidden') return;
        if (/[0-9]/.test(input.value)) {
          mostrarError(input, 'El nombre no puede contener números.');
          hayErrores = true;
        }
      });

      // Validar teléfono
      form.querySelectorAll('input[name="telefono"]').forEach(function (input) {
        const val = input.value.replace(/\D/g, '');
        if (val.length > 0 && val.length < 7) {
          mostrarError(input, 'El teléfono debe tener al menos 7 dígitos.');
          hayErrores = true;
        }
      });

      // Validar correo
      form.querySelectorAll('input[name="correo"]').forEach(function (input) {
        if (input.value.trim() !== '' && !regexCorreo.test(input.value.trim())) {
          mostrarError(input, 'Escribe un correo válido (ej: usuario@dominio.com).');
          hayErrores = true;
        }
      });

      if (hayErrores) {
        e.preventDefault();
        // Hacer scroll al primer error visible
        const primerError = form.querySelector('.val-error:not(:empty)');
        if (primerError) {
          primerError.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }
    });
  });

});
