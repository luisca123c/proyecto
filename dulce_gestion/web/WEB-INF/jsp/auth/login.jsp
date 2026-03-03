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

    <!-- HEADER LOGIN -->
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

    <!-- MAIN -->
    <main class="pagina-main">
      <section class="login">
        <div class="form-login-container">

          <img src="${pageContext.request.contextPath}/assets/images/Logo.png"
               alt="Logo Dulce Gestión">

          <h1>Inicio de sesión</h1>

          <!-- Error del servidor -->
          <div id="contenedorErrorLogin"
               class="login-error"
               hidden></div>

          <!-- Datos del servidor (mensaje de error) -->
          <div id="datosLogin"
               data-mensaje-error="<%= request.getAttribute("errorLogin") == null ? "" : request.getAttribute("errorLogin") %>"
               hidden></div>

          <form id="formularioLogin"
                method="POST"
                action="${pageContext.request.contextPath}/login"
                novalidate>

            <div class="form-group">
              <label for="correo">Correo o usuario:</label>
              <div class="input-wrapper">
                <i class="fi fi-sr-envelope"></i>
                <input type="text"
                       id="correo"
                       name="correo"
                       placeholder="Ingresa el correo o usuario"
                       autocomplete="email"
                       required>
              </div>
            </div>

            <div class="form-group">
              <label for="contrasena">Contraseña:</label>
              <div class="input-wrapper">
                <i class="fi fi-sr-lock"></i>
                <input type="password"
                       id="contrasena"
                       name="contrasena"
                       placeholder="Ingresa la contraseña"
                       autocomplete="current-password"
                       required>
              </div>
            </div>

            <button id="btnLogin"
                    type="submit"
                    class="boton boton--primario">
              Iniciar sesión
            </button>

          </form>

        </div>
      </section>
    </main>

    <!-- FOOTER LOGIN -->
    <footer class="main-footer">
      <p class="main-footer__texto">
        &copy; 2025 Dulce Gestión &mdash; Todos los derechos reservados
      </p>
    </footer>

  </div>

  <script type="module"
          src="${pageContext.request.contextPath}/assets/js/login/index.js">
  </script>

</body>
</html>
