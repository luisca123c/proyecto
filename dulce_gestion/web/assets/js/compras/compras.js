function abrirCrear()  { document.getElementById('modalCrear').classList.add('activo'); }
function cerrarCrear() { document.getElementById('modalCrear').classList.remove('activo'); }
function cerrarEditar(){ document.getElementById('modalEditar').classList.remove('activo'); }

document.getElementById('modalCrear').addEventListener('click', function(e) {
  if (e.target === this) cerrarCrear();
});
document.getElementById('modalEditar').addEventListener('click', function(e) {
  if (e.target === this) cerrarEditar();
});

// Validaciones del modal crear
document.getElementById('formCrear').addEventListener('submit', function(e) {
  var desc  = this.querySelector('textarea[name="descripcion"]');
  var total = this.querySelector('input[name="total"]');
  var fecha = this.querySelector('input[name="fecha"]');
  var ok = true;
  [desc, total, fecha].forEach(function(f) {
    var err = f.parentElement.querySelector('.val-error');
    if (!err) { err = document.createElement('span'); err.className = 'val-error'; f.parentElement.appendChild(err); }
    err.textContent = '';
    f.style.borderColor = '';
  });
  if (!desc.value.trim()) { mostrarErr(desc, 'La descripcion es obligatoria.'); ok = false; }
  if (!total.value || parseFloat(total.value) <= 0) { mostrarErr(total, 'El monto debe ser mayor a cero.'); ok = false; }
  if (!fecha.value) { mostrarErr(fecha, 'La fecha es obligatoria.'); ok = false; }
  if (!ok) e.preventDefault();
});

// Validaciones del modal editar
document.getElementById('formEditar').addEventListener('submit', function(e) {
  var desc  = this.querySelector('textarea[name="descripcion"]');
  var total = this.querySelector('input[name="total"]');
  var fecha = this.querySelector('input[name="fecha"]');
  var ok = true;
  if (!desc.value.trim()) { mostrarErr(desc, 'La descripcion es obligatoria.'); ok = false; }
  if (!total.value || parseFloat(total.value) <= 0) { mostrarErr(total, 'El monto debe ser mayor a cero.'); ok = false; }
  if (!fecha.value) { mostrarErr(fecha, 'La fecha es obligatoria.'); ok = false; }
  if (!ok) e.preventDefault();
});

function mostrarErr(input, msg) {
  var err = input.parentElement.querySelector('.val-error');
  if (!err) { err = document.createElement('span'); err.className = 'val-error'; input.parentElement.appendChild(err); }
  err.textContent = msg;
  input.style.borderColor = '#e53935';
}

// Abrir modal correcto si el servidor lo indica
window.addEventListener('DOMContentLoaded', function() {
  var data  = document.getElementById('compras-data');
  var abrir = data ? data.dataset.abrir : '';
  if (abrir === 'editar') document.getElementById('modalEditar').classList.add('activo');
  else if (abrir === 'crear') abrirCrear();
});
