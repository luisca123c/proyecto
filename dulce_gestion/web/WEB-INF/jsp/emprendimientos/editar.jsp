<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario,
                 com.dulce_gestion.models.Emprendimiento" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    Emprendimiento e = (Emprendimiento) request.getAttribute("emprendimiento");
    String error = (String) request.getAttribute("error");
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Editar Emprendimiento | Dulce Gestión</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
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
        <h1 class="modulo-titulo"><i class="fi fi-sr-pencil"></i> Editar emprendimiento</h1>
        <a class="boton boton--secundario boton--sm" href="<%= ctx %>/emprendimientos">
          <i class="fi fi-sr-arrow-left"></i> Volver
        </a>
      </div>

      <% if (error != null) { %>
      <div class="alerta alerta-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <% if (e != null) { %>
      <div class="card-form">
        <form method="POST" action="<%= ctx %>/emprendimientos/editar">
          <input type="hidden" name="id" value="<%= e.getId() %>">

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">NOMBRE <span style="color:#e53935">*</span></label>
            <input class="nv-campo__input" type="text" name="nombre"
                   maxlength="100" required value="<%= e.getNombre() %>">
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">NIT</label>
            <input class="nv-campo__input" type="text" name="nit"
                   maxlength="30" value="<%= e.getNit() != null ? e.getNit() : "" %>">
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">DIRECCIÓN</label>
            <input class="nv-campo__input" type="text" name="direccion"
                   maxlength="150" value="<%= e.getDireccion() != null ? e.getDireccion() : "" %>">
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">CIUDAD</label>
            <input class="nv-campo__input" type="text" name="ciudad"
                   maxlength="100" value="<%= e.getCiudad() != null ? e.getCiudad() : "" %>">
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">TELÉFONO</label>
            <input class="nv-campo__input" type="tel" name="telefono"
                   maxlength="20" value="<%= e.getTelefono() != null ? e.getTelefono() : "" %>">
          </div>

          <div class="nv-campo">
            <label class="nv-campo__etiqueta">CORREO</label>
            <input class="nv-campo__input" type="email" name="correo"
                   maxlength="100" value="<%= e.getCorreo() != null ? e.getCorreo() : "" %>">
          </div>

          <div class="form-botones">
            <button type="submit" class="boton boton--primario">
              <i class="fi fi-sr-disk"></i> Guardar cambios
            </button>
            <a href="<%= ctx %>/emprendimientos" class="boton boton--secundario">
              <i class="fi fi-sr-cross"></i> Cancelar
            </a>
          </div>

        </form>
      </div>
      <% } %>

    </div>
  </main>
</body>
</html>
