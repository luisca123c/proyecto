<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.dulce_gestion.models.Usuario, java.util.List" %>
<%
    Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");
    String ctx = request.getContextPath();
    Usuario perfil = (Usuario) request.getAttribute("perfil");
    Boolean esOtroPerfil = (Boolean) request.getAttribute("esOtroPerfil");
    if (esOtroPerfil == null) esOtroPerfil = false;
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Perfil de <%= perfil.getNombreCompleto() %> | Dulce Gestión</title>
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
            <a class="sidebar__link sidebar__link--activo" href="<%= ctx %>/empleados">
                <i class="fi fi-sr-users"></i><span>Empleados</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/productos">
                <i class="fi fi-sr-box-open"></i><span>Productos</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/ventas">
                <i class="fi fi-sr-shopping-cart"></i><span>Carrito</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/historial">
                <i class="fi fi-sr-chart-histogram"></i><span>Ventas</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/gastos">
                <i class="fi fi-sr-receipt"></i><span>Gastos</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/compras"><i class="fi fi-sr-shop"></i><span>Compras</span></a>
            <a class="sidebar__link" href="<%= ctx %>/ganancias">
                <i class="fi fi-sr-chart-line-up"></i><span>Ganancias</span>
            </a>
            <a class="sidebar__link" href="<%= ctx %>/emprendimientos"><i class="fi fi-sr-store-alt"></i><span>Emprendimientos</span></a>
            <a class="sidebar__link" href="<%= ctx %>/perfil">
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
                        <h2 style="margin: 0 0 20px 0; font-size: 16px; font-weight: 600; color: #333; text-transform: uppercase; letter-spacing: 0.5px;">Información del Usuario</h2>

                        <div style="margin-bottom: 16px;">
                            <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #666; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;">Nombre Completo</label>
                            <p style="margin: 0; padding: 11px 14px; background: #f5f5f5; border-radius: 6px; color: #333; font-size: 13px;"><%= perfil.getNombreCompleto() %></p>
                        </div>

                        <div style="margin-bottom: 16px;">
                            <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #666; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;">Correo Electrónico</label>
                            <p style="margin: 0; padding: 11px 14px; background: #f5f5f5; border-radius: 6px; color: #333; font-size: 13px;"><%= perfil.getCorreo() %></p>
                        </div>

                        <div style="margin-bottom: 16px;">
                            <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #666; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;">Teléfono</label>
                            <p style="margin: 0; padding: 11px 14px; background: #f5f5f5; border-radius: 6px; color: #333; font-size: 13px;"><%= perfil.getTelefono() %></p>
                        </div>

                        <div style="margin-bottom: 16px;">
                            <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #666; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;">Género</label>
                            <p style="margin: 0; padding: 11px 14px; background: #f5f5f5; border-radius: 6px; color: #333; font-size: 13px;"><%= perfil.getGenero() %></p>
                        </div>

                        <div style="margin-bottom: 0;">
                            <label style="display: block; margin-bottom: 6px; font-weight: 600; color: #666; font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px;">Rol</label>
                            <p style="margin: 0; padding: 11px 14px; background: #f5f5f5; border-radius: 6px; color: #333; font-size: 13px;"><%= perfil.getNombreRol() %></p>
                        </div>

                        <div style="display: flex; gap: 12px; margin-top: 28px;">
                            <a href="<%= ctx %>/empleados" style="padding: 12px 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 8px; flex: 1; justify-content: center; text-transform: uppercase; letter-spacing: 0.5px;">
                                <i class="fi fi-sr-arrow-left"></i> Volver
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

</body>
</html>
