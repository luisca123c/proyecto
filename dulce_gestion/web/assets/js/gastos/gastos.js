// Manejo de modales de Crear y Editar gasto.
// El JSP emite data-abrir="crear"|"editar"|"" en #gastos-data
// para indicar qué modal abrir al cargar la página.
function abrirCrear()  { document.getElementById('modalCrear').classList.add('activo'); }
function cerrarCrear() { document.getElementById('modalCrear').classList.remove('activo'); }
function cerrarEditar(){ document.getElementById('modalEditar').classList.remove('activo'); }

// Cerrar al hacer clic en el fondo del modal
document.getElementById('modalCrear').addEventListener('click', function (e) {
  if (e.target === this) cerrarCrear();
});
document.getElementById('modalEditar').addEventListener('click', function (e) {
  if (e.target === this) cerrarEditar();
});

// Abrir modal correcto si el servidor lo indica (después de un error de validación)
window.addEventListener('DOMContentLoaded', function () {
  const data  = document.getElementById('gastos-data');
  const abrir = data ? data.dataset.abrir : '';

  if (abrir === 'editar') {
    document.getElementById('modalEditar').classList.add('activo');
  } else if (abrir === 'crear') {
    abrirCrear();
  }
});
