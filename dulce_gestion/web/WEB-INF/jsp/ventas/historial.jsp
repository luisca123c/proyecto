<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario,
                 com.dulce_gestion.dao.HistorialDAO.FilaVenta,
                 java.util.List, java.math.BigDecimal" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    boolean esSuperAdmin  = "SuperAdministrador".equals(sesionUsuario.getNombreRol());
    boolean esAdmin       = "Administrador".equals(sesionUsuario.getNombreRol());
    boolean esAdminOSuper = esSuperAdmin || esAdmin;

    List<FilaVenta> ventas = (List<FilaVenta>) request.getAttribute("ventas");
    String error = (String) request.getAttribute("error");

    BigDecimal totalGeneral = BigDecimal.ZERO;
    if (ventas != null) {
        for (FilaVenta v : ventas) {
            if (v.total != null) totalGeneral = totalGeneral.add(v.total);
        }
    }
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Ventas | Dulce Gestión</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
  <style>
    .hist-wrapper { max-width: 980px; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; }
    .modulo-encabezado { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; }
    .modulo-titulo { font-size:1.3rem; font-weight:700; color:var(--color-texto-oscuro); display:flex; align-items:center; gap:10px; }
    .modulo-titulo i { color:var(--color-principal-morado); }

    /* Tarjeta total */
    .hist-total { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.07); padding:18px 22px; display:flex; align-items:center; justify-content:space-between; }
    .hist-total__label { font-size:0.82rem; font-weight:700; text-transform:uppercase; letter-spacing:0.5px; color:#aaa; }
    .hist-total__valor { font-size:1.6rem; font-weight:800; color:#2e7d32; }

    /* Sección */
    .hist-seccion { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.07); overflow:hidden; }
    .hist-seccion__header { background:var(--color-principal-morado); padding:12px 18px; display:flex; align-items:center; gap:10px; }
    .hist-seccion__titulo { color:white; font-weight:700; font-size:0.92rem; }
    .hist-seccion__badge { background:rgba(255,255,255,0.25); color:white; font-size:0.75rem; font-weight:700; padding:2px 8px; border-radius:10px; margin-left:auto; }

    /* Cabecera de columnas */
    .hist-cols { display:grid; padding:8px 18px; background:#fafafa; border-bottom:1px solid #f0f0f0; font-size:0.74rem; font-weight:700; text-transform:uppercase; color:#aaa; letter-spacing:0.4px; }
    .hist-cols.con-quien { grid-template-columns: 50px 1fr 160px 130px 110px 90px; }
    .hist-cols.sin-quien { grid-template-columns: 50px 1fr 130px 110px 90px; }

    /* Fila de venta */
    .hist-fila { border-bottom:1px solid #f5f5f5; }
    .hist-fila:last-child { border-bottom:none; }
    .hist-fila__datos { display:grid; align-items:center; padding:12px 18px; cursor:pointer; transition:background 0.15s; }
    .hist-fila__datos:hover { background:#faf8ff; }
    .hist-fila__datos.con-quien { grid-template-columns: 50px 1fr 160px 130px 110px 90px; }
    .hist-fila__datos.sin-quien { grid-template-columns: 50px 1fr 130px 110px 90px; }

    .td-id    { color:#ccc; font-size:0.76rem; }
    .td-total { text-align:right; font-weight:700; color:#2e7d32; font-size:0.88rem; }
    .td-fecha { font-size:0.87rem; color:var(--color-texto-oscuro); }
    .td-accion { text-align:right; }
    .badge-mp   { display:inline-block; padding:3px 10px; border-radius:20px; font-size:0.75rem; font-weight:600; background:rgba(75,0,130,0.08); color:var(--color-principal-morado); }
    .badge-quien { display:inline-block; padding:3px 10px; border-radius:20px; font-size:0.75rem; font-weight:600; background:rgba(46,125,50,0.1); color:#2e7d32; }

    .btn-ver { background:none; border:none; cursor:pointer; color:var(--color-principal-morado); font-size:0.82rem; font-weight:600; display:inline-flex; align-items:center; gap:5px; padding:4px 8px; border-radius:6px; transition:background 0.15s; }
    .btn-ver:hover { background:rgba(75,0,130,0.08); }

    /* Detalle expandible */
    .hist-fila__detalle { overflow:hidden; max-height:0; transition:max-height 0.3s ease; background:#f9f7ff; }
    .hist-fila__detalle.abierto { max-height:600px; border-top:1px dashed #e0d0f0; }
    .det-pad { padding:14px 24px 18px; }

    .det-tabla { width:100%; border-collapse:collapse; font-size:0.83rem; }
    .det-tabla th { color:#aaa; font-weight:700; text-transform:uppercase; font-size:0.74rem; padding:6px 10px; text-align:left; border-bottom:1px solid #eee; }
    .det-tabla td { padding:8px 10px; color:var(--color-texto-oscuro); border-bottom:1px solid #f5f5f5; }
    .det-tabla td:last-child { text-align:right; font-weight:700; color:#2e7d32; }
    .det-tabla tr:last-child td { border-bottom:none; }
    .det-tabla tfoot td { background:#f0f0f0; font-weight:700; border-top:2px solid #e0e0e0; }

    /* Pie total */
    .hist-pie { padding:10px 18px; background:#fafafa; border-top:2px solid #f0f0f0; display:flex; justify-content:space-between; font-size:0.88rem; font-weight:700; }
    .hist-pie span:last-child { color:#2e7d32; }

    /* Vacío / error */
    .hist-vacio { padding:40px; text-align:center; color:#ccc; }
    .hist-vacio i { font-size:2.5rem; display:block; margin-bottom:10px; }
    .msg-error { display:flex; align-items:center; gap:10px; padding:14px 18px; border-radius:8px; background:linear-gradient(135deg,#c62828 0%,#e53935 100%); color:#fff; font-weight:600; }
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
      <% if (esSuperAdmin || esAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/empleados"><i class="fi fi-sr-users"></i><span>Empleados</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/productos"><i class="fi fi-sr-box-open"></i><span>Productos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ventas"><i class="fi fi-sr-shopping-cart"></i><span>Carrito</span></a>
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/historial"><i class="fi fi-sr-chart-histogram"></i><span>Ventas</span></a>
      <% if (esSuperAdmin || esAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/gastos"><i class="fi fi-sr-receipt"></i><span>Gastos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ganancias"><i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span></a>
      <% } %>
      <% if (esSuperAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/emprendimientos"><i class="fi fi-sr-store-alt"></i><span>Emprendimientos</span></a>
      <% } %>
      <% if (esSuperAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/configuracion"><i class="fi fi-sr-settings"></i><span>Configuración</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/perfil"><i class="fi fi-sr-user"></i><span>Perfil</span></a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout"><i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesión</span></a>
    </nav>
  </aside>

  <main class="pagina-main">
    <div class="hist-wrapper">

      <div class="modulo-encabezado">
        <h1 class="modulo-titulo">
          <i class="fi fi-sr-chart-histogram"></i>
          <%= esAdminOSuper ? "Historial de Ventas" : "Mis Ventas" %>
        </h1>
      </div>

      <% if (error != null) { %>
      <div class="msg-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <!-- Tarjeta total general -->
      <div class="hist-total">
        <div>
          <div class="hist-total__label"><%= esAdminOSuper ? "Total vendido" : "Total de mis ventas" %></div>
          <div class="hist-total__valor">$<%= String.format("%,.0f", totalGeneral) %></div>
        </div>
        <i class="fi fi-sr-chart-histogram" style="font-size:1.8rem;color:#2e7d32;opacity:0.15"></i>
      </div>

      <!-- Lista de ventas -->
      <div class="hist-seccion">
        <div class="hist-seccion__header">
          <i class="fi fi-sr-list" style="color:white"></i>
          <span class="hist-seccion__titulo"><%= esAdminOSuper ? "Todas las ventas" : "Mis ventas" %></span>
          <span class="hist-seccion__badge"><%= ventas != null ? ventas.size() : 0 %> registros</span>
        </div>

        <% if (ventas == null || ventas.isEmpty()) { %>
        <div class="hist-vacio">
          <i class="fi fi-sr-chart-histogram"></i>
          <p><%= esAdminOSuper ? "No hay ventas registradas." : "Aún no has realizado ninguna venta." %></p>
        </div>
        <% } else { %>

        <!-- Cabecera -->
        <div class="hist-cols <%= esAdminOSuper ? "con-quien" : "sin-quien" %>">
          <span>#</span>
          <span>Fecha</span>
          <% if (esAdminOSuper) { %><span>Realizada por</span><% } %>
          <span>Método de pago</span>
          <span>Total</span>
          <span></span>
        </div>

        <!-- Filas -->
        <% for (FilaVenta v : ventas) { %>
        <div class="hist-fila">
          <div class="hist-fila__datos <%= esAdminOSuper ? "con-quien" : "sin-quien" %>"
               onclick="toggleDetalle(<%= v.id %>)">
            <span class="td-id">#<%= v.id %></span>
            <span class="td-fecha"><%= v.fecha %></span>
            <% if (esAdminOSuper) { %>
            <span><span class="badge-quien"><%= v.realizadaPor != null ? v.realizadaPor : "—" %></span></span>
            <% } %>
            <span><span class="badge-mp"><%= v.metodoPago %></span></span>
            <span class="td-total">$<%= v.total != null ? String.format("%,.0f", v.total) : "0" %></span>
            <span class="td-accion">
              <button class="btn-ver" type="button">
                <i class="fi fi-sr-eye" id="ico-<%= v.id %>"></i> Detalle
              </button>
            </span>
          </div>
          <div class="hist-fila__detalle" id="det-<%= v.id %>">
            <div class="det-pad">
              <em style="color:#aaa;font-size:0.82rem">Cargando...</em>
            </div>
          </div>
        </div>
        <% } %>

        <!-- Pie -->
        <div class="hist-pie">
          <span>Total acumulado</span>
          <span>$<%= String.format("%,.0f", totalGeneral) %></span>
        </div>

        <% } %>
      </div>

    </div>
  </main>

<script>
const cargados = {};
function toggleDetalle(id) {
  const det = document.getElementById('det-' + id);
  const abierto = det.classList.contains('abierto');
  det.classList.toggle('abierto');
  const ico = document.getElementById('ico-' + id);
  if (ico) ico.className = abierto ? 'fi fi-sr-eye' : 'fi fi-sr-eye-crossed';

  if (!abierto && !cargados[id]) {
    fetch('<%= ctx %>/historial?id=' + id)
      .then(function(r) { return r.json(); })
      .then(function(items) {
        const pad = det.querySelector('.det-pad');
        if (!items.length) {
          pad.innerHTML = '<em style="color:#aaa">Sin ítems registrados para esta venta.</em>';
          return;
        }
        let total = 0;
        let html = '<table class="det-tabla"><thead><tr>' +
                   '<th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th>' +
                   '</tr></thead><tbody>';
        items.forEach(function(it) {
          total += Number(it.subtotal);
          html += '<tr>' +
            '<td>' + it.producto + '</td>' +
            '<td>' + it.cantidad + '</td>' +
            '<td>$' + Number(it.precio).toLocaleString('es-CO') + '</td>' +
            '<td>$' + Number(it.subtotal).toLocaleString('es-CO') + '</td>' +
            '</tr>';
        });
        html += '</tbody><tfoot><tr>' +
          '<td colspan="3" style="padding:8px 10px">Total venta</td>' +
          '<td style="padding:8px 10px;text-align:right;">$' + total.toLocaleString('es-CO') + '</td>' +
          '</tr></tfoot></table>';
        pad.innerHTML = html;
        cargados[id] = true;
      })
      .catch(function() {
        const pad = det.querySelector('.det-pad');
        pad.innerHTML = '<em style="color:#e53935">Error al cargar el detalle.</em>';
      });
  }
}
</script>
</body>
</html>
