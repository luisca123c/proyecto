<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx  = request.getContextPath();
    String error = (String) request.getAttribute("error");
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Nuevo Emprendimiento | Dulce Gestión</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
  <style>
    .val-err { color:#e53935; font-size:0.78rem; margin-top:4px; display:block; }
    .nv-campo__input.input-error { border-color:#e53935; }
  </style>
</head>
<body class="layout-app">

  <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle">

  <header class="main-header header-app">
    <div class="header-app__izquierda">
      <label for="sidebar-toggle" class="header-app__btn-menu"><i class="fi fi-sr-menu-burger"></i></label>
      <div class="header-app__marca">
        <img class="header-app__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo">
        <span class="header-app__titulo">Dulce Gestión</span>
      </div>
    </div>
    <div class="header-app__acciones">
      <a class="header-app__icono" href="<%= ctx %>/logout"><i class="fi fi-sr-sign-out-alt"></i></a>
    </div>
  </header>

  <label for="sidebar-toggle" class="sidebar__overlay"></label>

  <aside class="main-sidebar sidebar">
    <div class="sidebar__top">
      <div class="sidebar__brand">
        <img class="sidebar__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo">
        <span class="sidebar__brand-text">Menú</span>
      </div>
      <label for="sidebar-toggle" class="sidebar__cerrar"><i class="fi fi-sr-cross"></i></label>
    </div>
    <nav class="sidebar__nav">
      <a class="sidebar__link" href="<%= ctx %>/dashboard"><i class="fi fi-sr-home"></i><span>Inicio</span></a>
      <a class="sidebar__link" href="<%= ctx %>/empleados"><i class="fi fi-sr-users"></i><span>Empleados</span></a>
      <a class="sidebar__link" href="<%= ctx %>/productos"><i class="fi fi-sr-box-open"></i><span>Productos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ventas"><i class="fi fi-sr-shopping-cart"></i><span>Carrito</span></a>
      <a class="sidebar__link" href="<%= ctx %>/historial"><i class="fi fi-sr-chart-histogram"></i><span>Ventas</span></a>
      <a class="sidebar__link" href="<%= ctx %>/gastos"><i class="fi fi-sr-receipt"></i><span>Gastos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ganancias"><i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span></a>
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/emprendimientos"><i class="fi fi-sr-store-alt"></i><span>Emprendimientos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/configuracion"><i class="fi fi-sr-settings"></i><span>Configuración</span></a>
      <a class="sidebar__link" href="<%= ctx %>/perfil"><i class="fi fi-sr-user"></i><span>Perfil</span></a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout"><i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesión</span></a>
    </nav>
  </aside>

  <main class="pagina-main">
    <div class="modulo-contenido" style="max-width:640px">

      <div class="modulo-encabezado">
        <h1 class="modulo-titulo"><i class="fi fi-sr-store-alt"></i> Nuevo emprendimiento</h1>
        <a class="boton boton--secundario boton--sm" href="<%= ctx %>/emprendimientos">
          <i class="fi fi-sr-arrow-left"></i> Volver
        </a>
      </div>

      <% if (error != null) { %>
      <div class="alerta alerta-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <div class="card-form">
        <form id="formNuevo" method="POST" action="<%= ctx %>/emprendimientos/nuevo" novalidate>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">NOMBRE <span style="color:#e53935">*</span></label>
            <input class="nv-campo__input" type="text" name="nombre" id="f_nombre"
                   maxlength="100" placeholder="Ej: Dulce Gestión"
                   value="<%= request.getParameter("nombre") != null ? request.getParameter("nombre") : "" %>">
            <span class="val-err" id="err_nombre"></span>
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">NIT <span style="color:#e53935">*</span></label>
            <input class="nv-campo__input" type="text" name="nit" id="f_nit"
                   maxlength="30" placeholder="Ej: 900123456-1"
                   value="<%= request.getParameter("nit") != null ? request.getParameter("nit") : "" %>">
            <span class="val-err" id="err_nit"></span>
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">DIRECCIÓN <span style="color:#e53935">*</span></label>
            <input class="nv-campo__input" type="text" name="direccion" id="f_direccion"
                   maxlength="150" placeholder="Ej: Calle 10 # 5-20"
                   value="<%= request.getParameter("direccion") != null ? request.getParameter("direccion") : "" %>">
            <span class="val-err" id="err_direccion"></span>
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">CIUDAD <span style="color:#e53935">*</span></label>
            <input class="nv-campo__input" type="text" name="ciudad" id="f_ciudad"
                   maxlength="100" placeholder="Ej: Bucaramanga"
                   value="<%= request.getParameter("ciudad") != null ? request.getParameter("ciudad") : "" %>">
            <span class="val-err" id="err_ciudad"></span>
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">TELÉFONO <span style="color:#e53935">*</span></label>
            <input class="nv-campo__input" type="tel" name="telefono" id="f_telefono"
                   maxlength="20" placeholder="Ej: 3001234567"
                   value="<%= request.getParameter("telefono") != null ? request.getParameter("telefono") : "" %>">
            <span class="val-err" id="err_telefono"></span>
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">CORREO <span style="color:#e53935">*</span></label>
            <input class="nv-campo__input" type="email" name="correo" id="f_correo"
                   maxlength="100" placeholder="Ej: contacto@emprendimiento.com"
                   value="<%= request.getParameter("correo") != null ? request.getParameter("correo") : "" %>">
            <span class="val-err" id="err_correo"></span>
          </div>

          <div class="form-botones">
            <button type="submit" class="boton boton--primario">
              <i class="fi fi-sr-disk"></i> Guardar emprendimiento
            </button>
            <a href="<%= ctx %>/emprendimientos" class="boton boton--secundario">
              <i class="fi fi-sr-cross"></i> Cancelar
            </a>
          </div>

        </form>
      </div>

    </div>
  </main>

<script>
(function () {
  var regexEmail    = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  var regexTel      = /^[0-9+\-\s()]{7,20}$/;
  var regexNit      = /^[0-9]{6,15}(-[0-9])?$/;
  var regexCiudad   = /^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\s]{2,100}$/;
  var regexDireccion = /^[a-zA-Z0-9áéíóúÁÉÍÓÚüÜñÑ\s#\-\.°]+$/;

  function err(id, msg) {
    var el = document.getElementById('err_' + id);
    var inp = document.getElementById('f_' + id);
    if (el) el.textContent = msg;
    if (inp) inp.classList.toggle('input-error', !!msg);
  }

  function validarCampo(id) {
    var inp = document.getElementById('f_' + id);
    if (!inp) return true;
    var v = inp.value.trim();
    if (!v) { err(id, 'Este campo es obligatorio.'); return false; }
    if (id === 'nombre'    && v.length < 2)            { err(id, 'Mínimo 2 caracteres.'); return false; }
    if (id === 'nit'       && !regexNit.test(v))        { err(id, 'Formato inválido. Ej: 900123456-1'); return false; }
    if (id === 'ciudad'    && v.length < 2)             { err(id, 'Mínimo 2 caracteres.'); return false; }
    if (id === 'ciudad'    && !regexCiudad.test(v))      { err(id, 'Solo letras y espacios, sin números ni símbolos.'); return false; }
    if (id === 'direccion' && v.length < 5)             { err(id, 'Mínimo 5 caracteres.'); return false; }
    if (id === 'direccion' && !regexDireccion.test(v))   { err(id, 'Solo letras, números, #, - y punto.'); return false; }
    if (id === 'telefono'  && !regexTel.test(v))        { err(id, 'Solo dígitos, +, - y espacios (mín. 7).'); return false; }
    if (id === 'correo'    && !regexEmail.test(v))      { err(id, 'Formato de correo inválido.'); return false; }
    err(id, '');
    return true;
  }

  ['nombre','nit','direccion','ciudad','telefono','correo'].forEach(function(id) {
    var inp = document.getElementById('f_' + id);
    if (inp) {
      inp.addEventListener('blur',  function() { validarCampo(id); });
      inp.addEventListener('input', function() { if (inp.classList.contains('input-error')) validarCampo(id); });
    }
  });

  document.getElementById('formNuevo').addEventListener('submit', function(e) {
    var ok = ['nombre','nit','direccion','ciudad','telefono','correo'].map(validarCampo).every(Boolean);
    if (!ok) e.preventDefault();
  });
})();
</script>
</body>
</html>
