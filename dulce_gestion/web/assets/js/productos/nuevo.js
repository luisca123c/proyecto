// Preview de nombre e imagen para el formulario de nuevo producto
const inputNombre   = document.getElementById('inputNombre');
const previewNombre = document.getElementById('previewNombre');
const inputImagen   = document.getElementById('inputImagen');
const imgPreview    = document.getElementById('imgPreview');

if (inputNombre && previewNombre) {
  inputNombre.addEventListener('input', () => {
    previewNombre.textContent = inputNombre.value.trim() || 'Nuevo Producto';
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
