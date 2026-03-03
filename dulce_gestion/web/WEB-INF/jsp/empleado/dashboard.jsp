<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    String nombre = (usuario != null) ? usuario.getNombreCompleto() : "Empleado";
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

<body>
<div class="pagina-dashboard">

  <!-- HEADER -->
  <header class="dashboard-header">
    <div class="dashboard-header__marca">
      <img class="dashboard-header__logo"
           src="<%= ctx %>/assets/images/Logo.png"
           alt="Logo Dulce Gestión">
      <span class="dashboard-header__titulo">Dulce Gestión</span>
    </div>
    <a class="dashboard-header__salir" href="<%= ctx %>/logout">
      <i class="fi fi-sr-sign-out-alt"></i>
      <span>Salir</span>
    </a>
  </header>

  <!-- CONTENIDO -->
  <main class="dashboard-contenido">
    <nav class="dashboard-grid" aria-label="Módulos del sistema">

      <%-- Empleado: solo VER_PRODUCTOS, CREAR_VENTA, VER_VENTAS --%>

      <a class="dashboard-card" href="<%= ctx %>/productos">
        <i class="dashboard-card__icono fi fi-sr-box-open"></i>
        <span class="dashboard-card__texto">Productos</span>
      </a>

      <a class="dashboard-card" href="<%= ctx %>/ventas">
        <i class="dashboard-card__icono fi fi-sr-shopping-cart"></i>
        <span class="dashboard-card__texto">Ventas</span>
      </a>

      <a class="dashboard-card dashboard-card--salir" href="<%= ctx %>/logout">
        <i class="dashboard-card__icono fi fi-sr-sign-out-alt"></i>
        <span class="dashboard-card__texto">Salir</span>
      </a>

    </nav>
  </main>

  <!-- FOOTER -->
  <footer class="dashboard-footer">
    <p class="dashboard-footer__texto">
      &copy; 2025 Sistema de Gestión. Todos los derechos reservados.
    </p>
  </footer>

</div>
</body>
</html>
