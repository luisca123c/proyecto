/**
 * validaciones.js
 * Validaciones en tiempo real para todos los formularios del sistema.
 * - nombreCompleto: sin números, máx 100 caracteres
 * - telefono: solo dígitos, 7–10 caracteres
 * - correo: formato válido, máx 100 caracteres
 * - nombre (producto): sin números, máx 200 caracteres
 * - stock: entero >= 0
 * - precio: decimal > 0
 * - fechaVencimiento: no en el pasado
 * - contraseña: mínimo 8 caracteres (en perfil)
 */

/* ── Helpers globales ─────────────────────────────────────────────────── */
function valMostrarError(input, mensaje) {
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

function valLimpiarError(input) {
  const span = input.parentElement.querySelector('.val-error');
  if (span) span.textContent = '';
  input.style.borderColor = '';
}

/* ── Toggle mostrar/ocultar contraseña ───────────────────────────────── */
function togglePass(btn) {
  var input = btn.previousElementSibling;
  if (!input || input.tagName !== 'INPUT') {
    input = btn.parentElement.querySelector('input[type="password"], input[type="text"]');
  }
  if (!input) return;
  var mostrar = input.type === 'password';
  input.type = mostrar ? 'text' : 'password';
  var icon = btn.querySelector('i');
  if (icon) {
    icon.className = mostrar ? 'fi fi-sr-eye-crossed' : 'fi fi-sr-eye';
  }
}

/* ── Constantes de límites (deben coincidir con la BD) ────────────────── */
const LIMITE = {
  nombre:     100,  // perfil_usuario.nombre_completo VARCHAR(100)
  correo:     100,  // correos.correo VARCHAR(100)
  telefono:    10,  // telefonos.telefono VARCHAR(20) — máx dígitos prácticos
  telMin:       7,  // mínimo dígitos
  producto:   200,  // productos.nombre VARCHAR(200)
  prodDesc:   200,  // productos.descripcion VARCHAR(200)
  gastoDesc:  150,  // detalle_compra.descripcion / compras_insumos.descripcion VARCHAR(150)
  pasMin:       8,  // mínimo contraseña (perfil)
  pasMinEmp:    6,  // mínimo contraseña (crear/editar empleado)
};

const REGEX_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/* ══════════════════════════════════════════════════════════════════════
   Se ejecuta cuando el DOM está listo
══════════════════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', function () {

  /* ── NOMBRE COMPLETO ─────────────────────────────────────────────── */
  document.querySelectorAll('input[name="nombreCompleto"]').forEach(function (input) {
    if (input.type === 'hidden') return;
    input.setAttribute('maxlength', LIMITE.nombre);

    input.addEventListener('input', function () {
      // Eliminar dígitos en tiempo real
      const limpio = this.value.replace(/[0-9]/g, '');
      if (this.value !== limpio) this.value = limpio;

      const len = this.value.trim().length;
      if (/[0-9]/.test(this.value)) {
        valMostrarError(this, 'El nombre no puede contener números.');
      } else if (len > LIMITE.nombre) {
        valMostrarError(this, `Máximo ${LIMITE.nombre} caracteres (tienes ${len}).`);
      } else {
        valLimpiarError(this);
      }
    });

    input.addEventListener('blur', function () {
      if (this.value.trim() === '') { valLimpiarError(this); return; }
      if (/[0-9]/.test(this.value)) {
        valMostrarError(this, 'El nombre no puede contener números.');
      } else if (this.value.trim().length > LIMITE.nombre) {
        valMostrarError(this, `Máximo ${LIMITE.nombre} caracteres.`);
      } else {
        valLimpiarError(this);
      }
    });
  });

  /* ── TELÉFONO ────────────────────────────────────────────────────── */
  document.querySelectorAll('input[name="telefono"]').forEach(function (input) {
    input.setAttribute('maxlength', LIMITE.telefono);

    input.addEventListener('keydown', function (e) {
      const permitidas = ['Backspace','Delete','Tab','ArrowLeft','ArrowRight','Home','End'];
      if (permitidas.includes(e.key) || e.ctrlKey || e.metaKey) return;
      if (!/^\d$/.test(e.key)) {
        e.preventDefault();
        valMostrarError(this, 'El teléfono solo acepta números.');
      }
    });

    input.addEventListener('input', function () {
      const limpio = this.value.replace(/\D/g, '').slice(0, LIMITE.telefono);
      if (this.value !== limpio) this.value = limpio;

      const len = this.value.length;
      if (len > 0 && len < LIMITE.telMin) {
        valMostrarError(this, `Mínimo ${LIMITE.telMin} dígitos (tienes ${len}).`);
      } else if (len > LIMITE.telefono) {
        valMostrarError(this, `Máximo ${LIMITE.telefono} dígitos.`);
      } else {
        valLimpiarError(this);
      }
    });

    input.addEventListener('blur', function () {
      if (this.value.trim() === '') { valLimpiarError(this); return; }
      const len = this.value.length;
      if (len < LIMITE.telMin) {
        valMostrarError(this, `Mínimo ${LIMITE.telMin} dígitos (tienes ${len}).`);
      } else {
        valLimpiarError(this);
      }
    });
  });

  /* ── CORREO ──────────────────────────────────────────────────────── */
  document.querySelectorAll('input[name="correo"]').forEach(function (input) {
    input.setAttribute('maxlength', LIMITE.correo);

    input.addEventListener('input', function () {
      if (this.value.length > LIMITE.correo) {
        valMostrarError(this, `Máximo ${LIMITE.correo} caracteres.`);
      } else if (REGEX_CORREO.test(this.value.trim())) {
        valLimpiarError(this);
      }
    });

    input.addEventListener('blur', function () {
      if (this.value.trim() === '') { valLimpiarError(this); return; }
      if (this.value.length > LIMITE.correo) {
        valMostrarError(this, `Máximo ${LIMITE.correo} caracteres.`);
      } else if (!REGEX_CORREO.test(this.value.trim())) {
        valMostrarError(this, 'Escribe un correo válido (ej: usuario@dominio.com).');
      } else {
        valLimpiarError(this);
      }
    });
  });

  /* ── DESCRIPCIÓN DE PRODUCTO (opcional, máx 200) ───────────────── */
  document.querySelectorAll('input[name="descripcion"]').forEach(function (input) {
    input.setAttribute('maxlength', LIMITE.prodDesc);
    input.addEventListener('blur', function () {
      if (this.value.length > LIMITE.prodDesc) {
        valMostrarError(this, `Máximo ${LIMITE.prodDesc} caracteres.`);
      } else {
        valLimpiarError(this);
      }
    });
  });

  /* ── DESCRIPCIÓN DE GASTOS/COMPRAS (obligatoria, máx 150) ───────── */
  document.querySelectorAll('textarea[name="descripcion"]').forEach(function (input) {
    input.setAttribute('maxlength', LIMITE.gastoDesc);
    input.addEventListener('blur', function () {
      if (this.value.trim() === '') {
        valMostrarError(this, 'La descripción es obligatoria.');
      } else if (this.value.length > LIMITE.gastoDesc) {
        valMostrarError(this, `Máximo ${LIMITE.gastoDesc} caracteres (tienes ${this.value.length}).`);
      } else {
        valLimpiarError(this);
      }
    });
    input.addEventListener('input', function () {
      if (this.value.length <= LIMITE.gastoDesc && this.value.trim() !== '') valLimpiarError(this);
    });
  });

    /* ── NOMBRE DE PRODUCTO ──────────────────────────────────────────── */
  document.querySelectorAll('input[name="nombre"]').forEach(function (input) {
    if (input.type === 'hidden') return;
    input.setAttribute('maxlength', LIMITE.producto);

    input.addEventListener('input', function () {
      const limpio = this.value.replace(/[0-9]/g, '');
      if (this.value !== limpio) this.value = limpio;

      const len = this.value.trim().length;
      if (/[0-9]/.test(this.value)) {
        valMostrarError(this, 'El nombre no puede contener números.');
      } else if (len > LIMITE.producto) {
        valMostrarError(this, `Máximo ${LIMITE.producto} caracteres (tienes ${len}).`);
      } else {
        valLimpiarError(this);
      }
    });

    input.addEventListener('blur', function () {
      if (this.value.trim() === '') { valLimpiarError(this); return; }
      if (/[0-9]/.test(this.value)) {
        valMostrarError(this, 'El nombre no puede contener números.');
      } else if (this.value.trim().length > LIMITE.producto) {
        valMostrarError(this, `Máximo ${LIMITE.producto} caracteres.`);
      } else {
        valLimpiarError(this);
      }
    });
  });

  /* ── STOCK ───────────────────────────────────────────────────────── */
  document.querySelectorAll('input[name="stock"]').forEach(function (input) {
    input.addEventListener('blur', function () {
      if (this.value === '') { valLimpiarError(this); return; }
      const val = parseInt(this.value);
      if (isNaN(val) || val < 0) {
        valMostrarError(this, 'El stock debe ser un número mayor o igual a 0.');
      } else {
        valLimpiarError(this);
      }
    });
    input.addEventListener('input', function () {
      if (parseInt(this.value) >= 0) valLimpiarError(this);
    });
  });

  /* ── PRECIO ──────────────────────────────────────────────────────── */
  document.querySelectorAll('input[name="precio"]').forEach(function (input) {
    input.addEventListener('blur', function () {
      if (this.value === '') { valLimpiarError(this); return; }
      const val = parseFloat(this.value);
      if (isNaN(val) || val <= 0) {
        valMostrarError(this, 'El precio debe ser mayor a cero.');
      } else {
        valLimpiarError(this);
      }
    });
    input.addEventListener('input', function () {
      if (parseFloat(this.value) > 0) valLimpiarError(this);
    });
  });

  /* ── FECHA DE VENCIMIENTO ────────────────────────────────────────── */
  document.querySelectorAll('input[name="fechaVencimiento"]').forEach(function (input) {
    // Establecer mínimo como hoy
    const hoyISO = new Date().toISOString().split('T')[0];
    input.setAttribute('min', hoyISO);

    input.addEventListener('change', function () {
      if (!this.value) { valLimpiarError(this); return; }
      const hoy     = new Date(); hoy.setHours(0,0,0,0);
      const elegida = new Date(this.value + 'T00:00:00');
      if (elegida < hoy) {
        valMostrarError(this, 'La fecha de vencimiento no puede ser anterior a hoy.');
      } else {
        valLimpiarError(this);
      }
    });
  });

  /* ── CONTRASEÑA NUEVA (perfil) ───────────────────────────────────── */
  document.querySelectorAll('input[name="contrasennaNueva"]').forEach(function (input) {
    input.addEventListener('blur', function () {
      if (this.value === '') { valLimpiarError(this); return; }
      if (this.value.length < LIMITE.pasMin) {
        valMostrarError(this, `Mínimo ${LIMITE.pasMin} caracteres.`);
      } else {
        valLimpiarError(this);
      }
    });
  });

  document.querySelectorAll('input[name="contrasennaNuevaConfirm"]').forEach(function (input) {
    input.addEventListener('blur', function () {
      if (this.value === '') { valLimpiarError(this); return; }
      const nueva = document.querySelector('input[name="contrasennaNueva"]');
      if (nueva && this.value !== nueva.value) {
        valMostrarError(this, 'Las contraseñas no coinciden.');
      } else {
        valLimpiarError(this);
      }
    });
  });

  /* ── CONTRASEÑA (nuevo/editar empleado) ──────────────────────────── */
  document.querySelectorAll('input[name="contrasena"]').forEach(function (input) {
    if (input.type === 'password') {
      input.addEventListener('blur', function () {
        if (this.value === '' || this.value.trim() === '') { valLimpiarError(this); return; }
        if (this.value.trim().length < 6) {
          valMostrarError(this, 'Mínimo 6 caracteres.');
        } else {
          valLimpiarError(this);
        }
      });
    }
  });

  /* ── BLOQUEO EN SUBMIT ───────────────────────────────────────────── */
  document.querySelectorAll('form').forEach(function (form) {
    if (form.dataset.valInit) return;
    form.dataset.valInit = '1';

    form.addEventListener('submit', function (e) {
      let hayErrores = false;

      const check = (selector, fn) => {
        form.querySelectorAll(selector).forEach(function (input) {
          if (input.type === 'hidden') return;
          const msg = fn(input);
          if (msg) { valMostrarError(input, msg); hayErrores = true; }
        });
      };

      // Nombre completo
      check('input[name="nombreCompleto"]', inp => {
        if (/[0-9]/.test(inp.value)) return 'El nombre no puede contener números.';
        if (inp.value.trim().length > LIMITE.nombre) return `Máximo ${LIMITE.nombre} caracteres.`;
        return null;
      });

      // Teléfono
      check('input[name="telefono"]', inp => {
        const val = inp.value.replace(/\D/g, '');
        if (val.length > 0 && val.length < LIMITE.telMin) return `Mínimo ${LIMITE.telMin} dígitos.`;
        if (val.length > LIMITE.telefono) return `Máximo ${LIMITE.telefono} dígitos.`;
        return null;
      });

      // Correo
      check('input[name="correo"]', inp => {
        if (inp.value.trim() === '') return null;
        if (inp.value.length > LIMITE.correo) return `Máximo ${LIMITE.correo} caracteres.`;
        if (!REGEX_CORREO.test(inp.value.trim())) return 'Correo inválido (ej: usuario@dominio.com).';
        return null;
      });

      // Nombre producto
      check('input[name="nombre"]', inp => {
        if (/[0-9]/.test(inp.value)) return 'El nombre no puede contener números.';
        if (inp.value.trim().length > LIMITE.producto) return `Máximo ${LIMITE.producto} caracteres.`;
        return null;
      });

      // Stock
      check('input[name="stock"]', inp => {
        if (inp.value === '') return null;
        const val = parseInt(inp.value);
        if (isNaN(val) || val < 0) return 'El stock debe ser mayor o igual a 0.';
        return null;
      });

      // Precio
      check('input[name="precio"]', inp => {
        if (inp.value === '') return null;
        const val = parseFloat(inp.value);
        if (isNaN(val) || val <= 0) return 'El precio debe ser mayor a cero.';
        return null;
      });

      // Fecha vencimiento
      check('input[name="fechaVencimiento"]', inp => {
        if (!inp.value) return null;
        const hoy     = new Date(); hoy.setHours(0,0,0,0);
        const elegida = new Date(inp.value + 'T00:00:00');
        if (elegida < hoy) return 'La fecha no puede ser anterior a hoy.';
        return null;
      });

      // Contraseña nueva (perfil)
      check('input[name="contrasennaNueva"]', inp => {
        if (inp.value === '') return null;
        if (inp.value.length < LIMITE.pasMin) return `Mínimo ${LIMITE.pasMin} caracteres.`;
        return null;
      });
      check('input[name="contrasennaNuevaConfirm"]', inp => {
        if (inp.value === '') return null;
        const nueva = form.querySelector('input[name="contrasennaNueva"]');
        if (nueva && inp.value !== nueva.value) return 'Las contraseñas no coinciden.';
        return null;
      });

      if (hayErrores) {
        e.preventDefault();
        const primerError = form.querySelector('.val-error:not(:empty)');
        if (primerError) primerError.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    });
  });

});
