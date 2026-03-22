package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.utils.EmpresaUtil;
import com.dulce_gestion.dao.GananciasDAO;
import com.dulce_gestion.models.Emprendimiento;
import java.util.List;
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
 * ============================================================
 * SERVLET: GananciasServlet
 * URL:     /ganancias
 * MÉTODOS: GET
 * ============================================================
 *
 */
@WebServlet("/ganancias")
public class GananciasServlet extends HttpServlet {

    /** Ruta interna del JSP del módulo de ganancias */
    private static final String VISTA = "/WEB-INF/jsp/ganancias/ganancias.jsp";

    /**
     * GET /ganancias → calcula el resumen financiero y hace forward al JSP.
     *
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: verificar sesión activa ──────────────────────────────
        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) {
            // Sin sesión → redirigir al login
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // ── Paso 2: verificar rol — solo Admin y SuperAdmin pueden acceder ─
        String rol = usuario.getNombreRol();
        boolean esAdminOSuper = "SuperAdministrador".equals(rol) || "Administrador".equals(rol);
        if (!esAdminOSuper) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        // ── Paso 3: leer el período solicitado ────────────────────────────
        String periodo = request.getParameter("periodo");
        // Si no se especificó período, mostrar la semana actual por defecto
        if (periodo == null || periodo.isBlank()) periodo = "semana";

        // ── Paso 4: calcular el resumen financiero ────────────────────────
        try {
            GananciasDAO dao = new GananciasDAO();

            // Filtro por emprendimiento para SuperAdmin
            boolean esSuperAdmin = "SuperAdministrador".equals(rol);
            int empFiltro = 0;
            if (esSuperAdmin) {
                String empParam = request.getParameter("emp");
                if (empParam != null && !empParam.isBlank()) {
                    try { empFiltro = Integer.parseInt(empParam); } catch (NumberFormatException ignored) {}
                }
                List<Emprendimiento> emps = new EmprendimientoDAO().listarActivos();
                request.setAttribute("emprendimientos", emps);
                request.setAttribute("empFiltro", empFiltro);
            } else {
                // EmpresaUtil corrige sesiones antiguas con idEmprendimiento=0
                empFiltro = EmpresaUtil.resolverEmprendimiento(usuario, request);
            }
            request.setAttribute("resumen", dao.obtenerResumen(usuario.getId(), esAdminOSuper, periodo, empFiltro));

            // Lista de los últimos 12 meses para el selector del filtro de período
            request.setAttribute("meses",        dao.listarMesesDisponibles());

            // El rol del usuario (para que el JSP muestre/oculte la sección de gastos)
            request.setAttribute("esAdminOSuper", esAdminOSuper);

            // El período actual (para que el <select> muestre la opción correcta seleccionada)
            request.setAttribute("periodo",      periodo);

        } catch (SQLException e) {
            e.printStackTrace(); // Log en consola de Tomcat
            // En caso de error, el JSP muestra el mensaje y la página queda vacía
            request.setAttribute("error", "Error al cargar las ganancias: " + e.getMessage());
        }

        // ── Paso 5: forward al JSP ──────────────────────────────────────
        request.getRequestDispatcher(VISTA).forward(request, response);
    }
}
