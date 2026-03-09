<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario,
                 com.dulce_gestion.dao.GananciasDAO.ResumenPeriodo,
                 com.dulce_gestion.dao.GananciasDAO.FilaVenta,
                 com.dulce_gestion.dao.GananciasDAO.FilaGasto,
                 java.util.List,
                 java.math.BigDecimal" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    boolean esSuperAdmin  = "SuperAdministrador".equals(sesionUsuario.getNombreRol());
    boolean esAdmin       = "Administrador".equals(sesionUsuario.getNombreRol());
    Boolean _tmp          = (Boolean) request.getAttribute("esAdminOSuper");
    boolean esAdminOSuper = (_tmp != null) ? _tmp : false;

    ResumenPeriodo r    = (ResumenPeriodo) request.getAttribute("resumen");
    List<String[]> meses = (List<String[]>) request.getAttribute("meses");
    String periodo      = (String) request.getAttribute("periodo");
    String error        = (String) request.getAttribute("error");
    if (periodo == null) periodo = "semana";

    boolean esMesEspecifico = periodo.matches("\\d{4}-\\d{2}");
    String  mesSel          = esMesEspecifico ? periodo : "";
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Ganancias | Dulce Gestion</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
  <style>
    .gan-wrapper { max-width: 960px; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; }

    /* Encabezado */
    .gan-header { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; }
    .gan-titulo { font-size:1.3rem; font-weight:700; color:var(--color-texto-oscuro); display:flex; align-items:center; gap:10px; }
    .gan-titulo i { color:var(--color-principal-morado); }

    /* Selector de período */
    .gan-filtro {
      background: white;
      border-radius: var(--radius-md);
      box-shadow: 0 2px 8px rgba(0,0,0,0.07);
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }
    .gan-filtro__label { font-size:0.82rem; font-weight:700; color:#888; text-transform:uppercase; letter-spacing:0.5px; white-space:nowrap; }
    .gan-tabs { display:flex; gap:8px; flex-wrap:wrap; }
    .gan-tab {
      padding: 8px 18px;
      border-radius: 20px;
      border: 2px solid #e0e0e0;
      background: white;
      color: #888;
      font-weight: 600;
      font-size: 0.85rem;
      cursor: pointer;
      text-decoration: none;
      transition: all 0.15s;
      white-space: nowrap;
    }
    .gan-tab:hover { border-color: var(--color-principal-morado); color: var(--color-principal-morado); }
    .gan-tab--activo { background: var(--color-principal-morado); border-color: var(--color-principal-morado); color: white; }

    .gan-mes-select {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-left: auto;
    }
    .gan-mes-select select {
      padding: 8px 14px;
      border: 2px solid #e0e0e0;
      border-radius: 20px;
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--color-texto-oscuro);
      background: white;
      cursor: pointer;
      appearance: none;
      -webkit-appearance: none;
      padding-right: 32px;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%23888'/%3E%3C/svg%3E");
      background-repeat: no-repeat;
      background-position: right 12px center;
    }
    .gan-mes-select select:focus { outline:none; border-color: var(--color-principal-morado); }
    .gan-mes-select select.activo { border-color: var(--color-principal-morado); color: var(--color-principal-morado); }

    /* Etiqueta período activo */
    .gan-periodo-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.82rem;
      color: #888;
      background: #f5f5f5;
      padding: 6px 14px;
      border-radius: 20px;
    }
    .gan-periodo-label strong { color: var(--color-principal-morado); }

    /* Tarjetas */
    .gan-cards { display:grid; grid-template-columns:repeat(3,1fr); gap:16px; }
    @media(max-width:640px){ .gan-cards { grid-template-columns:1fr; } }
    .gan-card { background:white; border-radius:var(--radius-md); padding:20px 22px; box-shadow:0 2px 8px rgba(0,0,0,0.07); display:flex; flex-direction:column; gap:6px; }
    .gan-card__label { font-size:0.76rem; font-weight:700; text-transform:uppercase; letter-spacing:0.5px; color:#aaa; }
    .gan-card__valor { font-size:1.55rem; font-weight:800; }
    .gan-card__icono { font-size:1.3rem; margin-bottom:2px; }
    .c-ventas  .gan-card__icono, .c-ventas  .gan-card__valor { color:var(--color-principal-morado); }
    .c-gastos  .gan-card__icono, .c-gastos  .gan-card__valor { color:var(--color-danger); }
    .c-positivo .gan-card__icono, .c-positivo .gan-card__valor { color:var(--color-success); }
    .c-negativo .gan-card__icono, .c-negativo .gan-card__valor { color:var(--color-danger); }

    /* Tablas */
    .gan-seccion { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.07); overflow:hidden; }
    .gan-seccion__header { padding:12px 18px; display:flex; align-items:center; gap:10px; }
    .gan-seccion__header--ventas { background:var(--color-principal-morado); }
    .gan-seccion__header--gastos { background:var(--color-danger); }
    .gan-seccion__titulo { color:white; font-weight:700; font-size:0.92rem; }
    .gan-seccion__badge  { background:rgba(255,255,255,0.25); color:white; font-size:0.75rem; font-weight:700; padding:2px 8px; border-radius:10px; margin-left:auto; }

    .gan-tabla { width:100%; border-collapse:collapse; }
    .gan-tabla th { padding:10px 16px; font-size:0.76rem; font-weight:700; text-transform:uppercase; letter-spacing:0.4px; color:#888; background:#fafafa; border-bottom:1px solid #f0f0f0; text-align:left; }
    .gan-tabla td { padding:11px 16px; font-size:0.88rem; color:var(--color-texto-oscuro); border-bottom:1px solid #f5f5f5; }
    .gan-tabla tr:last-child td { border-bottom:none; }
    .gan-tabla tr:hover td { background:#faf8ff; }
    .td-id   { color:#ccc; font-size:0.76rem; width:40px; }
    .td-r    { text-align:right; font-weight:700; }
    .td-v    { color:var(--color-principal-morado); }
    .td-g    { color:var(--color-danger); }
    .gan-tabla tfoot td { background:#fafafa; font-weight:700; border-top:2px solid #f0f0f0; }
    .badge-quien { display:inline-block; background:rgba(75,0,130,0.08); color:var(--color-principal-morado); font-size:0.76rem; font-weight:600; padding:3px 10px; border-radius:20px; }
    .badge-mp    { display:inline-block; background:#f5f5f5; color:#555; font-size:0.76rem; font-weight:600; padding:3px 10px; border-radius:20px; }

    .gan-vacio { padding:32px; text-align:center; color:#ccc; }
    .gan-vacio i { font-size:2.2rem; display:block; margin-bottom:8px; }

    .msg-error { display:flex; align-items:center; gap:10px; padding:12px 16px; border-radius:8px; background:rgba(169,50,38,0.1); border-left:4px solid var(--color-danger); color:var(--color-danger); font-weight:600; }
  </style>
</head>
<body class="layout-app">

  <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle">

  <header class="main-header header-app">
    <div class="header-app__izquierda">
      <label for="sidebar-toggle" class="header-app__btn-menu"><i class="fi fi-sr-menu-burger"></i></label>
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
      <a class="sidebar__link" href="<%= ctx %>/dashboard"><i class="fi fi-sr-home"></i><span>Inicio</span></a>
      <% if (esSuperAdmin || esAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/empleados"><i class="fi fi-sr-users"></i><span>Empleados</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/productos"><i class="fi fi-sr-box-open"></i><span>Productos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ventas"><i class="fi fi-sr-shopping-cart"></i><span>Ventas</span></a>
      <% if (esSuperAdmin || esAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/gastos"><i class="fi fi-sr-receipt"></i><span>Gastos</span></a>
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/ganancias"><i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/perfil"><i class="fi fi-sr-user"></i><span>Perfil</span></a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout"><i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesion</span></a>
    </nav>
  </aside>

  <main class="pagina-main">
    <div class="gan-wrapper">

      <div class="gan-header">
        <h1 class="gan-titulo"><i class="fi fi-sr-chart-line-up"></i> Ganancias</h1>
        <% if (r != null) { %>
        <span class="gan-periodo-label">
          <i class="fi fi-sr-calendar"></i>
          <strong><%= r.labelPeriodo %></strong>
        </span>
        <% } %>
      </div>

      <% if (error != null && !error.isBlank()) { %>
      <div class="msg-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <!-- Selector de período -->
      <div class="gan-filtro">
        <span class="gan-filtro__label">Ver:</span>
        <div class="gan-tabs">
          <a href="<%= ctx %>/ganancias?periodo=semana"
             class="gan-tab <%= "semana".equals(periodo) ? "gan-tab--activo" : "" %>">
            Esta semana
          </a>
          <a href="<%= ctx %>/ganancias?periodo=mes"
             class="gan-tab <%= "mes".equals(periodo) ? "gan-tab--activo" : "" %>">
            Este mes
          </a>
        </div>

        <!-- Selector mes específico -->
        <div class="gan-mes-select">
          <i class="fi fi-sr-calendar" style="color:#aaa;font-size:0.9rem"></i>
          <select onchange="if(this.value) window.location='<%= ctx %>/ganancias?periodo='+this.value"
                  class="<%= esMesEspecifico ? "activo" : "" %>">
            <option value="">Elegir mes...</option>
            <% if (meses != null) { for (String[] m : meses) { %>
            <option value="<%= m[0] %>" <%= m[0].equals(mesSel) ? "selected" : "" %>><%= m[1] %></option>
            <% } } %>
          </select>
        </div>
      </div>

      <% if (r != null) { %>

      <!-- Tarjetas resumen -->
      <div class="gan-cards">
        <div class="gan-card c-ventas">
          <i class="fi fi-sr-shopping-cart gan-card__icono"></i>
          <div class="gan-card__label">Ventas</div>
          <div class="gan-card__valor">$<%= String.format("%,.0f", r.totalVentas) %></div>
        </div>
        <% if (esAdminOSuper) { %>
        <div class="gan-card c-gastos">
          <i class="fi fi-sr-receipt gan-card__icono"></i>
          <div class="gan-card__label">Gastos</div>
          <div class="gan-card__valor">$<%= String.format("%,.0f", r.totalGastos) %></div>
        </div>
        <div class="gan-card <%= r.ganancia.compareTo(BigDecimal.ZERO) >= 0 ? "c-positivo" : "c-negativo" %>">
          <i class="fi fi-sr-coins gan-card__icono"></i>
          <div class="gan-card__label">Ganancia neta</div>
          <div class="gan-card__valor">$<%= String.format("%,.0f", r.ganancia) %></div>
        </div>
        <% } %>
      </div>

      <!-- Tabla ventas -->
      <div class="gan-seccion">
        <div class="gan-seccion__header gan-seccion__header--ventas">
          <i class="fi fi-sr-shopping-cart" style="color:white"></i>
          <span class="gan-seccion__titulo">
            <%= esAdminOSuper ? "Ventas del período" : "Mis ventas del período" %>
          </span>
          <span class="gan-seccion__badge"><%= r.ventas.size() %></span>
        </div>
        <% if (r.ventas.isEmpty()) { %>
        <div class="gan-vacio"><i class="fi fi-sr-shopping-cart"></i>No hay ventas en este período.</div>
        <% } else { %>
        <table class="gan-tabla">
          <thead>
            <tr>
              <th>#</th>
              <th>Fecha</th>
              <% if (esAdminOSuper) { %><th>Realizada por</th><% } %>
              <th>Metodo pago</th>
              <th style="text-align:right">Total</th>
            </tr>
          </thead>
          <tbody>
            <% for (FilaVenta v : r.ventas) { %>
            <tr>
              <td class="td-id">#<%= v.id %></td>
              <td><%= v.fecha %></td>
              <% if (esAdminOSuper) { %>
              <td><span class="badge-quien"><%= v.realizadaPor %></span></td>
              <% } %>
              <td><span class="badge-mp"><%= v.metodoPago %></span></td>
              <td class="td-r td-v">$<%= String.format("%,.0f", v.total) %></td>
            </tr>
            <% } %>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="<%= esAdminOSuper ? 4 : 3 %>" style="padding:10px 16px;color:#666;">Total ventas</td>
              <td class="td-r td-v" style="padding:10px 16px;">$<%= String.format("%,.0f", r.totalVentas) %></td>
            </tr>
          </tfoot>
        </table>
        <% } %>
      </div>

      <!-- Tabla gastos (solo admins) -->
      <% if (esAdminOSuper) { %>
      <div class="gan-seccion">
        <div class="gan-seccion__header gan-seccion__header--gastos">
          <i class="fi fi-sr-receipt" style="color:white"></i>
          <span class="gan-seccion__titulo">Gastos del período</span>
          <span class="gan-seccion__badge"><%= r.gastos.size() %></span>
        </div>
        <% if (r.gastos.isEmpty()) { %>
        <div class="gan-vacio"><i class="fi fi-sr-receipt"></i>No hay gastos en este período.</div>
        <% } else { %>
        <table class="gan-tabla">
          <thead>
            <tr>
              <th>#</th><th>Fecha</th><th>Descripcion</th><th>Metodo pago</th>
              <th style="text-align:right">Total</th>
            </tr>
          </thead>
          <tbody>
            <% for (FilaGasto g : r.gastos) { %>
            <tr>
              <td class="td-id">#<%= g.id %></td>
              <td><%= g.fecha %></td>
              <td><%= g.descripcion != null ? g.descripcion : "-" %></td>
              <td><span class="badge-mp"><%= g.metodoPago %></span></td>
              <td class="td-r td-g">$<%= String.format("%,.0f", g.total) %></td>
            </tr>
            <% } %>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="4" style="padding:10px 16px;color:#666;">Total gastos</td>
              <td class="td-r td-g" style="padding:10px 16px;">$<%= String.format("%,.0f", r.totalGastos) %></td>
            </tr>
          </tfoot>
        </table>
        <% } %>
      </div>
      <% } %>

      <% } %>

    </div>
  </main>
</body>
</html>