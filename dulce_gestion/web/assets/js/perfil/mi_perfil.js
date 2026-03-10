// Lógica de tabs y validación de contraseña en la página de perfil
function mostrarTab(tabName, event) {
  event.preventDefault();

  document.querySelectorAll('[id^="tab-"]').forEach(tab => {
    tab.style.display = 'none';
  });

  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.style.color = '#999';
    btn.style.borderBottomColor = 'transparent';
  });

  document.getElementById('tab-' + tabName).style.display = 'block';
  event.target.style.color = '#667eea';
  event.target.style.borderBottomColor = '#667eea';
}

document.addEventListener('DOMContentLoaded', function () {
  const formSeguridad = document.getElementById('formContrasena');
  if (!formSeguridad) return;

  formSeguridad.addEventListener('submit', function (e) {
    const nueva   = document.querySelector('input[name="contrasennaNueva"]').value;
    const confirm = document.querySelector('input[name="contrasennaNuevaConfirm"]').value;

    if (nueva !== confirm) {
      e.preventDefault();
      alert('Las contraseñas no coinciden');
      return false;
    }

    if (nueva.length < 8) {
      e.preventDefault();
      alert('La contraseña debe tener al menos 8 caracteres');
      return false;
    }
  });
});
