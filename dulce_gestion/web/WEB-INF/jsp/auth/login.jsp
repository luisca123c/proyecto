<%@ page contentType="text/html; charset=UTF-8" %>
<!doctype html>
<html lang="es">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Iniciar Sesión | Dulce Gestión</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/styles.css">
</head>

<body>
  <div class="contenedor-principal">

    <!-- HEADER -->
    <header class="main-header main-header--login">
      <div style="display:flex; align-items:center; gap:12px; padding:15px;">
        <img src="${pageContext.request.contextPath}/assets/images/Logo.png"
             alt="Logo Dulce Gestión"
             style="width:40px; height:40px; object-fit:contain;">
        <strong style="font-size:18px;">Dulce Gestión</strong>
      </div>
    </header>

    <!-- MAIN -->
    <main class="pagina-main">
      <section class="login">
        <div class="form-login-container">

          <img src="${pageContext.request.contextPath}/assets/images/Logo.png"
               alt="Logo Dulce Gestión">

          <h1>Inicio de sesión</h1>

          <form id="formularioLogin" method="POST"
                action="${pageContext.request.contextPath}/login">

            <div class="form-group">
              <label for="correo">Correo:</label>
              <div class="input-wrapper">
                <input type="email" name="correo" id="correo"
                       placeholder="Ingresa el correo" required
                       value="${not empty param.correo ? param.correo : ''}">
              </div>
            </div>

            <div class="form-group">
              <label for="contrasena">Contraseña:</label>
              <div class="input-wrapper">
                <input type="password" name="contrasena" id="contrasena"
                       placeholder="Ingresa la contraseña" required>
              </div>
            </div>

            <button type="submit" id="btnLogin" class="boton">
              Iniciar sesión
            </button>

          </form>

          <%-- Error del servidor --%>
          <% String errorLogin = (String) request.getAttribute("errorLogin"); %>
          <div id="contenedorErrorLogin" class="login-error"
               <%= errorLogin == null ? "hidden" : "" %>>
            <%= errorLogin != null ? errorLogin : "" %>
          </div>

        </div>
      </section>
    </main>

    <!-- FOOTER -->
    <footer class="main-footer">
      <p class="main-footer__texto">© 2026 Dulce Gestión</p>
    </footer>

  </div>

  <script type="module"
          src="${pageContext.request.contextPath}/assets/js/login/index.js">
  </script>
</body>

</html>
