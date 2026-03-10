package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.UsuarioDAO;
import com.dulce_gestion.models.Usuario;

import java.sql.SQLException;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * ============================================================
 * SERVLET: LoginServlet
 * URL:     /login
 * MÉTODOS: GET, POST
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Es la puerta de entrada al sistema. Gestiona dos cosas:
 *   GET  → muestra el formulario de inicio de sesión
 *   POST → recibe las credenciales, las valida y crea la sesión
 *
 * ¿QUÉ ES LA SESIÓN HTTP?
 * ------------------------
 * HTTP es un protocolo sin estado: cada petición es completamente
 * independiente y el servidor no recuerda al usuario entre peticiones.
 *
 * La sesión HTTP agrega "memoria" al servidor:
 *   - El servidor crea un objeto HttpSession con un ID único (JSESSIONID).
 *   - Envía ese ID al navegador como cookie.
 *   - En cada petición siguiente, el navegador manda la cookie.
 *   - El servidor reconoce el ID y carga los datos del usuario.
 *
 * En este sistema, se guarda el objeto Usuario en la sesión:
 *   session.setAttribute("usuario", usuario)
 *
 * FiltroAutenticacion verifica en CADA petición que exista ese atributo.
 *
 * ¿POR QUÉ getSession(true) EN EL POST?
 * ----------------------------------------
 * true = crear una sesión nueva si no existe.
 * Se usa solo DESPUÉS de verificar credenciales correctas.
 * Antes de eso, usamos getSession(false) para no crear sesiones vacías.
 *
 * ¿POR QUÉ setMaxInactiveInterval(30 * 60)?
 * -------------------------------------------
 * Después de 30 minutos de inactividad (sin peticiones del usuario),
 * Tomcat destruye la sesión automáticamente. Así, si el usuario deja
 * el navegador abierto y se aleja, la sesión expira sola.
 *
 * ¿FORWARD VS REDIRECT EN EL POST?
 * ----------------------------------
 * En caso de ERROR → usamos forward al JSP del login.
 *   El forward preserva los atributos del request (el mensaje de error).
 *   Si usáramos redirect, los atributos se perderían.
 *
 * En caso de ÉXITO → usamos redirect a /dashboard.
 *   El redirect hace que el navegador haga una petición GET nueva.
 *   Esto evita el problema de "¿volver a enviar el formulario?"
 *   que aparece si el usuario recarga la página después de un POST exitoso.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // =========================================================
    // GET /login
    // =========================================================

    /**
     * Muestra el formulario de inicio de sesión.
     *
     * Si el usuario YA tiene sesión activa (ya se autenticó antes),
     * no tiene sentido mostrarle el login — lo enviamos al dashboard.
     *
     * @param request  contiene la petición HTTP y posible sesión activa
     * @param response para redirigir o enviar la respuesta al navegador
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Buscar sesión existente SIN crear una nueva (false)
        HttpSession sesion = request.getSession(false);

        // Si ya hay sesión activa con usuario, redirigir al dashboard
        // (no mostrar el login a alguien que ya está autenticado)
        if (sesion != null && sesion.getAttribute("usuario") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        // Sin sesión activa → mostrar el formulario de login
        request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
               .forward(request, response);
    }

    // =========================================================
    // POST /login
    // =========================================================

    /**
     * Procesa las credenciales del formulario de inicio de sesión.
     *
     * FLUJO PASO A PASO:
     *
     * Paso 1 — Leer parámetros del formulario:
     *   request.getParameter() lee los campos del form HTML.
     *   El campo se llama "correo" y "contrasena" según el name= del input.
     *
     * Paso 2 — Validar que no estén vacíos:
     *   Si algún campo llegó vacío o null, no tiene sentido consultar la BD.
     *   Se pone el error como atributo y se reenvía al formulario.
     *
     * Paso 3 — Consultar la BD:
     *   UsuarioDAO.autenticar() hashea la contraseña y la compara con la BD.
     *   Retorna null si las credenciales son incorrectas.
     *
     * Paso 4 — Verificar que la cuenta esté activa:
     *   Un usuario puede existir pero estar en estado "Inactivo".
     *   En ese caso, no se permite el acceso aunque la contraseña sea correcta.
     *
     * Paso 5 — Crear la sesión:
     *   getSession(true) crea una sesión nueva (o retorna la existente).
     *   setAttribute("usuario", usuario) guarda el objeto en la sesión.
     *   setMaxInactiveInterval expira la sesión tras 30 min de inactividad.
     *
     * Paso 6 — Redirigir al dashboard:
     *   DashboardServlet se encarga de enviar al JSP correcto según el rol.
     *
     * @param request  contiene los parámetros del formulario (correo, contrasena)
     * @param response para redirigir en éxito o reenviar al form en error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: leer los campos del formulario ──────────────────────
        String correoOUsuario = request.getParameter("correo");    // campo name="correo"
        String contrasena     = request.getParameter("contrasena"); // campo name="contrasena"

        // ── Paso 2: validar que ambos campos tengan contenido ───────────
        if (correoOUsuario == null || contrasena == null ||
            correoOUsuario.isBlank() || contrasena.isBlank()) {

            // Poner mensaje de error en el request para que el JSP lo muestre
            request.setAttribute("errorLogin", "Debe completar todos los campos.");
            // Forward conserva los atributos del request; redirect los perdería
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
                   .forward(request, response);
            return; // Detener ejecución — no consultar la BD con datos vacíos
        }

        // ── Paso 3: consultar la BD con las credenciales ────────────────
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        Usuario usuario = null;
        try {
            // autenticar() hashea contrasena internamente y la compara en BD.
            // .trim() elimina espacios accidentales al inicio y al final del correo.
            usuario = usuarioDAO.autenticar(correoOUsuario.trim(), contrasena);

        } catch (SQLException e) {
            e.printStackTrace(); // Imprimir traza en la consola de Tomcat para debugging
            request.setAttribute("errorLogin", "Error al conectar con la base de datos.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
                   .forward(request, response);
            return;
        }

        // ── Paso 3b: null significa credenciales incorrectas ────────────
        if (usuario == null) {
            request.setAttribute("errorLogin", "Credenciales inválidas.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
                   .forward(request, response);
            return;
        }

        // ── Paso 4: verificar que la cuenta esté activa ─────────────────
        // Un admin puede desactivar una cuenta sin eliminarla.
        // Un usuario inactivo no debe poder entrar aunque tenga la contraseña correcta.
        if (!"Activo".equals(usuario.getEstado())) {
            request.setAttribute("errorLogin", "Tu cuenta está inactiva. Contacta al administrador.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
                   .forward(request, response);
            return;
        }

        // ── Paso 5: crear la sesión con el usuario autenticado ──────────
        // getSession(true): crear sesión nueva si no existe, o retornar la existente
        HttpSession sesion = request.getSession(true);
        sesion.setAttribute("usuario", usuario);        // Guardar el objeto Usuario en la sesión
        sesion.setMaxInactiveInterval(30 * 60);         // Expirar tras 30 minutos de inactividad

        // ── Paso 6: redirigir al dashboard (que resuelve la vista por rol) ─
        // Usamos redirect (no forward) para que el navegador haga un GET limpio.
        // Esto evita el problema del "¿volver a enviar el formulario?" al recargar.
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}
