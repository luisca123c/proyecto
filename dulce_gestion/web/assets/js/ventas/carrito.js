// Modal para seleccionar cantidad al agregar un producto al carrito
function abrirModal(idProducto, nombre, stock) {
  document.getElementById('modalIdProducto').value = idProducto;
  document.getElementById('modalNombre').textContent = 'Agregar: ' + nombre;

  const input = document.getElementById('modalCantidad');
  input.max   = stock;
  input.value = 1;

  document.getElementById('modalOverlay').classList.add('activo');
  setTimeout(function () { input.focus(); input.select(); }, 120);
}

function cerrarModal() {
  document.getElementById('modalOverlay').classList.remove('activo');
}

document.getElementById('modalOverlay').addEventListener('click', function (e) {
  if (e.target === this) cerrarModal();
});
