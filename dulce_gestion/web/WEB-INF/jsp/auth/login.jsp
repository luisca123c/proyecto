<%--
============================================================
JSP: login.jsp
RUTA: /WEB-INF/jsp/auth/login.jsp
PROPOSITO: Formulario de autenticación de usuarios
ACCESO: Público (sin autenticación requerida)
============================================================

Este JSP implementa la interfaz de inicio de sesión del sistema
con las siguientes características:

CARACTERÍSTICAS PRINCIPALES:
- Formulario de login con correo/usuario y contraseña
- Validación del lado del cliente con JavaScript
- Manejo de errores del servidor mediante atributos del request
- Diseño responsive y moderno con CSS personalizado
- Toggle para mostrar/ocultar contraseña
- Iconos de Flaticon para mejor UX

FLUJO DE FUNCIONAMIENTO:
1. El usuario ingresa credenciales en el formulario
2. JavaScript valida campos antes del envío
3. POST a /login (LoginServlet) procesa la autenticación
4. Si hay error, LoginServlet establece "errorLogin" en request
5. El JSP muestra el error mediante JavaScript dinámico
6. Si éxito, LoginServlet redirige al dashboard

COMPONENTES CLAVE:
- #formularioLogin: formulario principal de autenticación
- #contenedorErrorLogin: contenedor para mensajes de error
- #datosLogin: elemento data con mensaje de error del servidor
- .btn-toggle-pass: botón para mostrar/ocultar contraseña

SEGURIDAD:
- Campos con atributos autocomplete apropiados
- Validación del lado del servidor (obligatoria)
- CSRF implícito por el uso de sesiones de Java
- No se almacenan credenciales en el cliente

ESTILOS Y RECURSOS:
- styles.css: estilos personalizados del sistema
- uicons-solid-rounded: iconos de Flaticon
- login/index.js: validación y manejo de errores
- validaciones.js: funciones de validación reutilizables
--%>
<%@ page contentType="text/html; charset=UTF-8" %>
<!doctype html>
<html lang="es">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Iniciar Sesión | Dulce Gestión</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/styles.css">
  <link rel="stylesheet" href="https://cdn-uicons.flaticon.com/2.2.0/uicons-solid-rounded/css/uicons-solid-rounded.css">
</head>

<body>
  <div class="contenedor-principal">

    <!-- HEADER LOGIN - Logo y branding del sistema -->
    <header class="main-header main-header--login">
      <div class="header-app">
        <div class="header-app__marca">
          <img class="header-app__logo"
               src="${pageContext.request.contextPath}/assets/images/Logo.png"
               alt="Logo Dulce Gestión">
          <span class="header-app__titulo">Dulce Gestión</span>
        </div>
      </div>
    </header>

    <!-- MAIN - Contenido principal del formulario de login -->
    <main class="pagina-main">
      <section class="login">
        <div class="form-login-container">

          <!-- Logo principal del formulario -->
          <img src="${pageContext.request.contextPath}/assets/images/Logo.png"
               alt="Logo Dulce Gestión">

          <h1>Inicio de sesión</h1>

          <!-- Contenedor para errores del cliente (validación JavaScript) -->
          <div id="contenedorErrorLogin"
               class="login-error"
               hidden></div>

          <!-- Datos del servidor - contiene mensaje de error enviado por LoginServlet -->
          <!-- El atributo errorLogin se establece en LoginServlet cuando hay credenciales inválidas -->
          <div id="datosLogin"
               data-mensaje-error="<%= request.getAttribute("errorLogin") == null ? "" : request.getAttribute("errorLogin") %>"
               hidden></div>

          <!-- Formulario principal de autenticación -->
          <form id="formularioLogin"
                method="POST"
                action="${pageContext.request.contextPath}/login"
                novalidate>

            <!-- Campo de correo/usuario -->
            <div class="form-group">
              <label for="correo">Correo o usuario:</label>
              <div class="input-wrapper" style="position:relative;">
                <i class="fi fi-sr-envelope"></i>
                <input type="text"
                       id="correo"
                       name="correo"
                       placeholder="Ingresa el correo o usuario"
                       autocomplete="email"
                       required>
              </div>
            </div>

            <!-- Campo de contraseña con toggle para mostrar/ocultar -->
            <div class="form-group">
              <label for="contrasena">Contraseña:</label>
              <div class="input-wrapper" style="position:relative;">
                <i class="fi fi-sr-lock"></i>
                <input type="password"
                       id="contrasena"
                       name="contrasena"
                       placeholder="Ingresa la contraseña"
                       autocomplete="current-password"
                       required>
                <!-- Botón para mostrar/ocultar contraseña -->
                <button type="button" class="btn-toggle-pass" onclick="togglePass(this)" title="Mostrar/ocultar contraseña">
                  <i class="fi fi-sr-eye"></i>
                </button>
              </div>
            </div>

            <!-- Botón de envío del formulario -->
            <button id="btnLogin"
                    type="submit"
                    class="boton boton--primario">
              Iniciar sesión
            </button>

          </form>

        </div>
      </section>
    </main>

    <!-- FOOTER LOGIN - Información de copyright -->
    <footer class="main-footer">
      <p class="main-footer__texto">
        &copy; 2025 Dulce Gestión &mdash; Todos los derechos reservados
      </p>
    </footer>

  </div>

  <!-- Scripts JavaScript para validación y funcionalidad -->
  <!-- login/index.js: módulo principal de validación del formulario -->
  <script type="module"
          src="${pageContext.request.contextPath}/assets/js/login/index.js">
  </script>
  <!-- validaciones.js: funciones de validación reutilizables en todo el sistema -->
  <script src="${pageContext.request.contextPath}/assets/js/validaciones.js" defer></script>

</body>
</html>
