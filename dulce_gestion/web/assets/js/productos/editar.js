// Preview de nombre e imagen para el formulario de edición de producto.
// Los datos del servidor (path de imagen actual, etc.) se leen desde
// el atributo data-* del elemento #prod-data en el HTML.
const data        = document.getElementById('prod-data');
const tieneImagen = data && data.dataset.tieneImagen === 'true';
const imgActual   = data ? data.dataset.imgPath : '';

const inputNombre   = document.getElementById('inputNombre');
const previewNombre = document.getElementById('previewNombre');
const inputImagen   = document.getElementById('inputImagen');
const imgPreview    = document.getElementById('imgPreview');
const chkEliminar   = document.getElementById('chkEliminar');

if (inputNombre && previewNombre) {
  inputNombre.addEventListener('input', () => {
    previewNombre.textContent = inputNombre.value.trim() || 'Producto';
  });
}

if (tieneImagen && chkEliminar && imgPreview) {
  chkEliminar.addEventListener('change', function () {
    imgPreview.innerHTML = this.checked
      ? '<i class="fi fi-sr-ice-cream"></i>'
      : '<img src="' + imgActual + '" alt="Preview">';
  });
}

if (inputImagen && imgPreview) {
  inputImagen.addEventListener('change', function () {
    if (this.files && this.files[0]) {
      const reader = new FileReader();
      reader.onload = e => {
        imgPreview.innerHTML = '<img src="' + e.target.result + '" alt="Preview">';
      };
      reader.readAsDataURL(this.files[0]);
    }
  });
}
