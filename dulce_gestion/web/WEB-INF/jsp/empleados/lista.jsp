<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario, com.dulce_gestion.models.Emprendimiento, java.util.List" %>
<%
    Usuario sesionUsuario  = (Usuario) session.getAttribute("usuario");
    String ctx             = request.getContextPath();
    String rolSolicitante  = (String) request.getAttribute("rolSolicitante");
    List<Usuario> empleados = (List<Usuario>) request.getAttribute("empleados");
    List<Emprendimiento> emprendimientos = (List<Emprendimiento>) request.getAttribute("emprendimientos");
    Integer empFiltro      = (Integer) request.getAttribute("empFiltro");
    String errorEmpleados  = (String) request.getAttribute("errorEmpleados");

    boolean esSuperAdmin = "SuperAdministrador".equals(rolSolicitante);
    boolean esAdmin      = "Administrador".equals(rolSolicitante);
    int filtroActivo     = (empFiltro != null) ? empFiltro : 0;

    boolean exitoCreado    = "creado".equals(request.getParameter("exito"));
    boolean exitoEditado   = "editado".equals(request.getParameter("exito"));
    boolean exitoEliminado = "eliminado".equals(request.getParameter("exito"));
    String errorParam      = request.getParameter("error");

    // Nombre del emprendimiento: siempre desde la sesión (ya viene cargado desde el login)
    String nombreEmpAdmin = (esAdmin && sesionUsuario != null)
                            ? sesionUsuario.getNombreEmprendimiento()
                            : "";
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Empleados | Dulce Gestión</title>
  <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
</head>
<body class="layout-app">

  <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle" aria-label="Abrir menú">

  <!-- HEADER -->
  <header class="main-header header-app">
    <div class="header-app__izquierda">
      <label for="sidebar-toggle" class="header-app__btn-menu" aria-label="Abrir menú">
        <i class="fi fi-sr-menu-burger"></i>
      </label>
      <div class="header-app__marca">
        <img class="header-app__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo Dulce Gestión">
        <span class="header-app__titulo"><%= esAdmin ? nombreEmpAdmin : "Dulce Gestión" %></span>
      </div>
    </div>
    <div class="header-app__acciones">
      <a class="header-app__icono" href="<%= ctx %>/logout" title="Cerrar sesión">
        <i class="fi fi-sr-sign-out-alt"></i>
      </a>
    </div>
  </header>

  <label for="sidebar-toggle" class="sidebar__overlay" aria-label="Cerrar menú"></label>

  <!-- SIDEBAR -->
  <aside class="main-sidebar sidebar">
    <div class="sidebar__top">
      <div class="sidebar__brand">
        <img class="sidebar__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo">
        <span class="sidebar__brand-text">Menú</span>
      </div>
      <label for="sidebar-toggle" class="sidebar__cerrar">
        <i class="fi fi-sr-cross"></i>
      </label>
    </div>
    <nav class="sidebar__nav">
      <a class="sidebar__link" href="<%= ctx %>/dashboard">
        <i class="fi fi-sr-home"></i><span>Inicio</span>
      </a>
      <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/empleados">
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
      <a class="sidebar__link" href="<%= ctx %>/emprendimientos"><i class="fi fi-sr-store-alt"></i><span>Emprendimientos</span></a>
      <% if (esSuperAdmin) { %>
      <a class="sidebar__link" href="<%= ctx %>/configuracion"><i class="fi fi-sr-settings"></i><span>Configuración</span></a>
      <% } %>
      <a class="sidebar__link" href="<%= ctx %>/perfil">
        <i class="fi fi-sr-user"></i><span>Perfil</span>
      </a>
      <div class="sidebar__separador"></div>
      <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout">
        <i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesión</span>
      </a>
    </nav>
  </aside>

  <!-- MAIN -->
  <main class="pagina-main">
    <div class="modulo-contenido">

      <!-- Encabezado -->
      <div class="modulo-encabezado">
        <h1 class="modulo-titulo">
          <i class="fi fi-sr-users"></i>
          <% if (esSuperAdmin) { %>
            Administradores y Empleados
          <% } else { %>
            Empleados — <%= nombreEmpAdmin %>
          <% } %>
        </h1>
        <a class="boton boton--primario boton--sm" href="<%= ctx %>/empleados/nuevo">
          <i class="fi fi-sr-user-add"></i>
          Agregar empleado
        </a>
      </div>

      <!-- Mensajes de estado -->
      <% if (exitoCreado) { %>
      <div class="modulo-exito"><i class="fi fi-sr-check"></i> Usuario creado correctamente.</div>
      <% } %>
      <% if (exitoEditado) { %>
      <div class="modulo-exito"><i class="fi fi-sr-check"></i> Usuario actualizado correctamente.</div>
      <% } %>
      <% if (exitoEliminado) { %>
      <div class="modulo-exito"><i class="fi fi-sr-check"></i> Usuario inactivado correctamente.</div>
      <% } %>
      <% if ("eliminacion".equals(errorParam)) { %>
      <div class="modulo-error"><i class="fi fi-sr-triangle-warning"></i> No se pudo eliminar. El usuario tiene registros asociados.</div>
      <% } %>
      <% if ("sinpermiso".equals(errorParam)) { %>
      <div class="modulo-error"><i class="fi fi-sr-triangle-warning"></i> No tienes permiso para eliminar este usuario.</div>
      <% } %>
      <% if (errorEmpleados != null) { %>
      <div class="modulo-error"><i class="fi fi-sr-triangle-warning"></i> <%= errorEmpleados %></div>
      <% } %>

      <!-- FILTRO POR EMPRENDIMIENTO (solo SuperAdmin) -->
      <% if (esSuperAdmin && emprendimientos != null) { %>
      <div class="emp-filtro-select-wrap">
        <label class="emp-filtro-label" for="filtroEmp">
          <i class="fi fi-sr-store-alt"></i> Emprendimiento
        </label>
        <select id="filtroEmp" class="emp-filtro-select"
                onchange="window.location.href='<%= ctx %>/empleados' + (this.value ? '?emp=' + this.value : '')">
          <option value="" <%= filtroActivo == 0 ? "selected" : "" %>>— Todos los emprendimientos —</option>
          <% for (Emprendimiento emp : emprendimientos) { %>
          <option value="<%= emp.getId() %>" <%= filtroActivo == emp.getId() ? "selected" : "" %>>
            <%= emp.getNombre() %>
          </option>
          <% } %>
        </select>
      </div>
      <% } %>

      <!-- LISTA DE EMPLEADOS -->
      <% if (empleados == null || empleados.isEmpty()) { %>
      <div class="modulo-vacio">
        <i class="fi fi-sr-users modulo-vacio__icono"></i>
        <p>No hay empleados registrados<% if (esSuperAdmin && filtroActivo > 0) { %> en este emprendimiento<% } %>.</p>
      </div>
      <% } else {
           String empActual = "";
      %>
      <div class="empleados-lista">
        <% for (Usuario emp : empleados) {
             // Encabezado de grupo por emprendimiento (solo SuperAdmin viendo todos)
             if (esSuperAdmin && filtroActivo == 0) {
                 String nombreEmp = emp.getNombreEmprendimiento();
                 if (!nombreEmp.equals(empActual)) {
                     empActual = nombreEmp;
        %>
        <div class="emp-grupo-cabecera">
          <i class="fi fi-sr-store-alt"></i>
          <span><%= nombreEmp %></span>
        </div>
        <%       }
             }
        %>
        <div class="empleado-card">
          <div class="empleado-card__avatar">
            <i class="fi fi-sr-user"></i>
          </div>
          <div class="empleado-card__info">
            <span class="empleado-card__nombre"><%= emp.getNombreCompleto() %></span>
            <span class="empleado-card__rol empleado-card__rol--<%= emp.getNombreRol().toLowerCase().replace(" ","") %>">
              <%= emp.getNombreRol() %>
            </span>
            <span class="empleado-card__estado empleado-card__estado--<%= emp.getEstado().toLowerCase() %>">
              <%= emp.getEstado() %>
            </span>
          </div>
          <!-- Botón Ver Perfil -->
          <a class="empleado-card__btn-perfil"
             href="<%= ctx %>/perfil/ver?id=<%= emp.getId() %>"
             title="Ver Perfil"
             style="display:inline-flex;align-items:center;justify-content:center;background:#667eea;color:white;width:32px;height:32px;border-radius:4px;text-decoration:none;margin-right:6px;">
            <i class="fi fi-sr-user"></i>
          </a>
          <%-- SuperAdmin edita todos; Admin solo edita Empleados --%>
          <% if (esSuperAdmin || (esAdmin && "Empleado".equals(emp.getNombreRol()))) { %>
          <a class="empleado-card__btn-editar"
             href="<%= ctx %>/empleados/editar?id=<%= emp.getId() %>"
             title="Editar">
            <i class="fi fi-sr-edit"></i>
          </a>
          <% if (!"Inactivo".equals(emp.getEstado())) { %>
          <form method="POST" action="<%= ctx %>/empleados/eliminar" style="display:contents"
                onsubmit="return confirm('¿Seguro que deseas inactivar a <%= emp.getNombreCompleto() %>?')">
            <input type="hidden" name="id" value="<%= emp.getId() %>">
            <button type="submit" class="empleado-card__btn-eliminar" title="Inactivar">
              <i class="fi fi-sr-trash"></i>
            </button>
          </form>
          <% } %>
          <% } %>
        </div>
        <% } %>
      </div>
      <% } %>

      <!-- Botón agregar (footer móvil) -->
      <div class="modulo-accion-footer">
        <a class="boton boton--primario" href="<%= ctx %>/empleados/nuevo">
          <i class="fi fi-sr-user-add"></i>
          Agregar empleado
        </a>
      </div>

    </div>
  </main>

</body>
</html>
