<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario,
                 com.dulce_gestion.models.Emprendimiento,
                 java.util.List" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx  = request.getContextPath();
    String rol  = sesionUsuario.getNombreRol();
    boolean esSuperAdmin = "SuperAdministrador".equals(rol);

    List<Emprendimiento> emprendimientos = (List<Emprendimiento>) request.getAttribute("emprendimientos");
    Emprendimiento miEmp = (Emprendimiento) request.getAttribute("miEmprendimiento");
    String error  = (String) request.getAttribute("error");
    String exito  = request.getParameter("exito");
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Emprendimientos | Dulce Gestión</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
  <style>
    .emp-grid { display:grid; gap:20px; }
    @media(min-width:768px){ .emp-grid { grid-template-columns: repeat(2, 1fr); } }
    @media(min-width:1100px){ .emp-grid { grid-template-columns: repeat(3, 1fr); } }

    .emp-card { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.07); overflow:hidden; display:flex; flex-direction:column; }
    .emp-card__header { background:var(--color-principal-morado); padding:18px 20px; display:flex; align-items:center; gap:12px; }
    .emp-card__icono { font-size:1.6rem; color:rgba(255,255,255,0.8); }
    .emp-card__nombre { color:white; font-weight:700; font-size:1rem; }
    .emp-card__nit { color:rgba(255,255,255,0.65); font-size:0.78rem; margin-top:2px; }
    .emp-card__body { padding:16px 20px; flex:1; display:flex; flex-direction:column; gap:8px; }
    .emp-card__fila { display:flex; align-items:center; gap:8px; font-size:0.85rem; color:#555; }
    .emp-card__fila i { color:var(--color-principal-morado); font-size:0.9rem; width:16px; flex-shrink:0; }
    .emp-card__footer { padding:12px 20px; border-top:1px solid #f0f0f0; display:flex; align-items:center; justify-content:space-between; }
    .badge-activo   { display:inline-block; padding:3px 10px; border-radius:20px; font-size:0.74rem; font-weight:700; background:rgba(46,125,50,0.1); color:#2e7d32; }
    .badge-inactivo { display:inline-block; padding:3px 10px; border-radius:20px; font-size:0.74rem; font-weight:700; background:rgba(198,40,40,0.1); color:#c62828; }
    .btn-acciones { display:flex; gap:8px; }
    .btn-mini { display:inline-flex; align-items:center; justify-content:center; width:32px; height:32px; border-radius:8px; border:none; cursor:pointer; font-size:0.9rem; text-decoration:none; transition:background 0.15s; }
    .btn-mini--edit { background:rgba(75,0,130,0.1); color:var(--color-principal-morado); }
    .btn-mini--edit:hover { background:rgba(75,0,130,0.2); }
    .btn-mini--off  { background:rgba(198,40,40,0.1); color:#c62828; }
    .btn-mini--off:hover  { background:rgba(198,40,40,0.2); }
    .btn-mini--on   { background:rgba(46,125,50,0.1); color:#2e7d32; }
    .btn-mini--on:hover   { background:rgba(46,125,50,0.2); }
    .emp-info { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.07); overflow:hidden; max-width:600px; margin:0 auto; }
    .emp-info__header { background:var(--color-principal-morado); padding:24px; text-align:center; }
    .emp-info__icon { font-size:3rem; color:rgba(255,255,255,0.8); }
    .emp-info__nombre { color:white; font-weight:800; font-size:1.2rem; margin-top:8px; }
    .emp-info__nit { color:rgba(255,255,255,0.65); font-size:0.85rem; }
    .emp-info__body { padding:20px 24px; display:flex; flex-direction:column; gap:12px; }
    .emp-info__fila { display:grid; grid-template-columns:140px 1fr; gap:8px; font-size:0.9rem; }
    .emp-info__label { color:#888; font-weight:600; font-size:0.78rem; text-transform:uppercase; letter-spacing:0.4px; }
    .emp-info__valor { color:var(--color-texto-oscuro); }
    .msg-exito { display:flex; align-items:center; gap:10px; padding:13px 18px; border-radius:8px; background:linear-gradient(135deg,#2e7d32,#388e3c); color:#fff; font-weight:600; margin-bottom:4px; }
    .msg-error { display:flex; align-items:center; gap:10px; padding:13px 18px; border-radius:8px; background:linear-gradient(135deg,#c62828,#e53935); color:#fff; font-weight:600; margin-bottom:4px; }
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
      <% if (esSuperAdmin || "Administrador".equals(rol)) { %>
      <a class="sidebar__link" href="<%= ctx %>/empleados"><i class="fi fi-sr-users"></i><span>Empleados</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/productos"><i class="fi fi-sr-box-open"></i><span>Productos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ventas"><i class="fi fi-sr-shopping-cart"></i><span>Carrito</span></a>
      <a class="sidebar__link" href="<%= ctx %>/historial"><i class="fi fi-sr-chart-histogram"></i><span>Ventas</span></a>
      <% if (esSuperAdmin || "Administrador".equals(rol)) { %>
      <a class="sidebar__link" href="<%= ctx %>/gastos"><i class="fi fi-sr-receipt"></i><span>Gastos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ganancias"><i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span></a>
      <% } %>
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/emprendimientos"><i class="fi fi-sr-store-alt"></i><span>Emprendimientos</span></a>
      <% if (esSuperAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/configuracion"><i class="fi fi-sr-settings"></i><span>Configuración</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/perfil"><i class="fi fi-sr-user"></i><span>Perfil</span></a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout"><i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesión</span></a>
    </nav>
  </aside>

  <main class="pagina-main">
    <div class="modulo-contenido">

      <div class="modulo-encabezado">
        <h1 class="modulo-titulo">
          <i class="fi fi-sr-store-alt"></i>
          Emprendimientos
        </h1>
        <% if (esSuperAdmin) { %>
        <a class="boton boton--primario boton--sm" href="<%= ctx %>/emprendimientos/nuevo">
          <i class="fi fi-sr-add"></i> Nuevo emprendimiento
        </a>
        <% } %>
      </div>

      <% if (exito != null) { %>
      <div class="msg-exito">
        <i class="fi fi-sr-check-circle"></i>
        <% if ("creado".equals(exito))    { %>Emprendimiento creado correctamente.
        <% } else if ("editado".equals(exito))   { %>Emprendimiento actualizado correctamente.
        <% } else if ("inactivado".equals(exito)){ %>Emprendimiento inactivado correctamente.
        <% } else                                { %>Emprendimiento activado correctamente.<% } %>
      </div>
      <% } %>
      <% if ("noexiste".equals(request.getParameter("error"))) { %>
      <div class="msg-error"><i class="fi fi-sr-triangle-warning"></i> Emprendimiento no encontrado.</div>
      <% } %>
      <% if (error != null) { %>
      <div class="msg-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <% if (esSuperAdmin && emprendimientos != null) { %>
      <!-- Vista SuperAdmin: grid de cards -->
      <% if (emprendimientos.isEmpty()) { %>
      <div style="text-align:center;padding:60px 20px;color:#ccc;">
        <i class="fi fi-sr-store-alt" style="font-size:3rem;display:block;margin-bottom:12px"></i>
        <p>No hay emprendimientos registrados aún.</p>
      </div>
      <% } else { %>
      <div class="emp-grid">
        <% for (Emprendimiento e : emprendimientos) { %>
        <div class="emp-card">
          <div class="emp-card__header">
            <i class="fi fi-sr-store-alt emp-card__icono"></i>
            <div>
              <div class="emp-card__nombre"><%= e.getNombre() %></div>
              <div class="emp-card__nit"><%= e.getNit() != null ? "NIT: " + e.getNit() : "Sin NIT" %></div>
            </div>
          </div>
          <div class="emp-card__body">
            <% if (e.getDireccion() != null) { %>
            <div class="emp-card__fila"><i class="fi fi-sr-marker"></i><span><%= e.getDireccion() %><% if (e.getCiudad() != null) { %>, <%= e.getCiudad() %><% } %></span></div>
            <% } %>
            <% if (e.getTelefono() != null) { %>
            <div class="emp-card__fila"><i class="fi fi-sr-phone-call"></i><span><%= e.getTelefono() %></span></div>
            <% } %>
            <% if (e.getCorreo() != null) { %>
            <div class="emp-card__fila"><i class="fi fi-sr-envelope"></i><span><%= e.getCorreo() %></span></div>
            <% } %>
            <div class="emp-card__fila"><i class="fi fi-sr-calendar"></i><span>Creado: <%= e.getFechaCreacion() %></span></div>
          </div>
          <div class="emp-card__footer">
            <span class="badge-<%= e.getEstado().toLowerCase() %>"><%= e.getEstado() %></span>
            <div class="btn-acciones">
              <a href="<%= ctx %>/emprendimientos/editar?id=<%= e.getId() %>" class="btn-mini btn-mini--edit" title="Editar"><i class="fi fi-sr-pencil"></i></a>
              <% if (e.isActivo()) { %>
              <form method="POST" action="<%= ctx %>/emprendimientos/editar" style="display:inline"
                    onsubmit="return confirm('¿Inactivar el emprendimiento <%= e.getNombre() %>?')">
                <input type="hidden" name="id" value="<%= e.getId() %>">
                <input type="hidden" name="accion" value="inactivar">
                <button type="submit" class="btn-mini btn-mini--off" title="Inactivar"><i class="fi fi-sr-ban"></i></button>
              </form>
              <% } else { %>
              <form method="POST" action="<%= ctx %>/emprendimientos/editar" style="display:inline"
                    onsubmit="return confirm('¿Activar el emprendimiento <%= e.getNombre() %>?')">
                <input type="hidden" name="id" value="<%= e.getId() %>">
                <input type="hidden" name="accion" value="activar">
                <button type="submit" class="btn-mini btn-mini--on" title="Activar"><i class="fi fi-sr-check"></i></button>
              </form>
              <% } %>
            </div>
          </div>
        </div>
        <% } %>
      </div>
      <% } %>

      <% } else if (miEmp != null) { %>
      <!-- Vista Admin/Empleado: info de su propio emprendimiento -->
      <div class="emp-info">
        <div class="emp-info__header">
          <div class="emp-info__icon"><i class="fi fi-sr-store-alt"></i></div>
          <div class="emp-info__nombre"><%= miEmp.getNombre() %></div>
          <% if (miEmp.getNit() != null) { %>
          <div class="emp-info__nit">NIT: <%= miEmp.getNit() %></div>
          <% } %>
        </div>
        <div class="emp-info__body">
          <% if (miEmp.getDireccion() != null) { %>
          <div class="emp-info__fila"><span class="emp-info__label">Dirección</span><span class="emp-info__valor"><%= miEmp.getDireccion() %></span></div>
          <% } %>
          <% if (miEmp.getCiudad() != null) { %>
          <div class="emp-info__fila"><span class="emp-info__label">Ciudad</span><span class="emp-info__valor"><%= miEmp.getCiudad() %></span></div>
          <% } %>
          <% if (miEmp.getTelefono() != null) { %>
          <div class="emp-info__fila"><span class="emp-info__label">Teléfono</span><span class="emp-info__valor"><%= miEmp.getTelefono() %></span></div>
          <% } %>
          <% if (miEmp.getCorreo() != null) { %>
          <div class="emp-info__fila"><span class="emp-info__label">Correo</span><span class="emp-info__valor"><%= miEmp.getCorreo() %></span></div>
          <% } %>
          <div class="emp-info__fila"><span class="emp-info__label">Estado</span><span class="badge-<%= miEmp.getEstado().toLowerCase() %>"><%= miEmp.getEstado() %></span></div>
          <div class="emp-info__fila"><span class="emp-info__label">Creado</span><span class="emp-info__valor"><%= miEmp.getFechaCreacion() %></span></div>
        </div>
      </div>
      <% } %>

    </div>
  </main>
</body>
</html>
