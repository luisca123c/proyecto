<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    String nombre = (usuario != null) ? usuario.getNombreCompleto() : "SuperAdministrador";
    String ctx = request.getContextPath();
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Página Principal | Dulce Gestión</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
</head>

<body class="layout-app">

  <!-- Toggle hamburguesa (solo CSS, sin JS) -->
  <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle" aria-label="Abrir menú">

  <!-- ===== HEADER ===== -->
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

  <!-- Overlay para cerrar sidebar en móvil -->
  <label for="sidebar-toggle" class="sidebar__overlay" aria-label="Cerrar menú"></label>

  <!-- ===== SIDEBAR ===== -->
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
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/dashboard">
        <i class="fi fi-sr-home"></i><span>Inicio</span>
      </a>
      <a class="sidebar__link" href="<%= ctx %>/empleados">
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

  <!-- ===== MAIN ===== -->
  <main class="pagina-main">
    <div class="dashboard-contenido">
      <nav class="dashboard-grid" aria-label="Módulos del sistema">

        <a class="dashboard-card" href="<%= ctx %>/empleados">
          <i class="dashboard-card__icono fi fi-sr-users"></i>
          <span class="dashboard-card__texto">Empleados</span>
        </a>

        <a class="dashboard-card" href="<%= ctx %>/productos">
          <i class="dashboard-card__icono fi fi-sr-box-open"></i>
          <span class="dashboard-card__texto">Productos</span>
        </a>

        <a class="dashboard-card" href="<%= ctx %>/ventas">
          <i class="dashboard-card__icono fi fi-sr-shopping-cart"></i>
          <span class="dashboard-card__texto">Carrito</span>
        </a>

        <a class="dashboard-card" href="<%= ctx %>/historial">
          <i class="dashboard-card__icono fi fi-sr-chart-histogram"></i>
          <span class="dashboard-card__texto">Ventas</span>
        </a>

        <a class="dashboard-card" href="<%= ctx %>/gastos">
          <i class="dashboard-card__icono fi fi-sr-receipt"></i>
          <span class="dashboard-card__texto">Gastos</span>
        </a>

        <a class="dashboard-card" href="<%= ctx %>/compras">
          <i class="dashboard-card__icono fi fi-sr-shop"></i>
          <span class="dashboard-card__texto">Compras</span>
        </a>

        <a class="dashboard-card" href="<%= ctx %>/ganancias">
          <i class="dashboard-card__icono fi fi-sr-chart-line-up"></i>
          <span class="dashboard-card__texto">Ganancias</span>
        </a>

        <a class="dashboard-card" href="<%= ctx %>/perfil">
          <i class="dashboard-card__icono fi fi-sr-user"></i>
          <span class="dashboard-card__texto">Perfil</span>
        </a>

      </nav>
    </div>
  </main>

</body>
</html>
