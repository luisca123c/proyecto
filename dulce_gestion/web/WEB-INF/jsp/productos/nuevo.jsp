<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario, com.dulce_gestion.models.Emprendimiento, java.util.List" %>
<%
    String ctx    = request.getContextPath();
    String error  = (String) request.getAttribute("error");
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String rolSolicitante = sesionUsuario != null ? sesionUsuario.getNombreRol() : "";
    boolean puedeEditar   = "SuperAdministrador".equals(rolSolicitante) || "Administrador".equals(rolSolicitante);
    boolean esSuperAdmin  = "SuperAdministrador".equals(rolSolicitante);
    List<String[]> categorias = (List<String[]>) request.getAttribute("categorias");
    List<String[]> unidades   = (List<String[]>) request.getAttribute("unidades");

    String vNombre  = request.getParameter("nombre")           != null ? request.getParameter("nombre")           : "";
    String vDesc    = request.getParameter("descripcion")      != null ? request.getParameter("descripcion")      : "";
    String vStock   = request.getParameter("stock")            != null ? request.getParameter("stock")            : "";
    String vPrecio  = request.getParameter("precio")           != null ? request.getParameter("precio")           : "";
    String vEstado  = request.getParameter("estado")           != null ? request.getParameter("estado")           : "Disponible";
    String vFecha   = request.getParameter("fechaVencimiento") != null ? request.getParameter("fechaVencimiento") : "";
    String vCat     = request.getParameter("idCategoria")      != null ? request.getParameter("idCategoria")      : "";
    String vUnidad  = request.getParameter("idUnidad")         != null ? request.getParameter("idUnidad")         : "";
    String vEmpId   = request.getParameter("idEmprendimiento") != null ? request.getParameter("idEmprendimiento") : "";
    List<Emprendimiento> emprendimientos = (List<Emprendimiento>) request.getAttribute("emprendimientos");
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Nuevo Producto | Dulce Gestion</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
</head>
<body class="layout-app">

  <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle" aria-label="Abrir menu">

  <!-- HEADER -->
  <header class="main-header header-app">
    <div class="header-app__izquierda">
      <label for="sidebar-toggle" class="header-app__btn-menu">
        <i class="fi fi-sr-menu-burger"></i>
      </label>
      <div class="header-app__marca">
        <img class="header-app__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo">
        <span class="header-app__titulo">Dulce Gestion</span>
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
        <span class="sidebar__brand-text">Menu</span>
      </div>
      <label for="sidebar-toggle" class="sidebar__cerrar"><i class="fi fi-sr-cross"></i></label>
    </div>
        <nav class="sidebar__nav">
      <a class="sidebar__link" href="<%= ctx %>/dashboard">
        <i class="fi fi-sr-home"></i><span>Inicio</span>
      </a>
      <% if (puedeEditar) { %>
      <a class="sidebar__link" href="<%= ctx %>/empleados">
        <i class="fi fi-sr-users"></i><span>Empleados</span>
      </a>
      <% } %>
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/productos">
        <i class="fi fi-sr-box-open"></i><span>Productos</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/ventas">
        <i class="fi fi-sr-shopping-cart"></i><span>Carrito</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/historial"><i class="fi fi-sr-chart-histogram"></i><span>Ventas</span></a>
      <% if (puedeEditar) { %>
      <a class="sidebar__link" href="<%= ctx %>/gastos">
        <i class="fi fi-sr-receipt"></i><span>Gastos</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ganancias">
        <i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span>
      </a>
      <% } %>
      <% if (esSuperAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/emprendimientos"><i class="fi fi-sr-store-alt"></i><span>Emprendimientos</span></a>
      <% } %>
      <% if (esSuperAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/configuracion"><i class="fi fi-sr-settings"></i><span>Configuración</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/perfil">
        <i class="fi fi-sr-user"></i><span>Perfil</span>
      </a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout">
        <i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesion</span>
      </a>
    </nav>
  </aside>

  <main class="pagina-main">
    <div class="modulo-contenido nuevo-empleado-contenido">

      <!-- Preview imagen -->
      <div class="prod-form__preview" id="imgPreviewBloque">
        <div class="prod-form__preview-img" id="imgPreview">
          <i class="fi fi-sr-ice-cream"></i>
        </div>
        <span class="nuevo-empleado__nombre-preview" id="previewNombre">
          <%= vNombre.isEmpty() ? "Nuevo Producto" : vNombre %>
        </span>
      </div>

      <% if (error != null && !error.isBlank()) { %>
      <div class="modulo-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <form method="POST" action="<%= ctx %>/productos/nuevo"
            enctype="multipart/form-data" novalidate>

        <!-- Imagen -->
        <div class="nv-campo">
          <label class="nv-campo__label">Imagen <span class="nv-campo__opcional">(opcional, max 5 MB)</span></label>
          <label class="prod-form__upload-label" for="inputImagen">
            <i class="fi fi-sr-upload"></i> Elegir imagen
          </label>
          <input class="prod-form__upload-input" type="file" id="inputImagen"
                 name="imagen" accept="image/*">
        </div>

        <div class="nv-campo">
          <label class="nv-campo__label">Nombre *</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="text" name="nombre" maxlength="50"
                   id="inputNombre" value="<%= vNombre %>" required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <div class="nv-campo">
          <label class="nv-campo__label">Descripcion</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="text" name="descripcion" maxlength="100" value="<%= vDesc %>">
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <div class="nv-campo">
          <label class="nv-campo__label">Stock Actual *</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="number" name="stock" min="0" value="<%= vStock %>" required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <div class="nv-campo">
          <label class="nv-campo__label">Precio Unitario *</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="number" name="precio" min="0" step="0.01" value="<%= vPrecio %>" required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <div class="nv-campo">
          <label class="nv-campo__label">Fecha de Vencimiento *</label>
          <div class="nv-campo__input-wrapper">
            <input class="nv-campo__input" type="date" name="fechaVencimiento" value="<%= vFecha %>" required>
            <i class="fi fi-sr-edit nv-campo__icono-edit"></i>
          </div>
        </div>

        <div class="nv-campo">
          <label class="nv-campo__label">Categoria *</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <select class="nv-campo__input" name="idCategoria" required>
              <option value="">Seleccionar...</option>
              <% if (categorias != null) { for (String[] cat : categorias) { %>
              <option value="<%= cat[0] %>" <%= cat[0].equals(vCat) ? "selected" : "" %>><%= cat[1] %></option>
              <% } } %>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
          </div>
        </div>

        <div class="nv-campo">
          <label class="nv-campo__label">Unidad de Medida *</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <select class="nv-campo__input" name="idUnidad" required>
              <option value="">Seleccionar...</option>
              <% if (unidades != null) { for (String[] uni : unidades) { %>
              <option value="<%= uni[0] %>" <%= uni[0].equals(vUnidad) ? "selected" : "" %>><%= uni[1] %></option>
              <% } } %>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
          </div>
        </div>

        <div class="nv-campo">
          <label class="nv-campo__label">Estado *</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <select class="nv-campo__input" name="estado" required>
              <option value="Disponible" <%= "Disponible".equals(vEstado) ? "selected" : "" %>>Disponible</option>
              <option value="Agotado"    <%= "Agotado"   .equals(vEstado) ? "selected" : "" %>>Agotado</option>
              <option value="Inactivo"   <%= "Inactivo"  .equals(vEstado) ? "selected" : "" %>>Inactivo</option>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
          </div>
        </div>

        <!-- Emprendimiento (solo SuperAdmin) -->
        <% if (esSuperAdmin && emprendimientos != null && !emprendimientos.isEmpty()) { %>
        <div class="nv-campo">
          <label class="nv-campo__label">Emprendimiento</label>
          <div class="nv-campo__input-wrapper nv-campo__input-wrapper--select">
            <select class="nv-campo__input" name="idEmprendimiento" required>
              <option value="" disabled <%= vEmpId.isBlank() ? "selected" : "" %>>Selecciona emprendimiento</option>
              <% for (Emprendimiento emp : emprendimientos) { %>
              <option value="<%= emp.getId() %>" <%= String.valueOf(emp.getId()).equals(vEmpId) ? "selected" : "" %>>
                <%= emp.getNombre() %>
              </option>
              <% } %>
            </select>
            <i class="fi fi-sr-angle-down nv-campo__icono-edit"></i>
          </div>
        </div>
        <% } %>

        <div class="nv-botones">
          <button type="submit" class="nv-btn nv-btn--agregar">Guardar producto</button>
          <a href="<%= ctx %>/productos" class="nv-btn nv-btn--volver">Volver</a>
        </div>

      </form>
    </div>
  </main>

</body>
<script src="<%= ctx %>/assets/js/productos/nuevo.js" defer></script>
<script src="<%= ctx %>/assets/js/validaciones.js" defer></script>
</html>
