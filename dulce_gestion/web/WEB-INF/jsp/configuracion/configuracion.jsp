<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario,
                 com.dulce_gestion.dao.ConfiguracionDAO.Fila,
                 java.util.List" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    boolean esSuperAdmin = "SuperAdministrador".equals(sesionUsuario.getNombreRol());
    boolean esAdmin      = "Administrador".equals(sesionUsuario.getNombreRol());

    List<Fila> categorias = (List<Fila>) request.getAttribute("categorias");
    List<Fila> unidades   = (List<Fila>) request.getAttribute("unidades");
    List<Fila> metodos    = (List<Fila>) request.getAttribute("metodos");

    String tab    = (String) request.getAttribute("tab");
    if (tab == null) tab = "categorias";
    String error  = (String) request.getAttribute("error");
    String exito  = request.getParameter("exito");
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Configuración | Dulce Gestión</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
  <style>
    .cfg-wrapper { max-width: 820px; margin: 0 auto; display:flex; flex-direction:column; gap:24px; }
    .modulo-titulo { font-size:1.3rem; font-weight:700; color:var(--color-texto-oscuro); display:flex; align-items:center; gap:10px; }
    .modulo-titulo i { color:var(--color-principal-morado); }

    /* Tabs */
    .tabs { display:flex; gap:4px; background:#f0f0f0; border-radius:10px; padding:4px; }
    .tab-btn { flex:1; padding:9px 14px; border:none; border-radius:8px; font-size:0.85rem; font-weight:600; cursor:pointer; background:transparent; color:#888; transition:all 0.2s; }
    .tab-btn.activo { background:white; color:var(--color-principal-morado); box-shadow:0 1px 4px rgba(0,0,0,0.12); }

    /* Panel */
    .panel { background:white; border-radius:var(--radius-md); box-shadow:0 2px 8px rgba(0,0,0,0.07); overflow:hidden; }
    .panel__header { background:var(--color-principal-morado); padding:12px 18px; display:flex; align-items:center; gap:10px; }
    .panel__titulo { color:white; font-weight:700; font-size:0.92rem; flex:1; }
    .btn-agregar { display:inline-flex; align-items:center; gap:6px; padding:7px 14px; background:rgba(255,255,255,0.2); color:white; border:none; border-radius:6px; font-weight:700; font-size:0.82rem; cursor:pointer; transition:background 0.15s; }
    .btn-agregar:hover { background:rgba(255,255,255,0.3); }

    /* Tabla */
    .cfg-tabla { width:100%; border-collapse:collapse; }
    .cfg-tabla th { padding:9px 16px; font-size:0.74rem; font-weight:700; text-transform:uppercase; color:#aaa; background:#fafafa; border-bottom:1px solid #f0f0f0; text-align:left; }
    .cfg-tabla td { padding:11px 16px; font-size:0.88rem; color:var(--color-texto-oscuro); border-bottom:1px solid #f5f5f5; vertical-align:middle; }
    .cfg-tabla tr:last-child td { border-bottom:none; }
    .cfg-tabla tr:hover td { background:#faf8ff; }
    .td-id { color:#ccc; font-size:0.76rem; width:40px; }
    .td-desc { max-width:200px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; color:#888; font-size:0.82rem; }
    .td-acciones { text-align:right; white-space:nowrap; }

    /* Botones de fila */
    .btn-fila { display:inline-flex; align-items:center; justify-content:center; width:30px; height:30px; border-radius:6px; border:none; cursor:pointer; font-size:0.85rem; transition:background 0.15s; }
    .btn-editar { background:rgba(75,0,130,0.1); color:var(--color-principal-morado); }
    .btn-editar:hover { background:rgba(75,0,130,0.2); }
    .btn-eliminar { background:rgba(198,40,40,0.1); color:#c62828; }
    .badge-activo   { display:inline-block; padding:2px 8px; border-radius:10px; font-size:0.72rem; font-weight:700; background:rgba(46,125,50,0.12); color:#2e7d32; }
    .badge-inactivo { display:inline-block; padding:2px 8px; border-radius:10px; font-size:0.72rem; font-weight:700; background:rgba(198,40,40,0.12); color:#c62828; }
    .btn-eliminar:hover { background:rgba(198,40,40,0.2); }

    /* Vacío */
    .cfg-vacio { padding:32px; text-align:center; color:#ccc; font-size:0.88rem; }

    /* Mensajes */
    .msg-exito { display:flex; align-items:center; gap:10px; padding:13px 18px; border-radius:8px; background:linear-gradient(135deg,#2e7d32,#388e3c); color:#fff; font-weight:600; }
    .msg-error { display:flex; align-items:center; gap:10px; padding:13px 18px; border-radius:8px; background:linear-gradient(135deg,#c62828,#e53935); color:#fff; font-weight:600; }

    /* Modal */
    .modal-overlay { display:none; position:fixed; inset:0; background:rgba(0,0,0,0.45); z-index:999; align-items:center; justify-content:center; }
    .modal-overlay.activo { display:flex; }
    .modal-caja { background:white; border-radius:12px; padding:28px; width:420px; max-width:95vw; box-shadow:0 8px 32px rgba(0,0,0,0.2); display:flex; flex-direction:column; gap:14px; }
    .modal-titulo { font-weight:700; color:var(--color-texto-oscuro); font-size:1rem; display:flex; align-items:center; gap:8px; border-bottom:1px solid #f0f0f0; padding-bottom:12px; }
    .modal-titulo i { color:var(--color-principal-morado); }
    .campo { display:flex; flex-direction:column; gap:5px; }
    .campo label { font-size:0.76rem; font-weight:700; color:#666; text-transform:uppercase; letter-spacing:0.4px; }
    .campo input, .campo textarea { padding:10px 14px; border:1.5px solid #e0e0e0; border-radius:8px; font-size:0.92rem; width:100%; box-sizing:border-box; font-family:inherit; transition:border-color 0.15s; }
    .campo input:focus, .campo textarea:focus { outline:none; border-color:var(--color-principal-morado); }
    .campo textarea { resize:vertical; min-height:68px; }
    .modal-botones { display:flex; gap:10px; padding-top:4px; }
    .btn-guardar { flex:1; padding:10px; border:none; border-radius:8px; background:var(--color-principal-morado); color:white; font-weight:700; font-size:0.9rem; cursor:pointer; display:flex; align-items:center; justify-content:center; gap:8px; }
    .btn-guardar:hover { background:var(--color-morado-medio); }
    .btn-guardar--edit { background:#2e7d32; }
    .btn-guardar--edit:hover { background:#1b5e20; }
    .btn-cancelar { padding:10px 16px; border:1.5px solid #e0e0e0; border-radius:8px; background:white; color:#666; font-weight:600; font-size:0.88rem; cursor:pointer; }
    .toggle-row { display:flex; align-items:center; justify-content:space-between; padding:10px 14px; background:#f9f7ff; border-radius:8px; border:1.5px solid #e0e0e0; }
    .toggle-label { font-size:0.85rem; font-weight:600; color:#555; }
    .toggle-switch { position:relative; display:inline-block; width:44px; height:24px; }
    .toggle-switch input { opacity:0; width:0; height:0; }
    .toggle-slider { position:absolute; cursor:pointer; inset:0; background:#ccc; border-radius:24px; transition:0.2s; }
    .toggle-slider:before { position:absolute; content:""; height:18px; width:18px; left:3px; bottom:3px; background:white; border-radius:50%; transition:0.2s; }
    .toggle-switch input:checked + .toggle-slider { background:var(--color-principal-morado); }
    .toggle-switch input:checked + .toggle-slider:before { transform:translateX(20px); }
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
      <a class="sidebar__link" href="<%= ctx %>/empleados"><i class="fi fi-sr-users"></i><span>Empleados</span></a>
      <a class="sidebar__link" href="<%= ctx %>/productos"><i class="fi fi-sr-box-open"></i><span>Productos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ventas"><i class="fi fi-sr-shopping-cart"></i><span>Carrito</span></a>
      <a class="sidebar__link" href="<%= ctx %>/historial"><i class="fi fi-sr-chart-histogram"></i><span>Ventas</span></a>
      <a class="sidebar__link" href="<%= ctx %>/gastos"><i class="fi fi-sr-receipt"></i><span>Gastos</span></a>
      <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
      <a class="sidebar__link" href="<%= ctx %>/ganancias"><i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span></a>
      <a class="sidebar__link" href="<%= ctx %>/emprendimientos"><i class="fi fi-sr-store-alt"></i><span>Emprendimientos</span></a>
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/configuracion"><i class="fi fi-sr-settings"></i><span>Configuración</span></a>
      <a class="sidebar__link" href="<%= ctx %>/perfil"><i class="fi fi-sr-user"></i><span>Perfil</span></a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout"><i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesión</span></a>
    </nav>
  </aside>

  <main class="pagina-main">
    <div class="cfg-wrapper">

      <h1 class="modulo-titulo"><i class="fi fi-sr-settings"></i> Configuración del sistema</h1>

      <% if (exito != null) { %>
      <div class="msg-exito"><i class="fi fi-sr-check-circle"></i>
        <% if ("creado".equals(exito)) { %>Registro creado correctamente.
        <% } else if ("editado".equals(exito)) { %>Registro actualizado correctamente.
        <% } else { %>Registro inactivado correctamente.<% } %>
      </div>
      <% } %>
      <% if (error != null && !error.isBlank()) { %>
      <div class="msg-error"><i class="fi fi-sr-triangle-warning"></i> <%= error %></div>
      <% } %>

      <!-- Tabs -->
      <div class="tabs">
        <button class="tab-btn" id="tab-categorias" onclick="cambiarTab('categorias')">
          <i class="fi fi-sr-tags"></i> Categorías
        </button>
        <button class="tab-btn" id="tab-unidades" onclick="cambiarTab('unidades')">
          <i class="fi fi-sr-ruler-combined"></i> Unidades de medida
        </button>
        <button class="tab-btn" id="tab-metodos" onclick="cambiarTab('metodos')">
          <i class="fi fi-sr-credit-card"></i> Métodos de pago
        </button>
      </div>

      <!-- ══ PANEL CATEGORÍAS ══ -->
      <div class="panel" id="panel-categorias">
        <div class="panel__header">
          <i class="fi fi-sr-tags" style="color:white"></i>
          <span class="panel__titulo">Categorías de productos (<%= categorias != null ? categorias.size() : 0 %>)</span>
          <button class="btn-agregar" onclick="nuevaCat()">
            <i class="fi fi-sr-add"></i> Nueva categoría
          </button>
        </div>
        <% if (categorias == null || categorias.isEmpty()) { %>
        <div class="cfg-vacio">No hay categorías registradas.</div>
        <% } else { %>
        <table class="cfg-tabla">
          <thead><tr><th>#</th><th>Nombre</th><th>Descripción</th><th>Estado</th><th></th></tr></thead>
          <tbody>
            <% for (Fila f : categorias) { %>
            <tr>
              <td class="td-id"><%= f.id %></td>
              <td><strong><%= f.nombre %></strong></td>
              <td class="td-desc"><%= f.descripcion != null ? f.descripcion : "—" %></td>
              <td><span class="<%= f.activo ? "badge-activo" : "badge-inactivo" %>"><%= f.activo ? "Activo" : "Inactivo" %></span></td>
              <td class="td-acciones">
                <button class="btn-fila btn-editar" title="Editar"
                        data-id="<%= f.id %>" data-nombre="<%= f.nombre %>"
                        data-desc="<%= f.descripcion != null ? f.descripcion : "" %>"
                        data-activo="<%= f.activo %>"
                        onclick="editarCat(this)">
                  <i class="fi fi-sr-pencil"></i>
                </button>
                <% if (f.activo) { %>
                <form method="POST" action="<%= ctx %>/configuracion" style="display:inline"
                      onsubmit="return confirm('¿Inactivar categoría \'<%= f.nombre %>\'?')">
                  <input type="hidden" name="accion" value="eliminarCategoria">
                  <input type="hidden" name="id" value="<%= f.id %>">
                  <input type="hidden" name="tab" value="categorias">
                  <button type="submit" class="btn-fila btn-eliminar" title="Inactivar">
                    <i class="fi fi-sr-ban"></i>
                  </button>
                </form>
                <% } %>
              </td>
            </tr>
            <% } %>
          </tbody>
        </table>
        <% } %>
      </div>

      <!-- ══ PANEL UNIDADES ══ -->
      <div class="panel" id="panel-unidades">
        <div class="panel__header">
          <i class="fi fi-sr-ruler-combined" style="color:white"></i>
          <span class="panel__titulo">Unidades de medida (<%= unidades != null ? unidades.size() : 0 %>)</span>
          <button class="btn-agregar" onclick="nuevaUni()">
            <i class="fi fi-sr-add"></i> Nueva unidad
          </button>
        </div>
        <% if (unidades == null || unidades.isEmpty()) { %>
        <div class="cfg-vacio">No hay unidades de medida registradas.</div>
        <% } else { %>
        <table class="cfg-tabla">
          <thead><tr><th>#</th><th>Nombre</th><th>Estado</th><th></th></tr></thead>
          <tbody>
            <% for (Fila f : unidades) { %>
            <tr>
              <td class="td-id"><%= f.id %></td>
              <td><strong><%= f.nombre %></strong></td>
              <td><span class="<%= f.activo ? "badge-activo" : "badge-inactivo" %>"><%= f.activo ? "Activo" : "Inactivo" %></span></td>
              <td class="td-acciones">
                <button class="btn-fila btn-editar" title="Editar"
                        data-id="<%= f.id %>" data-nombre="<%= f.nombre %>"
                        data-activo="<%= f.activo %>"
                        onclick="editarUni(this)">
                  <i class="fi fi-sr-pencil"></i>
                </button>
                <% if (f.activo) { %>
                <form method="POST" action="<%= ctx %>/configuracion" style="display:inline"
                      onsubmit="return confirm('¿Inactivar unidad \'<%= f.nombre %>\'?')">
                  <input type="hidden" name="accion" value="eliminarUnidad">
                  <input type="hidden" name="id" value="<%= f.id %>">
                  <input type="hidden" name="tab" value="unidades">
                  <button type="submit" class="btn-fila btn-eliminar" title="Inactivar">
                    <i class="fi fi-sr-ban"></i>
                  </button>
                </form>
                <% } %>
              </td>
            </tr>
            <% } %>
          </tbody>
        </table>
        <% } %>
      </div>

      <!-- ══ PANEL MÉTODOS DE PAGO ══ -->
      <div class="panel" id="panel-metodos">
        <div class="panel__header">
          <i class="fi fi-sr-credit-card" style="color:white"></i>
          <span class="panel__titulo">Métodos de pago (<%= metodos != null ? metodos.size() : 0 %>)</span>
          <button class="btn-agregar" onclick="nuevoMet()">
            <i class="fi fi-sr-add"></i> Nuevo método
          </button>
        </div>
        <% if (metodos == null || metodos.isEmpty()) { %>
        <div class="cfg-vacio">No hay métodos de pago registrados.</div>
        <% } else { %>
        <table class="cfg-tabla">
          <thead><tr><th>#</th><th>Nombre</th><th>Estado</th><th></th></tr></thead>
          <tbody>
            <% for (Fila f : metodos) { %>
            <tr>
              <td class="td-id"><%= f.id %></td>
              <td><strong><%= f.nombre %></strong></td>
              <td><span class="<%= f.activo ? "badge-activo" : "badge-inactivo" %>"><%= f.activo ? "Activo" : "Inactivo" %></span></td>
              <td class="td-acciones">
                <button class="btn-fila btn-editar" title="Editar"
                        data-id="<%= f.id %>" data-nombre="<%= f.nombre %>"
                        data-activo="<%= f.activo %>"
                        onclick="editarMet(this)">
                  <i class="fi fi-sr-pencil"></i>
                </button>
                <% if (f.activo) { %>
                <form method="POST" action="<%= ctx %>/configuracion" style="display:inline"
                      onsubmit="return confirm('¿Inactivar método \'<%= f.nombre %>\'?')">
                  <input type="hidden" name="accion" value="eliminarMetodo">
                  <input type="hidden" name="id" value="<%= f.id %>">
                  <input type="hidden" name="tab" value="metodos">
                  <button type="submit" class="btn-fila btn-eliminar" title="Inactivar">
                    <i class="fi fi-sr-ban"></i>
                  </button>
                </form>
                <% } %>
              </td>
            </tr>
            <% } %>
          </tbody>
        </table>
        <% } %>
      </div>

    </div>
  </main>

  <!-- ══ MODAL CREAR/EDITAR CATEGORÍA ══ -->
  <div class="modal-overlay" id="modalCat">
    <div class="modal-caja">
      <div class="modal-titulo" id="modalCat-titulo"><i class="fi fi-sr-tags"></i> Nueva categoría</div>
      <form method="POST" action="<%= ctx %>/configuracion">
        <input type="hidden" name="accion" id="catAccion" value="crearCategoria">
        <input type="hidden" name="id"     id="catId"     value="">
        <input type="hidden" name="tab" value="categorias">
        <div class="campo">
          <label>Nombre *</label>
          <input type="text" name="nombre" id="catNombre" maxlength="100" required placeholder="Ej: Helados">
        </div>
        <div class="campo">
          <label>Descripción (opcional)</label>
          <textarea name="descripcion" id="catDesc" maxlength="150" placeholder="Descripción breve..."></textarea>
        </div>
        <div class="toggle-row" id="catActivoRow" style="display:none">
          <span class="toggle-label">Estado activo</span>
          <label class="toggle-switch">
            <input type="checkbox" name="activo" id="catActivo" value="1">
            <span class="toggle-slider"></span>
          </label>
        </div>
        <div class="modal-botones">
          <button type="submit" class="btn-guardar" id="catBtnGuardar"><i class="fi fi-sr-check"></i> Guardar</button>
          <button type="button" class="btn-cancelar" onclick="cerrarModal('modalCat')">Cancelar</button>
        </div>
      </form>
    </div>
  </div>

  <!-- ══ MODAL CREAR/EDITAR UNIDAD ══ -->
  <div class="modal-overlay" id="modalUni">
    <div class="modal-caja">
      <div class="modal-titulo" id="modalUni-titulo"><i class="fi fi-sr-ruler-combined"></i> Nueva unidad de medida</div>
      <form method="POST" action="<%= ctx %>/configuracion">
        <input type="hidden" name="accion" id="uniAccion" value="crearUnidad">
        <input type="hidden" name="id"     id="uniId"     value="">
        <input type="hidden" name="tab" value="unidades">
        <div class="campo">
          <label>Nombre *</label>
          <input type="text" name="nombre" id="uniNombre" maxlength="50" required placeholder="Ej: Kilogramos">
        </div>
        <div class="toggle-row" id="uniActivoRow" style="display:none">
          <span class="toggle-label">Estado activo</span>
          <label class="toggle-switch">
            <input type="checkbox" name="activo" id="uniActivo" value="1">
            <span class="toggle-slider"></span>
          </label>
        </div>
        <div class="modal-botones">
          <button type="submit" class="btn-guardar" id="uniBtnGuardar"><i class="fi fi-sr-check"></i> Guardar</button>
          <button type="button" class="btn-cancelar" onclick="cerrarModal('modalUni')">Cancelar</button>
        </div>
      </form>
    </div>
  </div>

  <!-- ══ MODAL CREAR/EDITAR MÉTODO ══ -->
  <div class="modal-overlay" id="modalMet">
    <div class="modal-caja">
      <div class="modal-titulo" id="modalMet-titulo"><i class="fi fi-sr-credit-card"></i> Nuevo método de pago</div>
      <form method="POST" action="<%= ctx %>/configuracion">
        <input type="hidden" name="accion" id="metAccion" value="crearMetodo">
        <input type="hidden" name="id"     id="metId"     value="">
        <input type="hidden" name="tab" value="metodos">
        <div class="campo">
          <label>Nombre *</label>
          <input type="text" name="nombre" id="metNombre" maxlength="50" required placeholder="Ej: Tarjeta débito">
        </div>
        <div class="toggle-row" id="metActivoRow" style="display:none">
          <span class="toggle-label">Estado activo</span>
          <label class="toggle-switch">
            <input type="checkbox" name="activo" id="metActivo" value="1">
            <span class="toggle-slider"></span>
          </label>
        </div>
        <div class="modal-botones">
          <button type="submit" class="btn-guardar" id="metBtnGuardar"><i class="fi fi-sr-check"></i> Guardar</button>
          <button type="button" class="btn-cancelar" onclick="cerrarModal('modalMet')">Cancelar</button>
        </div>
      </form>
    </div>
  </div>

<script>
// ── Tabs ───────────────────────────────────────────────────────
const paneles = ['categorias','unidades','metodos'];
function cambiarTab(id) {
  paneles.forEach(function(p) {
    document.getElementById('panel-' + p).style.display = (p === id) ? '' : 'none';
    var btn = document.getElementById('tab-' + p);
    if (btn) btn.classList.toggle('activo', p === id);
  });
}
(function() { cambiarTab('<%= tab %>'); })();

// ── Modales ────────────────────────────────────────────────────
function cerrarModal(id) { document.getElementById(id).classList.remove('activo'); }
document.querySelectorAll('.modal-overlay').forEach(function(m) {
  m.addEventListener('click', function(e) { if (e.target === this) this.classList.remove('activo'); });
});

function mostrarError(inp, msg) {
  var err = inp.parentElement.querySelector('.val-err');
  if (!err) { err = document.createElement('span'); err.className = 'val-err'; err.style.cssText='color:#e53935;font-size:12px;margin-top:3px;display:block'; inp.parentElement.appendChild(err); }
  err.textContent = msg; inp.style.borderColor = '#e53935';
}
function limpiarError(inp) {
  var err = inp.parentElement.querySelector('.val-err');
  if (err) err.textContent = ''; inp.style.borderColor = '';
}
function validarNombre(inp, max) {
  inp.addEventListener('input', function() {
    if (this.value.trim() === '') mostrarError(this, 'El nombre es obligatorio.');
    else if (this.value.length > max) mostrarError(this, 'Máximo ' + max + ' caracteres.');
    else limpiarError(this);
  });
  inp.addEventListener('blur', function() {
    if (this.value.trim() === '') mostrarError(this, 'El nombre es obligatorio.');
    else if (this.value.length > max) mostrarError(this, 'Máximo ' + max + ' caracteres.');
    else limpiarError(this);
  });
}
validarNombre(document.getElementById('catNombre'), 100);
validarNombre(document.getElementById('uniNombre'), 50);
validarNombre(document.getElementById('metNombre'), 50);

document.querySelectorAll('.modal-overlay form').forEach(function(form) {
  form.addEventListener('submit', function(e) {
    var ok = true;
    this.querySelectorAll('input[name="nombre"]').forEach(function(inp) {
      if (inp.value.trim() === '') { mostrarError(inp, 'El nombre es obligatorio.'); ok = false; }
    });
    if (!ok) e.preventDefault();
  });
});

// ── Nueva Categoría (limpia el modal) ──────────────────────────
function nuevaCat() {
  document.getElementById('catAccion').value  = 'crearCategoria';
  document.getElementById('catId').value       = '';
  document.getElementById('catNombre').value   = '';
  document.getElementById('catDesc').value     = '';
  document.getElementById('catActivoRow').style.display = 'none';
  limpiarError(document.getElementById('catNombre'));
  document.getElementById('modalCat-titulo').innerHTML = '<i class="fi fi-sr-tags"></i> Nueva categoría';
  document.getElementById('catBtnGuardar').className   = 'btn-guardar';
  document.getElementById('modalCat').classList.add('activo');
}
// ── Editar Categoría ───────────────────────────────────────────
function editarCat(btn) {
  document.getElementById('catAccion').value  = 'editarCategoria';
  document.getElementById('catId').value       = btn.dataset.id;
  document.getElementById('catNombre').value   = btn.dataset.nombre;
  document.getElementById('catDesc').value     = btn.dataset.desc || '';
  document.getElementById('catActivo').checked = btn.dataset.activo === 'true';
  document.getElementById('catActivoRow').style.display = '';
  limpiarError(document.getElementById('catNombre'));
  document.getElementById('modalCat-titulo').innerHTML = '<i class="fi fi-sr-pencil"></i> Editar categoría';
  document.getElementById('catBtnGuardar').className   = 'btn-guardar btn-guardar--edit';
  document.getElementById('modalCat').classList.add('activo');
}
// ── Nueva Unidad (limpia el modal) ─────────────────────────────
function nuevaUni() {
  document.getElementById('uniAccion').value  = 'crearUnidad';
  document.getElementById('uniId').value       = '';
  document.getElementById('uniNombre').value   = '';
  document.getElementById('uniActivoRow').style.display = 'none';
  limpiarError(document.getElementById('uniNombre'));
  document.getElementById('modalUni-titulo').innerHTML = '<i class="fi fi-sr-ruler-combined"></i> Nueva unidad de medida';
  document.getElementById('uniBtnGuardar').className   = 'btn-guardar';
  document.getElementById('modalUni').classList.add('activo');
}
// ── Editar Unidad ──────────────────────────────────────────────
function editarUni(btn) {
  document.getElementById('uniAccion').value  = 'editarUnidad';
  document.getElementById('uniId').value       = btn.dataset.id;
  document.getElementById('uniNombre').value   = btn.dataset.nombre;
  document.getElementById('uniActivo').checked = btn.dataset.activo === 'true';
  document.getElementById('uniActivoRow').style.display = '';
  limpiarError(document.getElementById('uniNombre'));
  document.getElementById('modalUni-titulo').innerHTML = '<i class="fi fi-sr-pencil"></i> Editar unidad';
  document.getElementById('uniBtnGuardar').className   = 'btn-guardar btn-guardar--edit';
  document.getElementById('modalUni').classList.add('activo');
}
// ── Nuevo Método (limpia el modal) ────────────────────────────
function nuevoMet() {
  document.getElementById('metAccion').value  = 'crearMetodo';
  document.getElementById('metId').value       = '';
  document.getElementById('metNombre').value   = '';
  document.getElementById('metActivoRow').style.display = 'none';
  limpiarError(document.getElementById('metNombre'));
  document.getElementById('modalMet-titulo').innerHTML = '<i class="fi fi-sr-credit-card"></i> Nuevo método de pago';
  document.getElementById('metBtnGuardar').className   = 'btn-guardar';
  document.getElementById('modalMet').classList.add('activo');
}
// ── Editar Método ──────────────────────────────────────────────
function editarMet(btn) {
  document.getElementById('metAccion').value  = 'editarMetodo';
  document.getElementById('metId').value       = btn.dataset.id;
  document.getElementById('metNombre').value   = btn.dataset.nombre;
  document.getElementById('metActivo').checked = btn.dataset.activo === 'true';
  document.getElementById('metActivoRow').style.display = '';
  limpiarError(document.getElementById('metNombre'));
  document.getElementById('modalMet-titulo').innerHTML = '<i class="fi fi-sr-pencil"></i> Editar método de pago';
  document.getElementById('metBtnGuardar').className   = 'btn-guardar btn-guardar--edit';
  document.getElementById('modalMet').classList.add('activo');
}
</script>
</body>
</html>
