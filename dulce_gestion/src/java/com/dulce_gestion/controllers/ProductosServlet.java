package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.utils.EmpresaUtil;
import com.dulce_gestion.dao.ProductoDAO;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.models.Producto;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * ============================================================
 * SERVLET: ProductosServlet
 * URL:     /productos
 * MÉTODOS: GET
 * ============================================================
 *
 */
@WebServlet("/productos")
public class ProductosServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/productos/lista.jsp";


    /**
     * GET /productos → carga la lista de productos y hace forward al JSP.
     *
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: verificar sesión activa ─────────────────────────────
        HttpSession session = request.getSession(false);
        // Ternario para evitar NullPointerException si session es null
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) {
            // Sin sesión → redirigir al login
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String rol = usuario.getNombreRol();
        int empFiltro = 0;

        // ── Paso 2: cargar productos filtrados por emprendimiento ────────
        List<Producto> productos;
        try {
            ProductoDAO dao = new ProductoDAO();
            if ("SuperAdministrador".equals(rol)) {
                String empParam = request.getParameter("emp");
                if (empParam != null && !empParam.isBlank()) {
                    try { empFiltro = Integer.parseInt(empParam); } catch (NumberFormatException ignored) {}
                }
                List<Emprendimiento> emprendimientos = new EmprendimientoDAO().listarActivos();
                request.setAttribute("emprendimientos", emprendimientos);
                productos = dao.listarFiltrado(rol, empFiltro);
            } else {
                // Admin y Empleado: solo ven productos de su emprendimiento
                empFiltro = EmpresaUtil.resolverEmprendimiento(usuario, request);
                productos = dao.listarFiltrado(rol, empFiltro);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            productos = List.of();
            request.setAttribute("errorProductos", "Error al cargar los productos.");
        }

        // ── Paso 3: poner los datos en el request para el JSP ───────────
        request.setAttribute("productos",      productos);
        request.setAttribute("rolSolicitante", rol);
        request.setAttribute("empFiltro",      empFiltro);

        // ── Paso 4: forward al JSP ──────────────────────────────────────
        request.getRequestDispatcher(VISTA)
               .forward(request, response);
    }
}
