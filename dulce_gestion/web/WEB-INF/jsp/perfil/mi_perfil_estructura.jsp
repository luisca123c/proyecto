<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Producto, com.dulce_gestion.models.Usuario, java.util.List" %>
<%
    Usuario sesionUsuario  = (Usuario) session.getAttribute("usuario");
    String  ctx            = request.getContextPath();
    String  rolSolicitante = (String) request.getAttribute("rolSolicitante");
    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    String  errorProductos = (String) request.getAttribute("errorProductos");

    boolean puedeEditar    = "SuperAdministrador".equals(rolSolicitante)
                          || "Administrador".equals(rolSolicitante);
    boolean esSuperAdmin   = "SuperAdministrador".equals(rolSolicitante);

    boolean exitoCreado    = "creado".equals(request.getParameter("exito"));
    boolean exitoEditado   = "editado".equals(request.getParameter("exito"));
    boolean exitoEliminado = "eliminado".equals(request.getParameter("exito"));
    String  errorParam     = request.getParameter("error");
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Productos | Dulce Gestion</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/pages/mensajes.css">
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
</head>
<body class="layout-app">

  <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle" aria-label="Abrir menu">

  <!-- HEADER -->
  <header class="main-header header-app">
    <div class="header-app__izquierda">
      <label for="sidebar-toggle" class="header-app__btn-menu" aria-label="Abrir menu">
        <i class="fi fi-sr-menu-burger"></i>
      </label>
      <div class="header-app__marca">
        <img class="header-app__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo Dulce Gestion">
        <span class="header-app__titulo">Dulce Gestion</span>
      </div>
    </div>
    <div class="header-app__acciones">
      <a class="header-app__icono" href="<%= ctx %>/logout" title="Cerrar sesion">
        <i class="fi fi-sr-sign-out-alt"></i>
      </a>
    </div>
  </header>

  <label for="sidebar-toggle" class="sidebar__overlay" aria-label="Cerrar menu"></label>

  <!-- SIDEBAR -->
  <aside class="main-sidebar sidebar">
    <div class="sidebar__top">
      <div class="sidebar__brand">
        <img class="sidebar__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo">
        <span class="sidebar__brand-text">Menu</span>
      </div>
      <label for="sidebar-toggle" class="sidebar__cerrar">
        <i class="fi fi-sr-cross"></i>
      </label>
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

  <!-- MAIN -->
  <main class="pagina-main">
    <div class="modulo-contenido productos-contenido">

      <!-- Mensajes -->
      <% if (exitoCreado) { %>
      <div class="alerta alerta-exito"><i class="fi fi-sr-check-circle"></i> <span>Producto creado correctamente</span></div>
      <% } %>
      <% if (exitoEditado) { %>
      <div class="modulo-exito"><i class="fi fi-sr-check"></i> Producto actualizado correctamente.</div>
      <% } %>
      <% if (exitoEliminado) { %>
      <div class="alerta alerta-exito"><i class="fi fi-sr-check-circle"></i> <span>Producto eliminado correctamente</span></div>
      <% } %>
      <% if ("eliminacion".equals(errorParam)) { %>
      <div class="modulo-error"><i class="fi fi-sr-triangle-warning"></i> No se pudo eliminar. El producto tiene ventas asociadas.</div>
      <% } %>
      <% if (errorProductos != null) { %>
      <div class="modulo-error"><i class="fi fi-sr-triangle-warning"></i> <%= errorProductos %></div>
      <% } %>

      <!-- Boton agregar (solo admin) -->
      <% if (puedeEditar) { %>
      <div class="productos-acciones">
        <a class="boton boton--success boton--sm" href="<%= ctx %>/productos/nuevo">
          <i class="fi fi-sr-plus"></i> Agregar Producto
        </a>
      </div>
      <% } %>

      <!-- Grid de cards -->
      <% if (productos == null || productos.isEmpty()) { %>
      <div class="modulo-vacio">
        <i class="fi fi-sr-box-open modulo-vacio__icono"></i>
        <p>No hay productos registrados aun.</p>
      </div>
      <% } else { %>
      <div class="productos-grid">
        <% for (Producto prod : productos) { %>
        <div class="pcard">

          <!-- Imagen o placeholder -->
          <div class="pcard__imagen">
            <% if (prod.getPathImagen() != null && !prod.getPathImagen().isBlank()) { %>
            <img src="<%= ctx %>/<%= prod.getPathImagen() %>"
                 alt="<%= prod.getAltImagen() != null ? prod.getAltImagen() : prod.getNombre() %>">
            <% } else { %>
            <div class="pcard__placeholder">
              <i class="fi fi-sr-ice-cream"></i>
            </div>
            <% } %>

            <!-- Acciones encima de la imagen (solo admin) -->
            <% if (puedeEditar) { %>
            <div class="pcard__acciones">
              <a class="pcard__btn pcard__btn--editar"
                 href="<%= ctx %>/productos/editar?id=<%= prod.getId() %>"
                 title="Editar">
                <i class="fi fi-sr-edit"></i>
              </a>
              <form method="POST" action="<%= ctx %>/productos/eliminar" style="display:contents"
                    onsubmit="return confirm('Eliminar <%= prod.getNombre() %>?')">
                <input type="hidden" name="id" value="<%= prod.getId() %>">
                <button type="submit" class="pcard__btn pcard__btn--eliminar" title="Eliminar">
                  <i class="fi fi-sr-trash"></i>
                </button>
              </form>
            </div>
            <% } %>
          </div>

          <!-- Info -->
          <div class="pcard__info">
            <span class="pcard__nombre"><%= prod.getNombre() %></span>
            <span class="pcard__detalle">
              stock = <%= prod.getStockActual() %> precio = $<%= prod.getPrecioUnitario() %>
            </span>
          </div>

        </div>
        <% } %>
      </div>
      <% } %>

    </div>
  </main>

</body>
</html>
