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
