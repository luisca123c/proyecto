<%--
============================================================
JSP: dashboard.jsp (Administrador)
RUTA: /WEB-INF/jsp/admin/dashboard.jsp
PROPOSITO: Panel principal para rol Administrador
ACCESO: Requiere autenticación y rol Administrador
============================================================

Este JSP implementa el panel de control principal para usuarios
con rol Administrador con las siguientes características:

CARACTERÍSTICAS PRINCIPALES:
- Layout con sidebar colapsable y navegación principal
- Grid de tarjetas con acceso rápido a todos los módulos
- Diseño responsive adaptado para dispositivos móviles
- Navegación contextual según rol del usuario
- Acceso cerrado a configuración del sistema (solo SuperAdmin)

MÓDULOS ACCESIBLES PARA ADMINISTRADOR:
- Empleados: gestión de personal del emprendimiento
- Productos: catálogo y gestión de inventario
- Carrito: sistema de ventas activo
- Ventas: historial de transacciones
- Gastos: control de egresos
- Compras: gestión de insumos
- Ganancias: reportes financieros
- Perfil: datos personales del usuario

ESTRUCTURA DEL LAYOUT:
1. Header: branding del sistema y botón de logout
2. Sidebar: navegación principal con iconos
3. Main: grid de tarjetas de acceso rápido
4. Overlay: fondo oscuro para sidebar móvil

COMPONENTES CLAVE:
- #sidebar-toggle: checkbox para controlar estado del sidebar
- .sidebar__overlay: fondo para cerrar sidebar en móvil
- .dashboard-grid: rejilla de tarjetas de módulos
- .dashboard-card: tarjeta individual de cada módulo

SEGURIDAD:
- Acceso restringido por DashboardServlet según rol
- Enlaces relativos con contextPath para seguridad
- Sin acceso a módulos de configuración del sistema

ESTILOS Y RECURSOS:
- styles.css: estilos personalizados del sistema
- uicons-solid-rounded: iconos de Flaticon
--%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario" %>
<%
    Usuario usuario = (Usuario) session.getAttribute("usuario");
    String nombre = (usuario != null) ? usuario.getNombreCompleto() : "Administrador";
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

  <!-- Checkbox para controlar el estado del sidebar (colapsable/abierto) -->
  <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle" aria-label="Abrir menú">

  <!-- ===== HEADER - Barra superior con branding y acciones ===== -->
  <header class="main-header header-app">
    <div class="header-app__izquierda">
      <!-- Botón hamburguesa para toggle del sidebar -->
      <label for="sidebar-toggle" class="header-app__btn-menu" aria-label="Abrir menú">
        <i class="fi fi-sr-menu-burger"></i>
      </label>
      <!-- Logo y nombre del sistema -->
      <div class="header-app__marca">
        <img class="header-app__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo Dulce Gestión">
        <span class="header-app__titulo">Dulce Gestión</span>
      </div>
    </div>
    <div class="header-app__acciones">
      <!-- Botón de logout con icono y tooltip -->
      <a class="header-app__icono" href="<%= ctx %>/logout" aria-label="Cerrar sesión" title="Cerrar sesión">
        <i class="fi fi-sr-sign-out-alt"></i>
      </a>
    </div>
  </header>

  <!-- Overlay para cerrar sidebar en dispositivos móviles -->
  <label for="sidebar-toggle" class="sidebar__overlay" aria-label="Cerrar menú"></label>

  <!-- ===== SIDEBAR - Navegación principal ===== -->
  <aside class="main-sidebar sidebar">
    <div class="sidebar__top">
      <!-- Branding del sidebar -->
      <div class="sidebar__brand">
        <img class="sidebar__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo Dulce Gestión">
        <span class="sidebar__brand-text">Menú</span>
      </div>
      <!-- Botón para cerrar sidebar en móvil -->
      <label for="sidebar-toggle" class="sidebar__cerrar" aria-label="Cerrar menú">
        <i class="fi fi-sr-cross"></i>
      </label>
    </div>

    <!-- Navegación principal con enlaces a módulos -->
    <nav class="sidebar__nav">
      <!-- Inicio - página actual (activo) -->
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/dashboard">
        <i class="fi fi-sr-home"></i><span>Inicio</span>
      </a>
      <!-- Gestión de empleados -->
      <a class="sidebar__link" href="<%= ctx %>/empleados">
        <i class="fi fi-sr-users"></i><span>Empleados</span>
      </a>
      <!-- Gestión de productos -->
      <a class="sidebar__link" href="<%= ctx %>/productos">
        <i class="fi fi-sr-box-open"></i><span>Productos</span>
      </a>
      <!-- Carrito de ventas -->
      <a class="sidebar__link" href="<%= ctx %>/ventas">
        <i class="fi fi-sr-shopping-cart"></i><span>Carrito</span>
      </a>
      <!-- Historial de ventas -->
      <a class="sidebar__link" href="<%= ctx %>/historial"><i class="fi fi-sr-chart-histogram"></i><span>Ventas</span></a>
      <!-- Gestión de gastos -->
      <a class="sidebar__link" href="<%= ctx %>/gastos">
        <i class="fi fi-sr-receipt"></i><span>Gastos</span>
      </a>
      <!-- Gestión de compras -->
      <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
      <!-- Reportes de ganancias -->
      <a class="sidebar__link" href="<%= ctx %>/ganancias">
        <i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span>
      </a>
      <!-- Gestión de emprendimientos -->
      <a class="sidebar__link" href="<%= ctx %>/emprendimientos"><i class="fi fi-sr-store-alt"></i><span>Emprendimientos</span></a>
      <!-- Perfil personal -->
      <a class="sidebar__link" href="<%= ctx %>/perfil">
        <i class="fi fi-sr-user"></i><span>Perfil</span>
      </a>
      <!-- Separador visual -->
      <div class="sidebar__separador"></div>
      <!-- Cerrar sesión -->
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout">
        <i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesión</span>
      </a>
    </nav>
  </aside>

  <!-- ===== MAIN - Contenido principal del dashboard ===== -->
  <main class="pagina-main">
    <div class="dashboard-contenido">
      <!-- Grid de tarjetas de acceso rápido a los módulos del sistema -->
      <nav class="dashboard-grid" aria-label="Módulos del sistema">

        <!-- Tarjeta: Gestión de empleados -->
        <a class="dashboard-card" href="<%= ctx %>/empleados">
          <i class="dashboard-card__icono fi fi-sr-users"></i>
          <span class="dashboard-card__texto">Empleados</span>
        </a>

        <!-- Tarjeta: Gestión de productos -->
        <a class="dashboard-card" href="<%= ctx %>/productos">
          <i class="dashboard-card__icono fi fi-sr-box-open"></i>
          <span class="dashboard-card__texto">Productos</span>
        </a>

        <!-- Tarjeta: Carrito de ventas activo -->
        <a class="dashboard-card" href="<%= ctx %>/ventas">
          <i class="dashboard-card__icono fi fi-sr-shopping-cart"></i>
          <span class="dashboard-card__texto">Carrito</span>
        </a>

        <!-- Tarjeta: Historial de ventas realizadas -->
        <a class="dashboard-card" href="<%= ctx %>/historial">
          <i class="dashboard-card__icono fi fi-sr-chart-histogram"></i>
          <span class="dashboard-card__texto">Ventas</span>
        </a>

        <!-- Tarjeta: Control de gastos -->
        <a class="dashboard-card" href="<%= ctx %>/gastos">
          <i class="dashboard-card__icono fi fi-sr-receipt"></i>
          <span class="dashboard-card__texto">Gastos</span>
        </a>

        <!-- Tarjeta: Gestión de compras de insumos -->
        <a class="dashboard-card" href="<%= ctx %>/compras">
          <i class="dashboard-card__icono fi fi-sr-shop"></i>
          <span class="dashboard-card__texto">Compras</span>
        </a>

        <!-- Tarjeta: Reportes de ganancias -->
        <a class="dashboard-card" href="<%= ctx %>/ganancias">
          <i class="dashboard-card__icono fi fi-sr-chart-line-up"></i>
          <span class="dashboard-card__texto">Ganancias</span>
        </a>

        <!-- Tarjeta: Perfil personal del usuario -->
        <a class="dashboard-card" href="<%= ctx %>/perfil">
          <i class="dashboard-card__icono fi fi-sr-user"></i>
          <span class="dashboard-card__texto">Perfil</span>
        </a>

      </nav>
    </div>
  </main>

</body>
</html>
