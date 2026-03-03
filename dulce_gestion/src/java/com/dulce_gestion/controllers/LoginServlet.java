package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.UsuarioDAO;
import com.dulce_gestion.models.Usuario;
import java.sql.SQLException;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Si ya hay sesión activa, redirigir al dashboard (que resuelve el rol)
        HttpSession sesion = request.getSession(false);

        if (sesion != null && sesion.getAttribute("usuario") != null) {
            // FIX 3: redirigir a /dashboard en lugar de hardcodear /admin/dashboard
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String correoOUsuario = request.getParameter("correo");
        String contrasena     = request.getParameter("contrasena");

        if (correoOUsuario == null || contrasena == null ||
            correoOUsuario.isBlank() || contrasena.isBlank()) {

            request.setAttribute("errorLogin", "Debe completar todos los campos.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
                   .forward(request, response);
            return;
        }

        UsuarioDAO usuarioDAO = new UsuarioDAO();
        Usuario usuario = null;
        try {
            usuario = usuarioDAO.autenticar(correoOUsuario.trim(), contrasena);
        } catch (SQLException e) {
            e.printStackTrace();

            request.setAttribute("errorLogin", "Error al conectar con la base de datos.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
                   .forward(request, response);
            return;
        }

        if (usuario == null) {
            request.setAttribute("errorLogin", "Credenciales inválidas.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
                   .forward(request, response);
            return;
        }

        // Verificar que el usuario esté activo
        if (!"Activo".equals(usuario.getEstado())) {
            request.setAttribute("errorLogin", "Tu cuenta está inactiva. Contacta al administrador.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp")
                   .forward(request, response);
            return;
        }

        // Crear sesión
        HttpSession sesion = request.getSession(true);
        sesion.setAttribute("usuario", usuario);
        sesion.setMaxInactiveInterval(30 * 60); // 30 minutos

        // FIX 4: redirigir a /dashboard que resuelve el rol correctamente
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}
