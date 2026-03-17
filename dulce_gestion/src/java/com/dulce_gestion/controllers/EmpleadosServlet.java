package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmpleadoDAO;
import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.utils.EmpresaUtil;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * ============================================================
 * SERVLET: EmpleadosServlet
 * URL:     /empleados
 * METODOS: GET
 * ============================================================
 *
 * LOGICA DE FILTRADO POR EMPRENDIMIENTO:
 *   SuperAdministrador:
 *     - Carga la lista de emprendimientos activos para mostrar los tabs/filtros.
 *     - Lee el parametro ?emp=<id> de la URL (0 o ausente = todos).
 *     - Llama a EmpleadoDAO.listarFiltrado("SuperAdministrador", idEmp).
 *     - Pasa "emprendimientos" y "empFiltro" al JSP.
 *
 *   Administrador:
 *     - Usa directamente su id_emprendimiento de la sesion.
 *     - Solo ve los Empleados de su emprendimiento (sin opcion de cambiar).
 *     - El JSP muestra el nombre del emprendimiento como contexto.
 *
 *   Empleado:
 *     - Redirigido al dashboard (no tiene acceso).
 */
@WebServlet("/empleados")
public class EmpleadosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Paso 1: verificar sesion
        HttpSession session = request.getSession(false);
        Usuario solicitante = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (solicitante == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String rol = solicitante.getNombreRol();

        // Paso 2: solo Admin y SuperAdmin tienen acceso
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        EmpleadoDAO dao = new EmpleadoDAO();
        List<Usuario> lista;
        int empFiltro = 0; // 0 = todos (solo relevante para SuperAdmin)

        try {
            if ("SuperAdministrador".equals(rol)) {
                // Leer ?emp= de la URL; 0 si no viene o no es numero
                String empParam = request.getParameter("emp");
                if (empParam != null && !empParam.isBlank()) {
                    try { empFiltro = Integer.parseInt(empParam); }
                    catch (NumberFormatException ignored) { empFiltro = 0; }
                }

                // Cargar lista de emprendimientos para los tabs del JSP
                EmprendimientoDAO empDAO = new EmprendimientoDAO();
                List<Emprendimiento> emprendimientos = empDAO.listarActivos();
                request.setAttribute("emprendimientos", emprendimientos);
                request.setAttribute("empFiltro", empFiltro);

                lista = dao.listarFiltrado(rol, empFiltro);

            } else {
                // Admin: siempre filtra por su propio emprendimiento
                empFiltro = EmpresaUtil.resolverEmprendimiento(solicitante, request);
                lista = dao.listarFiltrado(rol, empFiltro);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorEmpleados", "Error al cargar los empleados.");
            lista = List.of();
        }

        request.setAttribute("empleados",      lista);
        request.setAttribute("rolSolicitante", rol);

        request.getRequestDispatcher("/WEB-INF/jsp/empleados/lista.jsp")
               .forward(request, response);
    }
}
