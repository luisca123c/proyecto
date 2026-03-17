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
 * ============================================================
 * SERVLET: DashboardServlet
 * URL:     /dashboard
 * MÉTODOS: GET
 * ============================================================
 *
 */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    // =========================================================
    // CONSTANTES: rutas internas de los JSPs
    // Están en /WEB-INF/ para que no sean accesibles directamente
    // por el navegador — solo se pueden servir mediante forward.
    // =========================================================

    /** JSP del panel de control del SuperAdministrador */
    private static final String VISTA_SUPERADMIN = "/WEB-INF/jsp/superadmin/dashboard.jsp";

    /** JSP del panel de control del Administrador */
    private static final String VISTA_ADMIN      = "/WEB-INF/jsp/admin/dashboard.jsp";

    /** JSP del panel de control del Empleado */
    private static final String VISTA_EMPLEADO   = "/WEB-INF/jsp/empleado/dashboard.jsp";

    // =========================================================
    // GET /dashboard
    // =========================================================

    /**
     * Determina el rol del usuario en sesión y hace forward
     * al JSP de dashboard correspondiente.
     *
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // getSession(false) busca la sesión existente SIN crear una nueva.
        // Si el usuario no tiene sesión, retorna null.
        HttpSession session = request.getSession(false);

        // Extraer el usuario de la sesión. Si session es null, el operador
        // ternario retorna null sin lanzar NullPointerException.
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        // Seguridad adicional: si no hay usuario, redirigir al login.
        // (El filtro ya debería haberlo bloqueado antes de llegar aquí)
        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return; // Detener la ejecución para que no continúe al forward
        }

        // Switch expression: asigna la vista según el rol del usuario.
        // getNombreRol() retorna "SuperAdministrador", "Administrador" o "Empleado".
        String vista = switch (usuario.getNombreRol()) {
            case "SuperAdministrador" -> VISTA_SUPERADMIN; // Panel completo con gestión de admins
            case "Empleado"           -> VISTA_EMPLEADO;   // Panel simplificado solo lectura
            default                   -> VISTA_ADMIN;      // Panel de administrador (caso default)
        };

        // Forward: el servidor procesa el JSP y envía la respuesta.
        // La URL del navegador sigue siendo /dashboard.
        request.getRequestDispatcher(vista).forward(request, response);
    }
}
