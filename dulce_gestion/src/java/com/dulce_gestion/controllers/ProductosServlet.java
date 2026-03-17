package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.dao.ProductoDAO;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.models.Producto;
import com.dulce_gestion.models.Usuario;

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
 * ¿QUÉ HACE?
 * ----------
 * Carga la lista completa de productos desde la BD y la envía
 * al JSP lista.jsp para mostrarlos en la grilla de tarjetas.
 *
 * ¿QUIÉN PUEDE ACCEDER?
 * ----------------------
 * TODOS los roles autenticados (SuperAdmin, Admin y Empleado).
 * La diferencia está en lo que pueden HACER con los productos:
 *
 *   Admin / SuperAdmin → ven botones "Editar" y "Eliminar" por producto
 *   Empleado           → solo lectura, solo ve el catálogo sin botones de acción
 *
 * Esta diferencia visual la maneja el JSP, no este servlet.
 * El servlet solo pasa el rol como atributo para que el JSP decida.
 *
 * ¿POR QUÉ NO SE HACE EL CONTROL DE ACCESO POR ROL AQUÍ?
 * --------------------------------------------------------
 * El módulo de productos es de lectura pública dentro del sistema
 * (todos los empleados necesitan ver qué hay en inventario para vender).
 * Solo las acciones de modificar (crear, editar, eliminar) están restringidas
 * a Admin y SuperAdmin — y eso lo controlan NuevoProductoServlet,
 * EditarProductoServlet y EliminarProductoServlet respectivamente.
 *
 * ¿QUÉ HACE List.of() EN CASO DE ERROR?
 * ----------------------------------------
 * List.of() crea una lista vacía inmutable. Se usa como fallback cuando
 * la consulta falla, para que el JSP pueda iterar una lista vacía sin
 * lanzar NullPointerException (ya que el JSP no verifica null en el for).
 */
public class ProductosServlet extends HttpServlet {

    /**
     * GET /productos → carga la lista de productos y hace forward al JSP.
     *
     * FLUJO PASO A PASO:
     * 1. Verificar que hay sesión activa.
     * 2. Cargar todos los productos desde la BD (JOIN con imágenes).
     * 3. Poner la lista y el rol en el request como atributos.
     * 4. Forward al JSP lista.jsp.
     *
     * @param request  contiene la sesión activa y posibles params ?exito= / ?error=
     * @param response para redirigir si no hay sesión, o para el forward al JSP
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
                empFiltro = usuario.getIdEmprendimiento();
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
        request.getRequestDispatcher("/WEB-INF/jsp/productos/lista.jsp")
               .forward(request, response);
    }
}
