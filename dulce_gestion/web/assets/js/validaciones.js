/**
 * validaciones.js
 * Validaciones en tiempo real para todos los formularios del sistema.
 */

/* ── Helpers ─────────────────────────────────────────────────────────── */
function valMostrarError(input, mensaje) {
  const contenedor = input.parentElement;
  let span = contenedor.querySelector('.val-error');
  if (!span) {
    span = document.createElement('span');
    span.className = 'val-error';
    span.setAttribute('aria-live', 'polite');
    span.style.cssText = 'display:block;color:#e53935;font-size:12px;margin-top:5px;font-weight:500;';
    contenedor.appendChild(span);
  }
  span.textContent = mensaje;
  input.style.borderColor = '#e53935';
  input.style.boxShadow   = '0 0 0 2px rgba(229,57,53,.20)';
}

function valLimpiarError(input) {
  const span = input.parentElement.querySelector('.val-error');
  if (span) span.textContent = '';
  input.style.borderColor = '';
  input.style.boxShadow   = '';
}

function togglePass(btn) {
  const input = btn.previousElementSibling ||
    btn.parentElement.querySelector('input[type="password"],input[type="text"]');
  if (!input) return;
  const mostrar = input.type === 'password';
  input.type = mostrar ? 'text' : 'password';
  const icon = btn.querySelector('i');
  if (icon) icon.className = mostrar ? 'fi fi-sr-eye-crossed' : 'fi fi-sr-eye';
}

/* ── Constantes ─────────────────────────────────────────────────────── */
const LIMITE = {
  nombre:    100,  correo:    100,  telefono:  10,
  telMin:      7,  producto:   50,  prodDesc: 100,
  gastoDesc: 150,  pasMin:      6,
};
const REGEX_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/* ── Inicialización ─────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', function () {

  /* NOMBRE COMPLETO */
  document.querySelectorAll('input[name="nombreCompleto"]').forEach(function (input) {
    if (input.type === 'hidden') return;
    input.setAttribute('maxlength', LIMITE.nombre);
    input.addEventListener('input', function () {
      const tieneNum = /[0-9]/.test(this.value);
      if (tieneNum) {
        this.value = this.value.replace(/[0-9]/g, '');
        valMostrarError(this, 'El nombre no puede contener números.');
        return;
      }
      this.value.trim().length > LIMITE.nombre
        ? valMostrarError(this, 'Máximo ' + LIMITE.nombre + ' caracteres.')
        : valLimpiarError(this);
    });
    input.addEventListener('blur', function () {
      if (this.value.trim() === '') { valLimpiarError(this); return; }
      /[0-9]/.test(this.value)
        ? valMostrarError(this, 'El nombre no puede contener números.')
        : valLimpiarError(this);
    });
  });

  /* NOMBRE DE PRODUCTO */
  document.querySelectorAll('input[name="nombre"]').forEach(function (input) {
    if (input.type === 'hidden') return;
    input.setAttribute('maxlength', LIMITE.producto);
    input.addEventListener('input', function () {
      const tieneNum = /[0-9]/.test(this.value);
      if (tieneNum) {
        this.value = this.value.replace(/[0-9]/g, '');
        valMostrarError(this, 'El nombre del producto no puede contener números.');
        return;
      }
      this.value.trim().length > LIMITE.producto
        ? valMostrarError(this, 'Máximo ' + LIMITE.producto + ' caracteres (tienes ' + this.value.trim().length + ').')
        : valLimpiarError(this);
    });
    input.addEventListener('blur', function () {
      if (this.value.trim() === '') { valLimpiarError(this); return; }
      /[0-9]/.test(this.value)
        ? valMostrarError(this, 'El nombre del producto no puede contener números.')
        : valLimpiarError(this);
    });
  });

  /* TELÉFONO */
  document.querySelectorAll('input[name="telefono"]').forEach(function (input) {
    input.setAttribute('maxlength', LIMITE.telefono);
    input.addEventListener('keydown', function (e) {
      const ok = ['Backspace','Delete','Tab','ArrowLeft','ArrowRight','Home','End'];
      if (ok.includes(e.key) || e.ctrlKey || e.metaKey) return;
      if (!/^\d$/.test(e.key)) { e.preventDefault(); valMostrarError(this, 'Solo se permiten dígitos (0–9).'); }
    });
    input.addEventListener('input', function () {
      const limpio = this.value.replace(/\D/g, '').slice(0, LIMITE.telefono);
      if (this.value !== limpio) this.value = limpio;
      const len = this.value.length;
      if (len > 0 && len < LIMITE.telMin)
        valMostrarError(this, 'Mínimo ' + LIMITE.telMin + ' dígitos (llevas ' + len + ').');
      else valLimpiarError(this);
    });
    input.addEventListener('blur', function () {
      if (!this.value.trim()) { valLimpiarError(this); return; }
      this.value.length < LIMITE.telMin
        ? valMostrarError(this, 'Mínimo ' + LIMITE.telMin + ' dígitos.')
        : valLimpiarError(this);
    });
  });

  /* CORREO */
  document.querySelectorAll('input[name="correo"]').forEach(function (input) {
    input.setAttribute('maxlength', LIMITE.correo);
    input.addEventListener('input', function () {
      if (this.value.length > LIMITE.correo)
        valMostrarError(this, 'Máximo ' + LIMITE.correo + ' caracteres.');
      else if (REGEX_CORREO.test(this.value.trim())) valLimpiarError(this);
    });
    input.addEventListener('blur', function () {
      if (!this.value.trim()) { valLimpiarError(this); return; }
      if (!REGEX_CORREO.test(this.value.trim()))
        valMostrarError(this, 'Escribe un correo válido (ej: usuario@dominio.com).');
      else valLimpiarError(this);
    });
  });

  /* DESCRIPCIÓN PRODUCTO (input, opcional) */
  document.querySelectorAll('input[name="descripcion"]').forEach(function (input) {
    input.setAttribute('maxlength', LIMITE.prodDesc);
    input.addEventListener('input', function () {
      const restantes = LIMITE.prodDesc - this.value.length;
      if (restantes < 0)
        valMostrarError(this, 'Máximo ' + LIMITE.prodDesc + ' caracteres.');
      else if (restantes <= 15) {
        valMostrarError(this, 'Te quedan ' + restantes + ' caracteres.');
        input.style.borderColor = '#ff9800';
        input.style.boxShadow   = '0 0 0 2px rgba(255,152,0,.20)';
      } else valLimpiarError(this);
    });
  });

  /* DESCRIPCIÓN GASTOS/COMPRAS (textarea, obligatoria) */
  document.querySelectorAll('textarea[name="descripcion"]').forEach(function (input) {
    input.setAttribute('maxlength', LIMITE.gastoDesc);
    input.addEventListener('input', function () {
      if (this.value.trim()) valLimpiarError(this);
      if (this.value.length > LIMITE.gastoDesc)
        valMostrarError(this, 'Máximo ' + LIMITE.gastoDesc + ' caracteres.');
    });
    input.addEventListener('blur', function () {
      if (!this.value.trim()) valMostrarError(this, 'La descripción es obligatoria.');
      else if (this.value.length > LIMITE.gastoDesc) valMostrarError(this, 'Máximo ' + LIMITE.gastoDesc + ' caracteres.');
      else valLimpiarError(this);
    });
  });

  /* STOCK */
  document.querySelectorAll('input[name="stock"]').forEach(function (input) {
    input.addEventListener('input', function () {
      if (this.value === '') { valLimpiarError(this); return; }
      if (this.value.includes('.') || this.value.includes(','))
        { valMostrarError(this, 'El stock debe ser un número entero (sin decimales).'); return; }
      const val = parseInt(this.value, 10);
      if (isNaN(val)) valMostrarError(this, 'El stock debe ser un número válido.');
      else if (val < 0) valMostrarError(this, 'El stock no puede ser negativo.');
      else valLimpiarError(this);
    });
    input.addEventListener('blur', function () {
      if (this.value === '') { valLimpiarError(this); return; }
      const val = parseInt(this.value, 10);
      (isNaN(val) || val < 0)
        ? valMostrarError(this, 'El stock debe ser un número entero mayor o igual a 0.')
        : valLimpiarError(this);
    });
  });

  /* PRECIO */
  document.querySelectorAll('input[name="precio"]').forEach(function (input) {
    input.addEventListener('input', function () {
      if (this.value === '') { valLimpiarError(this); return; }
      const val = parseFloat(this.value);
      if (isNaN(val)) valMostrarError(this, 'El precio debe ser un número válido (ej: 1500 o 1500.50).');
      else if (val < 0) valMostrarError(this, 'El precio no puede ser negativo.');
      else if (val === 0) valMostrarError(this, 'El precio debe ser mayor a cero.');
      else valLimpiarError(this);
    });
    input.addEventListener('blur', function () {
      if (this.value === '') { valLimpiarError(this); return; }
      const val = parseFloat(this.value);
      (isNaN(val) || val <= 0)
        ? valMostrarError(this, 'El precio debe ser un número mayor a cero.')
        : valLimpiarError(this);
    });
  });

  /* FECHA DE VENCIMIENTO */
  document.querySelectorAll('input[name="fechaVencimiento"]').forEach(function (input) {
    input.setAttribute('min', new Date().toISOString().split('T')[0]);
    function validar() {
      if (!this.value) { valLimpiarError(this); return; }
      const hoy = new Date(); hoy.setHours(0,0,0,0);
      new Date(this.value + 'T00:00:00') < hoy
        ? valMostrarError(this, 'La fecha de vencimiento no puede ser anterior a hoy.')
        : valLimpiarError(this);
    }
    input.addEventListener('change', validar);
    input.addEventListener('blur',   validar);
  });

  /* FECHA GASTO/COMPRA */
  document.querySelectorAll('input[name="fecha"]').forEach(function (input) {
    input.setAttribute('max', new Date().toISOString().split('T')[0]);
    function validar() {
      if (!this.value) { valLimpiarError(this); return; }
      const hoy = new Date(); hoy.setHours(0,0,0,0);
      new Date(this.value + 'T00:00:00') > hoy
        ? valMostrarError(this, 'La fecha no puede ser futura.')
        : valLimpiarError(this);
    }
    input.addEventListener('change', validar);
    input.addEventListener('blur',   validar);
    input.addEventListener('input',  validar);
  });

  /* CONTRASEÑA NUEVA + CONFIRMAR (perfil) */
  document.querySelectorAll('input[name="contrasennaNueva"]').forEach(function (input) {
    input.addEventListener('input', function () {
      if (!this.value) { valLimpiarError(this); return; }
      if (this.value.length < LIMITE.pasMin) {
        valMostrarError(this, 'Mínimo ' + LIMITE.pasMin + ' caracteres (llevas ' + this.value.length + ').');
      } else {
        valLimpiarError(this);
        const confirmar = document.querySelector('input[name="contrasennaNuevaConfirm"]');
        if (confirmar && confirmar.value)
          confirmar.value !== this.value
            ? valMostrarError(confirmar, 'Las contraseñas no coinciden.')
            : valLimpiarError(confirmar);
      }
    });
  });

  document.querySelectorAll('input[name="contrasennaNuevaConfirm"]').forEach(function (input) {
    function validar() {
      if (!this.value) { valLimpiarError(this); return; }
      const nueva = document.querySelector('input[name="contrasennaNueva"]');
      nueva && this.value !== nueva.value
        ? valMostrarError(this, 'Las contraseñas no coinciden.')
        : valLimpiarError(this);
    }
    input.addEventListener('input', validar);
    input.addEventListener('blur',  validar);
  });

  /* CONTRASEÑA (crear/editar empleado) */
  document.querySelectorAll('input[name="contrasena"][type="password"]').forEach(function (input) {
    input.addEventListener('input', function () {
      if (!this.value.trim()) { valLimpiarError(this); return; }
      this.value.trim().length < LIMITE.pasMin
        ? valMostrarError(this, 'Mínimo ' + LIMITE.pasMin + ' caracteres (llevas ' + this.value.trim().length + ').')
        : valLimpiarError(this);
    });
    input.addEventListener('blur', function () {
      if (!this.value.trim()) { valLimpiarError(this); return; }
      this.value.trim().length < LIMITE.pasMin
        ? valMostrarError(this, 'Mínimo ' + LIMITE.pasMin + ' caracteres.')
        : valLimpiarError(this);
    });
  });

  /* ── BLOQUEO EN SUBMIT ─────────────────────────────────────────── */
  document.querySelectorAll('form').forEach(function (form) {
    if (form.dataset.valInit) return;
    form.dataset.valInit = '1';

    form.addEventListener('submit', function (e) {
      let hayErrores = false;

      const check = (selector, fn) => form.querySelectorAll(selector).forEach(function (inp) {
        if (inp.type === 'hidden') return;
        const msg = fn(inp);
        if (msg) { valMostrarError(inp, msg); hayErrores = true; }
      });

      check('input[name="nombreCompleto"]', i => {
        if (!i.value.trim()) return 'El nombre completo es obligatorio.';
        if (/[0-9]/.test(i.value)) return 'El nombre no puede contener números.';
        return null;
      });
      check('input[name="nombre"]', i => {
        if (!i.value.trim()) return 'El nombre es obligatorio.';
        if (/[0-9]/.test(i.value)) return 'El nombre del producto no puede contener números.';
        return null;
      });
      check('input[name="telefono"]', i => {
        if (!i.value.trim()) return 'El teléfono es obligatorio.';
        const d = i.value.replace(/\D/g,'');
        if (d.length < LIMITE.telMin) return 'Mínimo ' + LIMITE.telMin + ' dígitos.';
        return null;
      });
      check('input[name="correo"]', i => {
        if (!i.value.trim()) return 'El correo es obligatorio.';
        if (!REGEX_CORREO.test(i.value.trim())) return 'Correo inválido (ej: usuario@dominio.com).';
        return null;
      });
      check('input[name="stock"]', i => {
        if (i.value === '') return 'El stock es obligatorio.';
        const v = parseInt(i.value,10);
        if (isNaN(v)) return 'Stock debe ser un número entero válido.';
        if (v < 0)    return 'El stock no puede ser negativo.';
        return null;
      });
      check('input[name="precio"]', i => {
        if (i.value === '') return 'El precio es obligatorio.';
        const v = parseFloat(i.value);
        if (isNaN(v) || v <= 0) return 'El precio debe ser mayor a cero.';
        return null;
      });
      check('input[name="fechaVencimiento"]', i => {
        if (!i.value) return 'La fecha de vencimiento es obligatoria.';
        const hoy = new Date(); hoy.setHours(0,0,0,0);
        if (new Date(i.value+'T00:00:00') < hoy) return 'No puede ser anterior a hoy.';
        return null;
      });
      check('input[name="fecha"]', i => {
        if (!i.value) return 'La fecha es obligatoria.';
        const hoy = new Date(); hoy.setHours(0,0,0,0);
        if (new Date(i.value+'T00:00:00') > hoy) return 'La fecha no puede ser futura.';
        return null;
      });
      check('input[name="contrasennaNueva"]', i => {
        if (!i.value) return null;
        if (i.value.length < LIMITE.pasMin) return 'Mínimo ' + LIMITE.pasMin + ' caracteres.';
        return null;
      });
      check('input[name="contrasennaNuevaConfirm"]', i => {
        if (!i.value) return null;
        const n = form.querySelector('input[name="contrasennaNueva"]');
        if (n && i.value !== n.value) return 'Las contraseñas no coinciden.';
        return null;
      });
      check('input[name="contrasena"][type="password"]', i => {
        if (!i.value.trim()) return null;
        if (i.value.trim().length < LIMITE.pasMin) return 'Mínimo ' + LIMITE.pasMin + ' caracteres.';
        return null;
      });

      if (hayErrores) {
        e.preventDefault();
        const primero = form.querySelector('.val-error:not(:empty)');
        if (primero) primero.scrollIntoView({ behavior:'smooth', block:'center' });
      }
    });
  });

}); // fin DOMContentLoaded
