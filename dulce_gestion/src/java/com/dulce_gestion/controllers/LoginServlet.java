package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.UsuarioDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Servlet que maneja la autenticación.
 * GET  /login → muestra el formulario
 * POST /login → procesa las credenciales
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VISTA_LOGIN    = "/WEB-INF/jsp/auth/login.jsp";
    private static final String RUTA_DASHBOARD = "/dashboard";

    // ── GET: mostrar formulario ───────────────────────────────

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Si ya hay sesión activa, ir directo al dashboard
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("usuario") != null) {
            response.sendRedirect(request.getContextPath() + RUTA_DASHBOARD);
            return;
        }

        request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
    }

    // ── POST: procesar credenciales ───────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String correo     = request.getParameter("correo");
        String contrasena = request.getParameter("contrasena");

        // Validación básica server-side
        if (correo == null || correo.isBlank() ||
            contrasena == null || contrasena.isBlank()) {
            reenviarConError(request, response, "Completa todos los campos.");
            return;
        }

        try {
            UsuarioDAO dao = new UsuarioDAO();
            Usuario usuario = dao.autenticar(correo, contrasena);

            if (usuario == null) {
                reenviarConError(request, response, "Correo o contraseña incorrectos.");
                return;
            }

            if (!"Activo".equalsIgnoreCase(usuario.getEstado())) {
                reenviarConError(request, response, "Tu cuenta está desactivada. Contacta al administrador.");
                return;
            }

            // Login exitoso: crear sesión
            HttpSession session = request.getSession(true);
            session.setAttribute("usuario", usuario);
            session.setMaxInactiveInterval(30 * 60); // 30 minutos

            response.sendRedirect(request.getContextPath() + RUTA_DASHBOARD);

        } catch (SQLException e) {
            getServletContext().log("Error de BD en LoginServlet", e);
            reenviarConError(request, response, "Error interno del servidor. Intenta más tarde.");
        }
    }

    // ── Utilidad ─────────────────────────────────────────────

    private void reenviarConError(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String mensaje)
            throws ServletException, IOException {

        request.setAttribute("errorLogin", mensaje);
        request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
    }
}
