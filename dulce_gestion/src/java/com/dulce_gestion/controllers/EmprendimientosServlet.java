package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
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
 * SERVLET: EmprendimientosServlet
 * URL:     /emprendimientos
 * MÉTODOS: GET
 * ============================================================
 *
 * Gestiona la visualización de emprendimientos según el rol del usuario:
 *
 * - SuperAdministrador: ve todos los emprendimientos del sistema
 * - Administrador: ve solo su emprendimiento asignado
 * - Empleado: ve solo el emprendimiento donde trabaja
 *
 * Lógica de acceso:
 * 1. Verifica que el usuario tenga sesión activa
 * 2. Según el rol, carga los emprendimientos correspondientes
 * 3. Envía los datos al JSP para visualización
 *
 * Atributos enviados al JSP:
 * - "emprendimientos": lista de emprendimientos (solo SuperAdmin)
 * - "miEmprendimiento": emprendimiento del usuario (Admin/Empleado)
 * - "rol": rol del usuario para personalizar la vista
 *
 * Usado por: menú de navegación principal para gestión de emprendimientos
 */
@WebServlet("/emprendimientos")
public class EmprendimientosServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/emprendimientos/lista.jsp";

    /**
     * GET /emprendimientos -> muestra emprendimientos según rol del usuario
     *
     * Proceso de ejecución:
     * 1. Verificar sesión activa del usuario
     * 2. Determinar el rol (SuperAdmin, Admin, Empleado)
     * 3. Cargar emprendimientos según reglas de visibilidad
     * 4. Enviar datos al JSP para visualización
     *
     * @param req  petición HTTP con sesión del usuario
     * @param res  respuesta HTTP para redirigir o enviar vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // Paso 1: verificar sesión activa
        HttpSession session = req.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuario") : null;
        if (usuario == null) { 
            // Sin sesión -> redirigir al login
            res.sendRedirect(req.getContextPath() + "/login"); 
            return; 
        }

        // Paso 2: determinar rol para aplicar reglas de visibilidad
        String rol = usuario.getNombreRol();
        EmprendimientoDAO dao = new EmprendimientoDAO();

        try {
            if ("SuperAdministrador".equals(rol)) {
                // SuperAdmin: ve todos los emprendimientos del sistema
                req.setAttribute("emprendimientos", dao.listarTodos());
            } else {
                // Admin o Empleado: solo ve su emprendimiento asignado
                int idEmp = usuario.getIdEmprendimiento();
                if (idEmp > 0) {
                    req.setAttribute("miEmprendimiento", dao.buscarPorId(idEmp));
                }
            }
            
            // Paso 3: enviar rol al JSP para personalizar la interfaz
            req.setAttribute("rol", rol);
            
            // Paso 4: forward al JSP para renderizar la vista
            req.getRequestDispatcher(VISTA).forward(req, res);
            
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error", "Error al cargar los emprendimientos.");
            req.getRequestDispatcher(VISTA).forward(req, res);
        }
    }
}
