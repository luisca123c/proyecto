// Actualiza el texto de preview mientras se escribe el nombre
const inputNombre   = document.getElementById('inputNombre');
const previewNombre = document.getElementById('previewNombre');

if (inputNombre && previewNombre) {
  inputNombre.addEventListener('input', () => {
    const fallback = inputNombre.dataset.fallback || 'Nombre';
    previewNombre.textContent = inputNombre.value.trim() || fallback;
  });
}
