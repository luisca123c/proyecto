<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario, com.dulce_gestion.models.CarritoItem, com.dulce_gestion.models.Producto, java.util.List, java.math.BigDecimal" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    String rolSolicitante = sesionUsuario.getNombreRol();
    boolean esSuperAdmin = "SuperAdministrador".equals(rolSolicitante);
    boolean esAdmin      = "Administrador".equals(rolSolicitante);
    List<CarritoItem> items     = (List<CarritoItem>) request.getAttribute("items");
    List<Producto>    productos = (List<Producto>)    request.getAttribute("productos");
    List<String[]>    metodos   = (List<String[]>)    request.getAttribute("metodos");
    BigDecimal        total     = (BigDecimal)         request.getAttribute("total");
    String error        = (String) request.getAttribute("error");
    String exitoParam   = request.getParameter("exito");
    String idVentaParam = request.getParameter("id");
    boolean exitoVenta  = "venta".equals(exitoParam);
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Ventas | Dulce Gestion</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
  <style>
    .ventas-wrapper { max-width: 900px; margin: 0 auto; }
    .modulo-encabezado { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; margin-bottom:20px; }
    .modulo-titulo { font-size:1.3rem; font-weight:700; color:var(--color-texto-oscuro); display:flex; align-items:center; gap:10px; }
    .modulo-titulo i { color:var(--color-principal-morado); }

    /* Carrito section */
    .carrito-seccion { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.08); overflow:hidden; margin-bottom:24px; }
    .carrito-seccion__header { background:var(--color-principal-morado); padding:14px 18px; display:flex; align-items:center; justify-content:space-between; }
    .carrito-seccion__titulo { color:white; font-weight:700; font-size:0.95rem; display:flex; align-items:center; gap:8px; }

    /* Items */
    .carrito-item { display:flex; align-items:center; gap:12px; padding:12px 18px; border-bottom:1px solid #f0f0f0; }
    .carrito-item:last-child { border-bottom:none; }
    .carrito-item__img { width:48px; height:48px; border-radius:8px; object-fit:cover; background:var(--color-principal-morado); flex-shrink:0; }
    .carrito-item__placeholder { width:48px; height:48px; border-radius:8px; background:var(--color-principal-morado); display:flex; align-items:center; justify-content:center; color:rgba(255,255,255,0.6); font-size:1.3rem; flex-shrink:0; }
    .carrito-item__info { flex:1; min-width:0; }
    .carrito-item__nombre { font-weight:600; font-size:0.9rem; color:var(--color-texto-oscuro); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
    .carrito-item__precio { font-size:0.78rem; color:#888; margin-top:2px; }
    .carrito-item__controles { display:flex; align-items:center; gap:6px; flex-shrink:0; }
    .qty-btn { width:28px; height:28px; border-radius:50%; border:2px solid var(--color-principal-morado); background:transparent; color:var(--color-principal-morado); font-size:1rem; font-weight:700; cursor:pointer; display:flex; align-items:center; justify-content:center; transition:all 0.15s; }
    .qty-btn:hover { background:var(--color-principal-morado); color:white; }
    .qty-btn:disabled { opacity:0.35; cursor:not-allowed; }
    .carrito-item__cantidad { min-width:26px; text-align:center; font-weight:700; font-size:0.95rem; color:var(--color-texto-oscuro); }
    .carrito-item__subtotal { font-weight:700; color:var(--color-texto-oscuro); font-size:0.9rem; min-width:80px; text-align:right; }
    .btn-eliminar-item { width:30px; height:30px; border-radius:6px; border:none; background:rgba(169,50,38,0.1); color:var(--color-danger); cursor:pointer; display:flex; align-items:center; justify-content:center; font-size:0.85rem; transition:background 0.15s; flex-shrink:0; }
    .btn-eliminar-item:hover { background:rgba(169,50,38,0.25); }

    /* Vacio */
    .carrito-vacio { padding:36px 20px; text-align:center; color:#aaa; }
    .carrito-vacio i { font-size:2.5rem; display:block; margin-bottom:10px; }

    /* Footer */
    .carrito-footer { display:flex; flex-direction:column; gap:14px; padding:16px 18px; background:#fafafa; border-top:2px solid #f0f0f0; }
    .carrito-total-row { display:flex; justify-content:space-between; align-items:center; font-weight:700; font-size:1.05rem; color:var(--color-texto-oscuro); }
    .carrito-total-valor { color:var(--color-principal-morado); }
    .carrito-pago-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
    .carrito-pago-row label { font-size:0.85rem; font-weight:600; color:#555; }
    .select-metodo { flex:1; min-width:140px; padding:8px 12px; border:1px solid #ddd; border-radius:6px; font-size:0.9rem; color:var(--color-texto-oscuro); background:white; }
    .carrito-acciones { display:flex; gap:10px; flex-wrap:wrap; }
    .btn-confirmar { flex:1; padding:11px 16px; border:none; border-radius:8px; background:var(--color-success); color:white; font-weight:700; font-size:0.9rem; cursor:pointer; display:flex; align-items:center; justify-content:center; gap:8px; transition:background 0.2s; }
    .btn-confirmar:hover { background:var(--color-success-hover); }
    .btn-vaciar { padding:11px 16px; border:none; border-radius:8px; background:rgba(169,50,38,0.1); color:var(--color-danger); font-weight:600; font-size:0.9rem; cursor:pointer; display:flex; align-items:center; gap:6px; transition:background 0.2s; }
    .btn-vaciar:hover { background:rgba(169,50,38,0.2); }

    /* Agregar */
    .agregar-titulo { font-size:1rem; font-weight:700; color:var(--color-texto-oscuro); display:flex; align-items:center; gap:8px; margin-bottom:14px; }
    .agregar-titulo i { color:var(--color-principal-morado); }
    .pcard { cursor:pointer; }
    .pcard__add { margin:0 10px 10px; padding:8px; border:none; border-radius:6px; background:var(--color-morado-medio); color:white; font-size:0.82rem; font-weight:600; cursor:pointer; width:calc(100% - 20px); transition:background 0.2s; display:flex; align-items:center; justify-content:center; gap:6px; }
    .pcard__add:hover { background:var(--color-morado-claro); }

    /* Mensajes */
    .msg-exito { display:flex; align-items:center; gap:10px; padding:12px 16px; border-radius:8px; background:rgba(46,125,50,0.1); border-left:4px solid var(--color-success); color:#1b5e20; font-weight:600; margin-bottom:16px; }
    .msg-error { display:flex; align-items:center; gap:10px; padding:12px 16px; border-radius:8px; background:rgba(169,50,38,0.1); border-left:4px solid var(--color-danger); color:var(--color-danger); font-weight:600; margin-bottom:16px; }

    /* Modal */
    .modal-overlay { display:none; position:fixed; inset:0; background:rgba(0,0,0,0.45); z-index:999; align-items:center; justify-content:center; }
    .modal-overlay.activo { display:flex; }
    .modal-caja { background:white; border-radius:12px; padding:24px; width:300px; box-shadow:0 8px 32px rgba(0,0,0,0.2); display:flex; flex-direction:column; gap:14px; }
    .modal-titulo { font-weight:700; color:var(--color-texto-oscuro); font-size:1rem; }
    .modal-input { padding:10px 14px; border:1px solid #ddd; border-radius:8px; font-size:1rem; width:100%; color:var(--color-texto-oscuro); box-sizing:border-box; }
    .modal-input:focus { outline:none; border-color:var(--color-principal-morado); }
    .modal-botones { display:flex; gap:10px; }
    .modal-btn-ok { flex:1; padding:10px; border:none; border-radius:8px; background:var(--color-principal-morado); color:white; font-weight:700; cursor:pointer; }
    .modal-btn-ok:hover { background:var(--color-morado-medio); }
    .modal-btn-cancel { flex:1; padding:10px; border:1px solid #ddd; border-radius:8px; background:white; color:#555; font-weight:600; cursor:pointer; }
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
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/ventas"><i class="fi fi-sr-shopping-cart"></i><span>Carrito</span></a>
      <a class="sidebar__link" href="<%= ctx %>/historial"><i class="fi fi-sr-chart-histogram"></i><span>Ventas</span></a>
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
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout"><i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesion</span></a>
    </nav>
  </aside>

  <main class="pagina-main">
    <div class="ventas-wrapper">

      <div class="modulo-encabezado">
        <h1 class="modulo-titulo"><i class="fi fi-sr-shopping-cart"></i> Carrito de Ventas</h1>
      </div>

      <% if (exitoVenta) { %>
      <div class="msg-exito"><i class="fi fi-sr-check-circle"></i> Venta #<%= idVentaParam %> registrada correctamente.</div>
      <% } %>
      <% if (error != null && !error.isBlank()) { %>
      <div class="msg-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <!-- CARRITO -->
      <div class="carrito-seccion">
        <div class="carrito-seccion__header">
          <span class="carrito-seccion__titulo"><i class="fi fi-sr-shopping-bag"></i> Mi carrito</span>
          <% if (items != null && !items.isEmpty()) { %>
          <span style="color:rgba(255,255,255,0.8);font-size:0.82rem;"><%= items.size() %> producto(s)</span>
          <% } %>
        </div>
        <div class="carrito-seccion__body">
          <% if (items == null || items.isEmpty()) { %>
          <div class="carrito-vacio">
            <i class="fi fi-sr-shopping-cart"></i>
            <p>El carrito esta vacio. Agrega productos abajo.</p>
          </div>
          <% } else { %>
            <% for (CarritoItem item : items) { %>
            <div class="carrito-item">
              <% if (item.getPathImagen() != null && !item.getPathImagen().isBlank()) { %>
              <img class="carrito-item__img" src="<%= ctx %>/<%= item.getPathImagen() %>" alt="<%= item.getNombreProducto() %>">
              <% } else { %>
              <div class="carrito-item__placeholder"><i class="fi fi-sr-box-open"></i></div>
              <% } %>
              <div class="carrito-item__info">
                <div class="carrito-item__nombre"><%= item.getNombreProducto() %></div>
                <div class="carrito-item__precio">$<%= String.format("%,.0f", item.getPrecioUnitario()) %> c/u</div>
              </div>
              <div class="carrito-item__controles">
                <form method="POST" action="<%= ctx %>/ventas" style="display:contents">
                  <input type="hidden" name="accion" value="actualizar">
                  <input type="hidden" name="idDetalle" value="<%= item.getIdDetalle() %>">
                  <input type="hidden" name="cantidad" value="<%= item.getCantidad() - 1 %>">
                  <button type="submit" class="qty-btn">-</button>
                </form>
                <span class="carrito-item__cantidad"><%= item.getCantidad() %></span>
                <form method="POST" action="<%= ctx %>/ventas" style="display:contents">
                  <input type="hidden" name="accion" value="actualizar">
                  <input type="hidden" name="idDetalle" value="<%= item.getIdDetalle() %>">
                  <input type="hidden" name="cantidad" value="<%= item.getCantidad() + 1 %>">
                  <button type="submit" class="qty-btn" <%= item.getCantidad() >= item.getStockDisponible() ? "disabled" : "" %>>+</button>
                </form>
              </div>
              <div class="carrito-item__subtotal">$<%= String.format("%,.0f", item.getSubtotal()) %></div>
              <form method="POST" action="<%= ctx %>/ventas" style="display:contents">
                <input type="hidden" name="accion" value="eliminar">
                <input type="hidden" name="idDetalle" value="<%= item.getIdDetalle() %>">
                <button type="submit" class="btn-eliminar-item" title="Eliminar"><i class="fi fi-sr-trash"></i></button>
              </form>
            </div>
            <% } %>
            <div class="carrito-footer">
              <div class="carrito-total-row">
                <span>Total</span>
                <span class="carrito-total-valor">$<%= String.format("%,.0f", total) %></span>
              </div>
              <form method="POST" action="<%= ctx %>/ventas">
                <input type="hidden" name="accion" value="confirmar">
                <div class="carrito-pago-row">
                  <label>Metodo de pago</label>
                  <select class="select-metodo" name="idMetodoPago" required>
                    <% for (String[] m : metodos) { %>
                    <option value="<%= m[0] %>"><%= m[1] %></option>
                    <% } %>
                  </select>
                </div>
                <div class="carrito-acciones" style="margin-top:12px">
                  <button type="submit" class="btn-confirmar"><i class="fi fi-sr-check"></i> Confirmar venta</button>
                </div>
              </form>
              <form method="POST" action="<%= ctx %>/ventas" onsubmit="return confirm('Vaciar todo el carrito?')">
                <input type="hidden" name="accion" value="vaciar">
                <button type="submit" class="btn-vaciar"><i class="fi fi-sr-trash"></i> Vaciar carrito</button>
              </form>
            </div>
          <% } %>
        </div>
      </div>

      <!-- AGREGAR PRODUCTOS -->
      <div class="agregar-titulo"><i class="fi fi-sr-add"></i> Agregar productos</div>
      <% if (productos == null || productos.isEmpty()) { %>
      <p style="color:#aaa;font-size:0.9rem;">No hay productos activos con stock disponible.</p>
      <% } else { %>
      <div class="productos-grid">
        <% for (Producto p : productos) { %>
        <%
          String _nom = p.getNombre().replace("\\", "\\\\").replace("'", "\\'");
        %>
        <div class="pcard" onclick="abrirModal(<%= p.getId() %>, '<%= _nom %>', <%= p.getStockActual() %>)">
          <div class="pcard__imagen">
            <% if (p.getPathImagen() != null && !p.getPathImagen().isBlank()) { %>
            <img src="<%= ctx %>/<%= p.getPathImagen() %>" alt="<%= p.getNombre() %>">
            <% } else { %>
            <div class="pcard__placeholder"><i class="fi fi-sr-box-open"></i></div>
            <% } %>
          </div>
          <div class="pcard__info">
            <div class="pcard__nombre"><%= p.getNombre() %></div>
            <div class="pcard__detalle">$<%= String.format("%,.0f", p.getPrecioUnitario()) %> &middot; Stock: <%= p.getStockActual() %></div>
          </div>
          <button class="pcard__add" type="button"><i class="fi fi-sr-add"></i> Agregar</button>
        </div>
        <% } %>
      </div>
      <% } %>

    </div>
  </main>

  <!-- MODAL -->
  <div class="modal-overlay" id="modalOverlay">
    <div class="modal-caja">
      <div class="modal-titulo" id="modalNombre">Agregar producto</div>
      <form method="POST" action="<%= ctx %>/ventas">
        <input type="hidden" name="accion" value="agregar">
        <input type="hidden" name="idProducto" id="modalIdProducto">
        <input class="modal-input" type="number" name="cantidad" id="modalCantidad" value="1" min="1">
        <div class="modal-botones" style="margin-top:12px">
          <button type="submit" class="modal-btn-ok"><i class="fi fi-sr-add"></i> Agregar</button>
          <button type="button" class="modal-btn-cancel" onclick="cerrarModal()">Cancelar</button>
        </div>
      </form>
    </div>
  </div>
</body>
<script src="<%= ctx %>/assets/js/ventas/carrito.js" defer></script>
</html>