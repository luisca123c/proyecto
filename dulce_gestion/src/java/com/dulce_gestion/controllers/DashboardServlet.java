package com.dulce_gestion.controllers;

import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Servlet para /dashboard
 * Lee el rol del usuario en sesión y hace forward al JSP correspondiente.
 *
 *   SuperAdministrador → /WEB-INF/jsp/superadmin/dashboard.jsp
 *   Administrador      → /WEB-INF/jsp/admin/dashboard.jsp
 *   Empleado           → /WEB-INF/jsp/empleado/dashboard.jsp
 */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final String VISTA_SUPERADMIN = "/WEB-INF/jsp/superadmin/dashboard.jsp";
    private static final String VISTA_ADMIN      = "/WEB-INF/jsp/admin/dashboard.jsp";
    private static final String VISTA_EMPLEADO   = "/WEB-INF/jsp/empleado/dashboard.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String vista = switch (usuario.getNombreRol()) {
            case "SuperAdministrador" -> VISTA_SUPERADMIN;
            case "Empleado"           -> VISTA_EMPLEADO;
            default                   -> VISTA_ADMIN;
        };

        request.getRequestDispatcher(vista).forward(request, response);
    }
}
