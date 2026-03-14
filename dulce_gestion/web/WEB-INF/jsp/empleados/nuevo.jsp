<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    boolean esSuperAdmin = Boolean.TRUE.equals(request.getAttribute("esSuperAdmin"));
    String error = (String) request.getAttribute("error");

    // Conservar datos si hubo error (para no perder lo escrito)
    String vNombre    = request.getParameter("nombreCompleto") != null ? request.getParameter("nombreCompleto") : "";
    String vTelefono  = request.getParameter("telefono")       != null ? request.getParameter("telefono")       : "";
    String vGenero    = request.getParameter("genero")         != null ? request.getParameter("genero")         : "";
    String vCorreo    = request.getParameter("correo")         != null ? request.getParameter("correo")         : "";
    String vEstado    = request.getParameter("estado")         != null ? request.getParameter("estado")         : "Activo";
    String vRol       = request.getParameter("rol")            != null ? request.getParameter("rol")            : "Empleado";
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Nuevo Empleado | Dulce Gestión</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
</head>

<body class="layout-app">

  <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle" aria-label="Abrir menú">

  <!-- HEADER -->
  <header class="main-header header-app">
    <div class="header-app__izquierda">
      <label for="sidebar-toggle" class="header-app__btn-menu" aria-label="Abrir menú">
        <i class="fi fi-sr-menu-burger"></i>
      </label>
      <div class="header-app__marca">
        <img class="header-app__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo Dulce Gestión">
        <span class="header-app__titulo">Dulce Gestión</span>
      </div>
    </div>
    <div class="header-app__acciones">
      <a class="header-app__icono" href="<%= ctx %>/logout" aria-label="Cerrar sesión" title="Cerrar sesión">
        <i class="fi fi-sr-sign-out-alt"></i>
      </a>
    </div>
  </header>

  <label for="sidebar-toggle" class="sidebar__overlay" aria-label="Cerrar menú"></label>

  <!-- SIDEBAR -->
  <aside class="main-sidebar sidebar">
    <div class="sidebar__top">
      <div class="sidebar__brand">
        <img class="sidebar__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo Dulce Gestión">
        <span class="sidebar__brand-text">Menú</span>
      </div>
      <label for="sidebar-toggle" class="sidebar__cerrar" aria-label="Cerrar menú">
        <i class="fi fi-sr-cross"></i>
      </label>
    </div>
    <nav class="sidebar__nav">
      <a class="sidebar__link" href="<%= ctx %>/dashboard">
        <i class="fi fi-sr-home"></i><span>Inicio</span>
      </a>
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/empleados">
        <i class="fi fi-sr-users"></i><span>Empleados</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/productos">
        <i class="fi fi-sr-box-open"></i><span>Productos</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/ventas">
        <i class="fi fi-sr-shopping-cart"></i><span>Ventas</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/gastos">
        <i class="fi fi-sr-receipt"></i><span>Gastos</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ganancias">
        <i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/perfil">
        <i class="fi fi-sr-user"></i><span>Perfil</span>
      </a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout">
        <i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesión</span>
      </a>
    </nav>
  </aside>

  <!-- MAIN -->
  <main class="pagina-main">
    <div class="modulo-contenido nuevo-empleado-contenido">

      <!-- Avatar + nombre preview -->
      <div class="nuevo-empleado__avatar-bloque">
        <div class="nuevo-empleado__avatar">
          <i class="fi fi-sr-user"></i>
        </div>
        <span class="nuevo-empleado__nombre-preview" id="previewNombre">
          <%= vNombre.isBlank() ? "Nombre" : vNombre %>
        </span>
      </div>

      <!-- Error -->
      <% if (error != null && !error.isBlank()) { %>
      <div class="modulo-error">
        <i class="fi fi-sr-triangle-warning"></i>
        <%= error %>
      </div>
      <% } %>

      <!-- FORMULARIO -->
      <form method="POST" action="<%= ctx %>/empleados/nuevo" novalidate>

        <!-- Nombre completo -->
        <div class="nv-campo">
          <label class="nv-campo__label">Nombre Completo Empleado</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input"
                   type="text" name="nombreCompleto"
                   placeholder="Nombre completo"
                   value="<%= vNombre %>"
                   id="inputNombre" data-fallback="Nombre"
                   required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Teléfono -->
        <div class="nv-campo">
          <label class="nv-campo__label">Teléfono Empleado</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input"
                   type="tel" name="telefono"
                   placeholder="Número de teléfono"
                   value="<%= vTelefono %>"
                   required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Género -->
        <div class="nv-campo">
          <label class="nv-campo__label">Género</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <select class="nv-campo__input" name="genero" required>
              <option value="" disabled <%= vGenero.isBlank() ? "selected" : "" %>>Selecciona</option>
              <option value="Masculino"  <%= "Masculino" .equals(vGenero) ? "selected" : "" %>>Masculino</option>
              <option value="Femenino"   <%= "Femenino"  .equals(vGenero) ? "selected" : "" %>>Femenino</option>
              <option value="Otro"       <%= "Otro"      .equals(vGenero) ? "selected" : "" %>>Otro</option>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Fecha ingreso (solo visual, se guarda automáticamente) -->
        <div class="nv-campo">
          <label class="nv-campo__label">Fecha Ingreso</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input nv-campo__input--readonly"
                   type="text"
                   value="<%= new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date()) %>"
                   readonly>
            <i class="fi fi-sr-calendar nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Correo -->
        <div class="nv-campo">
          <label class="nv-campo__label">Correo</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input"
                   type="email" name="correo"
                   placeholder="correo@ejemplo.com"
                   value="<%= vCorreo %>"
                   required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Contraseña -->
        <div class="nv-campo">
          <label class="nv-campo__label">Contraseña</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input"
                   type="password" name="contrasena"
                   placeholder="Mínimo 6 caracteres"
                   required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Estado -->
        <div class="nv-campo">
          <label class="nv-campo__label">Estado</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <select class="nv-campo__input" name="estado" required>
              <option value="Activo"   <%= "Activo"  .equals(vEstado) ? "selected" : "" %>>Activo</option>
              <option value="Inactivo" <%= "Inactivo".equals(vEstado) ? "selected" : "" %>>Inactivo</option>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Rol -->
        <div class="nv-campo">
          <label class="nv-campo__label">Rol</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <select class="nv-campo__input" name="rol" required>
              <option value="Empleado"       <%= "Empleado"      .equals(vRol) ? "selected" : "" %>>Empleado</option>
              <% if (esSuperAdmin) { %>
              <option value="Administrador"  <%= "Administrador" .equals(vRol) ? "selected" : "" %>>Administrador</option>
              <% } %>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Botones -->
        <div class="nv-botones">
          <button type="submit" class="nv-btn nv-btn--agregar">
            Agregar
          </button>
          <a href="<%= ctx %>/empleados" class="nv-btn nv-btn--volver">
            Volver
          </a>
        </div>

      </form>

    </div>
  </main>

</body>
<script src="<%= ctx %>/assets/js/empleados/preview-nombre.js" defer></script>
<script src="<%= ctx %>/assets/js/validaciones.js" defer></script>
</html>
