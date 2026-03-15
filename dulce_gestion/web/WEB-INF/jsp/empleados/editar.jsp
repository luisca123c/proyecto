<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    Usuario obj = (Usuario) request.getAttribute("objetivo");
    boolean esSuperAdmin = Boolean.TRUE.equals(request.getAttribute("esSuperAdmin"));
    String error = (String) request.getAttribute("error");

    // Si hubo error POST, tomar los valores del POST; si no, del objeto BD
    String vNombre   = request.getParameter("nombreCompleto") != null ? request.getParameter("nombreCompleto") : obj.getNombreCompleto();
    String vTelefono = request.getParameter("telefono")       != null ? request.getParameter("telefono")       : obj.getTelefono();
    String vGenero   = request.getParameter("genero")         != null ? request.getParameter("genero")         : obj.getGenero();
    String vCorreo   = request.getParameter("correo")         != null ? request.getParameter("correo")         : obj.getCorreo();
    String vEstado   = request.getParameter("estado")         != null ? request.getParameter("estado")         : obj.getEstado();
    String vRol      = request.getParameter("rol")            != null ? request.getParameter("rol")            : obj.getNombreRol();
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Editar Empleado | Dulce Gestión</title>
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
        <img class="header-app__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo">
        <span class="header-app__titulo">Dulce Gestión</span>
      </div>
    </div>
    <div class="header-app__acciones">
      <a class="header-app__icono" href="<%= ctx %>/logout" title="Cerrar sesión">
        <i class="fi fi-sr-sign-out-alt"></i>
      </a>
    </div>
  </header>

  <label for="sidebar-toggle" class="sidebar__overlay" aria-label="Cerrar menú"></label>

  <!-- SIDEBAR -->
  <aside class="main-sidebar sidebar">
    <div class="sidebar__top">
      <div class="sidebar__brand">
        <img class="sidebar__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo">
        <span class="sidebar__brand-text">Menú</span>
      </div>
      <label for="sidebar-toggle" class="sidebar__cerrar">
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
        <i class="fi fi-sr-shopping-cart"></i><span>Carrito</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/historial"><i class="fi fi-sr-chart-histogram"></i><span>Ventas</span></a>
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

      <!-- Avatar + nombre -->
      <div class="nuevo-empleado__avatar-bloque">
        <div class="nuevo-empleado__avatar">
          <i class="fi fi-sr-user"></i>
        </div>
        <span class="nuevo-empleado__nombre-preview" id="previewNombre">
          <%= vNombre %>
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
      <form method="POST" action="<%= ctx %>/empleados/editar" novalidate>
        <input type="hidden" name="id" value="<%= obj.getId() %>">

        <!-- Nombre -->
        <div class="nv-campo">
          <label class="nv-campo__label">Nombre Completo</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="text" name="nombreCompleto" maxlength="100"
                   id="inputNombre" data-fallback="Nombre" value="<%= vNombre %>" required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Teléfono -->
        <div class="nv-campo">
          <label class="nv-campo__label">Teléfono</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="tel" name="telefono" maxlength="10"
                   value="<%= vTelefono %>" required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Género -->
        <div class="nv-campo">
          <label class="nv-campo__label">Género</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <select class="nv-campo__input" name="genero" required>
              <option value="Masculino" <%= "Masculino".equals(vGenero) ? "selected" : "" %>>Masculino</option>
              <option value="Femenino"  <%= "Femenino" .equals(vGenero) ? "selected" : "" %>>Femenino</option>
              <option value="Otro"      <%= "Otro"     .equals(vGenero) ? "selected" : "" %>>Otro</option>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Correo -->
        <div class="nv-campo">
          <label class="nv-campo__label">Correo</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="email" name="correo" maxlength="100"
                   value="<%= vCorreo %>" required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Nueva contraseña (opcional) -->
        <div class="nv-campo">
          <label class="nv-campo__label">Nueva Contraseña <span style="opacity:.6;font-weight:400">(dejar vacío para no cambiar)</span></label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="password" name="contrasena" maxlength="100"
                   placeholder="Mínimo 6 caracteres">
            <button type="button" onclick="togglePass(this)" class="nv-campo__icono-edit" title="Mostrar/ocultar" style="background:none;border:none;cursor:pointer;padding:0;display:flex;align-items:center;color:#aaa;">
              <i class="fi fi-sr-eye"></i>
            </button>
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

        <!-- Rol — solo SuperAdmin puede cambiarlo -->
        <div class="nv-campo">
          <label class="nv-campo__label">Rol</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <% if (esSuperAdmin) { %>
            <select class="nv-campo__input" name="rol" required>
              <option value="Empleado"      <%= "Empleado"     .equals(vRol) ? "selected" : "" %>>Empleado</option>
              <option value="Administrador" <%= "Administrador".equals(vRol) ? "selected" : "" %>>Administrador</option>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
            <% } else { %>
            <input class="nv-campo__input nv-campo__input--readonly"
                   type="text" value="<%= vRol %>" readonly>
            <input type="hidden" name="rol" value="<%= vRol %>">
            <% } %>
          </div>
        </div>

        <!-- Botones -->
        <div class="nv-botones">
          <button type="submit" class="nv-btn nv-btn--agregar">
            Guardar cambios
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
