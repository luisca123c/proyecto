<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario,
                 com.dulce_gestion.dao.GastosDAO.FilaGasto,
                 java.util.List,
                 java.math.BigDecimal" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    boolean esSuperAdmin = "SuperAdministrador".equals(sesionUsuario.getNombreRol());
    boolean esAdmin      = "Administrador".equals(sesionUsuario.getNombreRol());

    List<FilaGasto> gastos  = (List<FilaGasto>) request.getAttribute("gastos");
    List<String[]>  metodos = (List<String[]>)  request.getAttribute("metodos");
    FilaGasto       ge      = (FilaGasto)        request.getAttribute("gastoEditar");
    String error   = (String) request.getAttribute("error");
    String exitoP  = request.getParameter("exito");
    boolean exitoCreado  = "creado".equals(exitoP);
    boolean exitoEditado = "editado".equals(exitoP);

    BigDecimal totalGeneral = BigDecimal.ZERO;
    if (gastos != null) for (FilaGasto g : gastos) totalGeneral = totalGeneral.add(g.total);

    String hoy = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

    boolean abrirEditar  = (ge != null);
    boolean abrirCrear   = (error != null && !abrirEditar);
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Gastos | Dulce Gestion</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
  <style>
    .gastos-wrapper { max-width: 960px; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; }
    .modulo-encabezado { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; }
    .modulo-titulo { font-size:1.3rem; font-weight:700; color:var(--color-texto-oscuro); display:flex; align-items:center; gap:10px; }
    .modulo-titulo i { color:var(--color-principal-morado); }

    /* Tarjeta total */
    .gastos-total { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.07); padding:18px 22px; display:flex; align-items:center; justify-content:space-between; }
    .gastos-total__label { font-size:0.82rem; font-weight:700; text-transform:uppercase; letter-spacing:0.5px; color:#aaa; }
    .gastos-total__valor { font-size:1.6rem; font-weight:800; color:var(--color-danger); }
    .gastos-total__icono { font-size:1.8rem; color:var(--color-danger); opacity:0.15; }

    /* Botón */
    .btn-agregar { display:inline-flex; align-items:center; gap:8px; padding:10px 20px; background:var(--color-principal-morado); color:white; border:none; border-radius:8px; font-weight:700; font-size:0.9rem; cursor:pointer; transition:background 0.2s; }
    .btn-agregar:hover { background:var(--color-morado-medio); }

    /* Sección tabla */
    .gastos-seccion { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.07); overflow:hidden; }
    .gastos-seccion__header { background:var(--color-principal-morado); padding:12px 18px; display:flex; align-items:center; gap:10px; }
    .gastos-seccion__titulo { color:white; font-weight:700; font-size:0.92rem; }
    .gastos-seccion__badge { background:rgba(255,255,255,0.25); color:white; font-size:0.75rem; font-weight:700; padding:2px 8px; border-radius:10px; margin-left:auto; }

    .gastos-tabla { width:100%; border-collapse:collapse; }
    .gastos-tabla th { padding:10px 16px; font-size:0.76rem; font-weight:700; text-transform:uppercase; letter-spacing:0.4px; color:#888; background:#fafafa; border-bottom:1px solid #f0f0f0; text-align:left; }
    .gastos-tabla td { padding:12px 16px; font-size:0.88rem; color:var(--color-texto-oscuro); border-bottom:1px solid #f5f5f5; vertical-align:middle; }
    .gastos-tabla tr:last-child td { border-bottom:none; }
    .gastos-tabla tr:hover td { background:#faf8ff; }
    .td-id    { color:#ccc; font-size:0.76rem; width:36px; }
    .td-total { text-align:right; font-weight:700; color:var(--color-danger); }
    .td-desc  { max-width:180px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
    .badge-mp { display:inline-block; padding:3px 10px; border-radius:20px; font-size:0.75rem; font-weight:600; background:rgba(75,0,130,0.08); color:var(--color-principal-morado); }
    .gastos-tabla tfoot td { background:#fafafa; font-weight:700; border-top:2px solid #f0f0f0; }

    /* Botón editar en fila */
    .btn-editar-fila { display:inline-flex; align-items:center; justify-content:center; width:30px; height:30px; border-radius:6px; border:none; background:rgba(75,0,130,0.1); color:var(--color-principal-morado); cursor:pointer; font-size:0.85rem; transition:background 0.15s; text-decoration:none; }
    .btn-editar-fila:hover { background:rgba(75,0,130,0.2); }

    /* Vacío */
    .gastos-vacio { padding:36px; text-align:center; color:#ccc; }
    .gastos-vacio i { font-size:2.5rem; display:block; margin-bottom:10px; }

    /* Mensajes */
    .msg-exito { display:flex; align-items:center; gap:10px; padding:12px 16px; border-radius:8px; background:rgba(46,125,50,0.1); border-left:4px solid var(--color-success); color:#1b5e20; font-weight:600; }
    .msg-error { display:flex; align-items:center; gap:10px; padding:12px 16px; border-radius:8px; background:rgba(169,50,38,0.1); border-left:4px solid var(--color-danger); color:var(--color-danger); font-weight:600; }

    /* Modal */
    .modal-overlay { display:none; position:fixed; inset:0; background:rgba(0,0,0,0.45); z-index:999; align-items:center; justify-content:center; }
    .modal-overlay.activo { display:flex; }
    .modal-caja { background:white; border-radius:12px; padding:28px; width:440px; max-width:95vw; box-shadow:0 8px 32px rgba(0,0,0,0.2); display:flex; flex-direction:column; gap:16px; }
    .modal-titulo { font-weight:700; color:var(--color-texto-oscuro); font-size:1.05rem; display:flex; align-items:center; gap:10px; border-bottom:1px solid #f0f0f0; padding-bottom:12px; }
    .modal-titulo i { color:var(--color-principal-morado); }
    .campo { display:flex; flex-direction:column; gap:6px; }
    .campo label { font-size:0.78rem; font-weight:700; color:#666; text-transform:uppercase; letter-spacing:0.4px; }
    .campo input, .campo select, .campo textarea { padding:10px 14px; border:1.5px solid #e0e0e0; border-radius:8px; font-size:0.92rem; color:var(--color-texto-oscuro); background:white; width:100%; box-sizing:border-box; font-family:inherit; transition:border-color 0.15s; }
    .campo input:focus, .campo select:focus, .campo textarea:focus { outline:none; border-color:var(--color-principal-morado); }
    .campo textarea { resize:vertical; min-height:72px; }
    .modal-botones { display:flex; gap:10px; padding-top:4px; }
    .btn-guardar { flex:1; padding:11px; border:none; border-radius:8px; background:var(--color-principal-morado); color:white; font-weight:700; font-size:0.92rem; cursor:pointer; display:flex; align-items:center; justify-content:center; gap:8px; }
    .btn-guardar:hover { background:var(--color-morado-medio); }
    .btn-guardar--editar { background:var(--color-success); }
    .btn-guardar--editar:hover { background:var(--color-success-hover); }
    .btn-cancelar { padding:11px 18px; border:1.5px solid #e0e0e0; border-radius:8px; background:white; color:#666; font-weight:600; font-size:0.9rem; cursor:pointer; }
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
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/gastos"><i class="fi fi-sr-receipt"></i><span>Gastos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ganancias"><i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/perfil"><i class="fi fi-sr-user"></i><span>Perfil</span></a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout"><i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesion</span></a>
    </nav>
  </aside>

  <main class="pagina-main">
    <div class="gastos-wrapper">

      <div class="modulo-encabezado">
        <h1 class="modulo-titulo"><i class="fi fi-sr-receipt"></i> Gastos</h1>
        <button class="btn-agregar" onclick="abrirCrear()">
          <i class="fi fi-sr-add"></i> Registrar gasto
        </button>
      </div>

      <% if (exitoCreado) { %>
      <div class="msg-exito"><i class="fi fi-sr-check-circle"></i> Gasto registrado correctamente.</div>
      <% } else if (exitoEditado) { %>
      <div class="msg-exito"><i class="fi fi-sr-check-circle"></i> Gasto actualizado correctamente.</div>
      <% } %>
      <% if (error != null && !error.isBlank()) { %>
      <div class="msg-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <!-- Tarjeta total -->
      <div class="gastos-total">
        <div>
          <div class="gastos-total__label">Total acumulado</div>
          <div class="gastos-total__valor">$<%= String.format("%,.0f", totalGeneral) %></div>
        </div>
        <i class="fi fi-sr-receipt gastos-total__icono"></i>
      </div>

      <!-- Tabla -->
      <div class="gastos-seccion">
        <div class="gastos-seccion__header">
          <i class="fi fi-sr-list" style="color:white"></i>
          <span class="gastos-seccion__titulo">Historial de gastos</span>
          <span class="gastos-seccion__badge"><%= gastos != null ? gastos.size() : 0 %></span>
        </div>

        <% if (gastos == null || gastos.isEmpty()) { %>
        <div class="gastos-vacio">
          <i class="fi fi-sr-receipt"></i>
          <p>No hay gastos registrados aun.</p>
        </div>
        <% } else { %>
        <table class="gastos-tabla">
          <thead>
            <tr>
              <th>#</th><th>Fecha</th><th>Descripcion</th>
              <th>Metodo pago</th><th>Registrado por</th>
              <th style="text-align:right">Total</th><th></th>
            </tr>
          </thead>
          <tbody>
            <% for (FilaGasto g : gastos) { %>
            <tr>
              <td class="td-id">#<%= g.id %></td>
              <td><%= g.fecha %></td>
              <td class="td-desc" title="<%= g.descripcion != null ? g.descripcion : "" %>">
                <%= g.descripcion != null ? g.descripcion : "—" %>
              </td>
              <td><span class="badge-mp"><%= g.metodoPago %></span></td>
              <td><%= g.registradoPor %></td>
              <td class="td-total">$<%= String.format("%,.0f", g.total) %></td>
              <td>
                <a href="<%= ctx %>/gastos?editar=<%= g.id %>"
                   class="btn-editar-fila" title="Editar gasto">
                  <i class="fi fi-sr-pencil"></i>
                </a>
              </td>
            </tr>
            <% } %>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="5" style="padding:10px 16px;color:#888;">Total</td>
              <td class="td-total" style="padding:10px 16px;">$<%= String.format("%,.0f", totalGeneral) %></td>
              <td></td>
            </tr>
          </tfoot>
        </table>
        <% } %>
      </div>

    </div>  <!-- Datos del servidor para el JS externo -->
  <div id="gastos-data" hidden
       data-abrir="<% if (abrirEditar) { %>editar<% } else if (abrirCrear) { %>crear<% } %>"></div>

  </main>

  <!-- ── MODAL CREAR ── -->
  <div class="modal-overlay" id="modalCrear">
    <div class="modal-caja">
      <div class="modal-titulo"><i class="fi fi-sr-add"></i> Registrar gasto</div>
      <form method="POST" action="<%= ctx %>/gastos">
        <input type="hidden" name="accion" value="crear">
        <div class="campo">
          <label>Descripcion *</label>
          <textarea name="descripcion" placeholder="Ej: Compra de ingredientes..." required></textarea>
        </div>
        <div class="campo">
          <label>Monto *</label>
          <input type="number" name="total" min="1" step="0.01" placeholder="0" required>
        </div>
        <div class="campo">
          <label>Metodo de pago *</label>
          <select name="idMetodoPago" required>
            <% if (metodos != null) for (String[] m : metodos) { %>
            <option value="<%= m[0] %>"><%= m[1] %></option>
            <% } %>
          </select>
        </div>
        <div class="campo">
          <label>Fecha *</label>
          <input type="date" name="fecha" value="<%= hoy %>" max="<%= hoy %>" required>
        </div>
        <div class="modal-botones">
          <button type="submit" class="btn-guardar"><i class="fi fi-sr-check"></i> Guardar</button>
          <button type="button" class="btn-cancelar" onclick="cerrarCrear()">Cancelar</button>
        </div>
      </form>
    </div>
  </div>

  <!-- ── MODAL EDITAR ── -->
  <div class="modal-overlay" id="modalEditar">
    <div class="modal-caja">
      <div class="modal-titulo"><i class="fi fi-sr-pencil"></i> Editar gasto</div>
      <form method="POST" action="<%= ctx %>/gastos">
        <input type="hidden" name="accion" value="editar">
        <input type="hidden" name="idGasto"         id="e_idGasto"         value="<%= ge != null ? ge.id : "" %>">
        <input type="hidden" name="idDetalleCompra" id="e_idDetalleCompra" value="<%= ge != null ? ge.idDetalleCompra : "" %>">
        <input type="hidden" name="idCompra"        id="e_idCompra"        value="<%= ge != null ? ge.idCompra : "" %>">
        <div class="campo">
          <label>Descripcion *</label>
          <textarea name="descripcion" id="e_descripcion" required><%= ge != null && ge.descripcion != null ? ge.descripcion : "" %></textarea>
        </div>
        <div class="campo">
          <label>Monto *</label>
          <input type="number" name="total" id="e_total" min="1" step="0.01" required
                 value="<%= ge != null ? ge.total : "" %>">
        </div>
        <div class="campo">
          <label>Metodo de pago *</label>
          <select name="idMetodoPago" id="e_metodo" required>
            <% if (metodos != null) for (String[] m : metodos) { %>
            <option value="<%= m[0] %>"
              <%= (ge != null && ge.idMetodoPago == Integer.parseInt(m[0])) ? "selected" : "" %>>
              <%= m[1] %>
            </option>
            <% } %>
          </select>
        </div>
        <div class="campo">
          <label>Fecha *</label>
          <input type="date" name="fecha" id="e_fecha" max="<%= hoy %>" required
                 value="<%= ge != null ? ge.fechaRaw : hoy %>">
        </div>
        <div class="modal-botones">
          <button type="submit" class="btn-guardar btn-guardar--editar">
            <i class="fi fi-sr-check"></i> Guardar cambios
          </button>
          <button type="button" class="btn-cancelar" onclick="cerrarEditar()">Cancelar</button>
        </div>
      </form>
    </div>
  </div>

</body>
<script src="<%= ctx %>/assets/js/gastos/gastos.js" defer></script>
</html>