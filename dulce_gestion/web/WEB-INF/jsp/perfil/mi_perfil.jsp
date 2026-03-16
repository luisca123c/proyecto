<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario, java.util.List" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    Usuario perfil = (Usuario) request.getAttribute("perfil");
    List<String[]> generos = (List<String[]>) request.getAttribute("generos");
    String error = (String) request.getAttribute("error");
    String exito = request.getParameter("exito");
    
    String rolSolicitante = sesionUsuario.getNombreRol();
    boolean puedeEditar = "SuperAdministrador".equals(rolSolicitante) || "Administrador".equals(rolSolicitante);
    boolean esEmpleado = "Empleado".equals(rolSolicitante);
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Mi Perfil | Dulce Gestión</title>
    <link rel="stylesheet" href="<%= ctx %>/assets/css/styles.css">
    <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
</head>
<body class="layout-app">

    <input type="checkbox" id="sidebar-toggle" class="sidebar__toggle" aria-label="Abrir menu">

    <!-- HEADER -->
    <header class="main-header header-app">
        <div class="header-app__izquierda">
            <label for="sidebar-toggle" class="header-app__btn-menu" aria-label="Abrir menu">
                <i class="fi fi-sr-menu-burger"></i>
            </label>
            <div class="header-app__marca">
                <img class="header-app__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo Dulce Gestion">
                <span class="header-app__titulo">Dulce Gestion</span>
            </div>
        </div>
        <div class="header-app__acciones">
            <a class="header-app__icono" href="<%= ctx %>/logout" title="Cerrar sesion">
                <i class="fi fi-sr-sign-out-alt"></i>
            </a>
        </div>
    </header>

    <label for="sidebar-toggle" class="sidebar__overlay" aria-label="Cerrar menu"></label>

    <!-- SIDEBAR -->
    <aside class="main-sidebar sidebar">
        <div class="sidebar__top">
            <div class="sidebar__brand">
                <img class="sidebar__logo" src="<%= ctx %>/assets/images/Logo.png" alt="Logo">
                <span class="sidebar__brand-text">Menu</span>
            </div>
            <label for="sidebar-toggle" class="sidebar__cerrar">
                <i class="fi fi-sr-cross"></i>
            </label>
        </div>
        <nav class="sidebar__nav">
            <a class="sidebar__link" href="<%= ctx %>/dashboard">
                <i class="fi fi-sr-home"></i><span>Inicio</span>
            </a>
            <% if (puedeEditar) { %>
            <a class="sidebar__link" href="<%= ctx %>/empleados">
                <i class="fi fi-sr-users"></i><span>Empleados</span>
            </a>
            <% } %>
            <a class="sidebar__link" href="<%= ctx %>/productos">
                <i class="fi fi-sr-box-open"></i><span>Productos</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/ventas">
                <i class="fi fi-sr-shopping-cart"></i><span>Carrito</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/historial">
                <i class="fi fi-sr-chart-histogram"></i><span>Ventas</span>
            </a>
            <% if (puedeEditar) { %>
            <a class="sidebar__link" href="<%= ctx %>/gastos">
                <i class="fi fi-sr-receipt"></i><span>Gastos</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
            <a class="sidebar__link" href="<%= ctx %>/ganancias">
                <i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span>
            </a>
            <% } %>
            <% if ("SuperAdministrador".equals(rolSolicitante)) { %>
            <a class="sidebar__link" href="<%= ctx %>/configuracion"><i class="fi fi-sr-settings"></i><span>Configuración</span></a>
            <% } %>
            <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/perfil">
                <i class="fi fi-sr-user"></i><span>Perfil</span>
            </a>
            <div class="sidebar__separador"></div>
            <a class="sidebar__link sidebar__link--salir" href="<%= ctx %>/logout">
                <i class="fi fi-sr-sign-out-alt"></i><span>Cerrar sesion</span>
            </a>
        </nav>
    </aside>

    <!-- MAIN -->
    <main class="pagina-main">
        <div class="container-pagina">
            <div style="max-width: 600px; margin: 30px auto;">
                <div style="background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    
                    <!-- HEADER PERFIL -->
                    <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 30px; text-align: center; color: white;">
                        <div style="width: 90px; height: 90px; background: rgba(255,255,255,0.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 40px; margin: 0 auto 20px;">
                            <i class="fi fi-sr-user"></i>
                        </div>
                        <h1 style="margin: 0 0 8px 0; font-size: 24px; font-weight: 700;"><%= perfil.getNombreCompleto() %></h1>
                        <p style="margin: 4px 0; font-size: 13px; opacity: 0.95;"><%= perfil.getCorreo() %></p>
                        <p style="margin: 4px 0; font-size: 13px; opacity: 0.95;"><%= perfil.getTelefono() %></p>
                        <p style="margin: 6px 0 0 0; font-size: 12px; opacity: 0.9;">Género: <%= perfil.getGenero() %></p>
                        <span style="display: inline-block; background: rgba(255,255,255,0.25); color: white; padding: 6px 14px; border-radius: 12px; font-size: 11px; font-weight: 700; text-transform: uppercase; margin-top: 12px;"><%= perfil.getNombreRol() %></span>
                    </div>

                    <!-- BODY PERFIL -->
                    <div style="padding: 30px;">
                        
                        <!-- MENSAJES -->
                        <% if (error != null && !error.isEmpty()) { %>
                            <div style="padding: 12px 16px; border-radius: 6px; background: #ffebee; border-left: 4px solid #f44336; color: #c62828; margin-bottom: 18px; display: flex; align-items: center; gap: 10px; font-size: 13px;">
                                <i class="fi fi-sr-exclamation"></i>
                                <span><%= error %></span>
                            </div>
                        <% } %>

                        <% if ("actualizado".equals(exito)) { %>
                            <div style="padding: 12px 16px; border-radius: 6px; background: #e8f5e9; border-left: 4px solid #4caf50; color: #2e7d32; margin-bottom: 18px; display: flex; align-items: center; gap: 10px; font-size: 13px;">
                                <i class="fi fi-sr-check-circle"></i>
                                <span>Perfil actualizado correctamente</span>
                            </div>
                        <% } %>

                        <% if ("contrasenna_cambiada".equals(exito)) { %>
                            <div style="padding: 12px 16px; border-radius: 6px; background: #e8f5e9; border-left: 4px solid #4caf50; color: #2e7d32; margin-bottom: 18px; display: flex; align-items: center; gap: 10px; font-size: 13px;">
                                <i class="fi fi-sr-check-circle"></i>
                                <span>Contraseña cambiada correctamente</span>
                            </div>
                        <% } %>

                        <!-- TABS -->
                        <div style="display: flex; gap: 5px; margin-bottom: 25px; border-bottom: 2px solid #e0e0e0; padding-bottom: 0;">
                            <button class="tab-btn active" onclick="mostrarTab('datos', event)" style="padding: 12px 20px; background: none; border: none; border-bottom: 3px solid transparent; cursor: pointer; font-weight: 600; color: #999; transition: all 0.3s ease; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;">
                                Editar Información
                            </button>
                            <button class="tab-btn" onclick="mostrarTab('seguridad', event)" style="padding: 12px 20px; background: none; border: none; border-bottom: 3px solid transparent; cursor: pointer; font-weight: 600; color: #999; transition: all 0.3s ease; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;">
                                Cambiar Contraseña
                            </button>
                        </div>

                        <!-- TAB 1: DATOS -->
                        <div id="tab-datos" style="display: block;">
                            <form method="POST" action="<%= ctx %>/perfil/editar">
                                <input type="hidden" name="tipo" value="datos">



                                <div style="margin-bottom: 18px;">
                                    <label style="display: block; margin-bottom: 8px; font-weight: 600; color: #333; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Nombre Completo</label>
                                    <input type="text" name="nombreCompleto" maxlength="100" value="<%= perfil.getNombreCompleto() %>"
                                        <%= esEmpleado ? "readonly style=\"width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #888; font-size: 13px; background:#f5f5f5; cursor:not-allowed;\"" : "required style=\"width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #333; font-size: 13px;\"" %>>
                                </div>

                                <div style="margin-bottom: 18px;">
                                    <label style="display: block; margin-bottom: 8px; font-weight: 600; color: #333; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Correo Electrónico</label>
                                    <input type="email" name="correo" maxlength="100" value="<%= perfil.getCorreo() %>" required style="width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #333; font-size: 13px; transition: all 0.3s ease;">
                                </div>

                                <div style="margin-bottom: 18px;">
                                    <label style="display: block; margin-bottom: 8px; font-weight: 600; color: #333; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Teléfono</label>
                                    <input type="tel" name="telefono" maxlength="10" value="<%= perfil.getTelefono() %>" required style="width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #333; font-size: 13px; transition: all 0.3s ease;">
                                </div>

                                <div style="margin-bottom: 18px;">
                                    <label style="display: block; margin-bottom: 8px; font-weight: 600; color: #333; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Género</label>
                                    <% if (esEmpleado) { %>
                                        <input type="text" value="<%= perfil.getGenero() %>" readonly
                                            style="width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #888; font-size: 13px; background:#f5f5f5; cursor:not-allowed;">
                                        <input type="hidden" name="nombreCompleto" value="<%= perfil.getNombreCompleto() %>">
                                        <input type="hidden" name="idGenero" value="<%= perfil.getGenero() %>">
                                    <% } else { %>
                                        <select name="idGenero" required style="width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #333; font-size: 13px;">
                                            <% for (String[] genero : generos) { %>
                                                <option value="<%= genero[0] %>" <%= genero[1].equals(perfil.getGenero()) ? "selected" : "" %>>
                                                    <%= genero[1] %>
                                                </option>
                                            <% } %>
                                        </select>
                                    <% } %>
                                </div>

                                <div style="display: flex; gap: 12px; margin-top: 28px;">
                                    <button type="submit" style="padding: 12px 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.3s ease; text-decoration: none; display: inline-flex; align-items: center; gap: 8px; flex: 1; justify-content: center; text-transform: uppercase; letter-spacing: 0.5px;">
                                        <i class="fi fi-sr-disk"></i> Guardar
                                    </button>
                                    <a href="<%= ctx %>/perfil" style="padding: 12px 20px; background: #f0f0f0; color: #333; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 8px; flex: 1; justify-content: center; text-transform: uppercase; letter-spacing: 0.5px;">
                                        <i class="fi fi-sr-cross"></i> Cancelar
                                    </a>
                                </div>
                            </form>
                        </div>

                        <!-- TAB 2: CONTRASEÑA -->
                        <div id="tab-seguridad" style="display: none;">
                            <form method="POST" action="<%= ctx %>/perfil/editar" id="formContrasena">
                                <input type="hidden" name="tipo" value="contrasena">

                                <div style="margin-bottom: 18px;">
                                    <label style="display: block; margin-bottom: 8px; font-weight: 600; color: #333; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Contraseña Actual</label>
                                    <div style="position:relative;">
                                        <input type="password" name="contrasennaActual" required style="width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #333; font-size: 13px;">
                                        <button type="button" onclick="togglePass(this)" style="position:absolute;right:10px;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;color:#aaa;padding:4px;display:flex;align-items:center;" title="Mostrar/ocultar">
                                          <i class="fi fi-sr-eye"></i>
                                        </button>
                                    </div>
                                </div>

                                <div style="margin-bottom: 18px;">
                                    <label style="display: block; margin-bottom: 8px; font-weight: 600; color: #333; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Contraseña Nueva</label>
                                    <div style="position:relative;">
                                        <input type="password" name="contrasennaNueva" required style="width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #333; font-size: 13px;">
                                        <button type="button" onclick="togglePass(this)" style="position:absolute;right:10px;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;color:#aaa;padding:4px;display:flex;align-items:center;" title="Mostrar/ocultar">
                                          <i class="fi fi-sr-eye"></i>
                                        </button>
                                    </div>
                                </div>

                                <div style="margin-bottom: 18px;">
                                    <label style="display: block; margin-bottom: 8px; font-weight: 600; color: #333; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Confirmar Contraseña Nueva</label>
                                    <div style="position:relative;">
                                        <input type="password" name="contrasennaNuevaConfirm" required style="width: 100%; padding: 11px 14px; border: 1px solid #ddd; border-radius: 6px; color: #333; font-size: 13px;">
                                        <button type="button" onclick="togglePass(this)" style="position:absolute;right:10px;top:50%;transform:translateY(-50%);background:none;border:none;cursor:pointer;color:#aaa;padding:4px;display:flex;align-items:center;" title="Mostrar/ocultar">
                                          <i class="fi fi-sr-eye"></i>
                                        </button>
                                    </div>
                                </div>

                                <div style="display: flex; gap: 12px; margin-top: 28px;">
                                    <button type="submit" style="padding: 12px 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.3s ease; text-decoration: none; display: inline-flex; align-items: center; gap: 8px; flex: 1; justify-content: center; text-transform: uppercase; letter-spacing: 0.5px;">
                                        <i class="fi fi-sr-lock"></i> Cambiar
                                    </button>
                                    <a href="<%= ctx %>/perfil" style="padding: 12px 20px; background: #f0f0f0; color: #333; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 8px; flex: 1; justify-content: center; text-transform: uppercase; letter-spacing: 0.5px;">
                                        <i class="fi fi-sr-cross"></i> Cancelar
                                    </a>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

</body>
<script src="<%= ctx %>/assets/js/perfil/mi_perfil.js" defer></script>
<script src="<%= ctx %>/assets/js/validaciones.js" defer></script>
</html>
