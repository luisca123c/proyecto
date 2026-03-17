package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /emprendimientos → lista todos (SuperAdmin) o el propio (Admin/Empleado)
 */
public class EmprendimientosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuario") : null;
        if (usuario == null) { res.sendRedirect(req.getContextPath() + "/login"); return; }

        String rol = usuario.getNombreRol();
        EmprendimientoDAO dao = new EmprendimientoDAO();

        try {
            if ("SuperAdministrador".equals(rol)) {
                req.setAttribute("emprendimientos", dao.listarTodos());
            } else {
                // Admin o Empleado: solo ve el suyo
                int idEmp = usuario.getIdEmprendimiento();
                if (idEmp > 0) {
                    req.setAttribute("miEmprendimiento", dao.buscarPorId(idEmp));
                }
            }
            req.setAttribute("rol", rol);
            req.getRequestDispatcher("/WEB-INF/jsp/emprendimientos/lista.jsp").forward(req, res);
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error", "Error al cargar los emprendimientos.");
            req.getRequestDispatcher("/WEB-INF/jsp/emprendimientos/lista.jsp").forward(req, res);
        }
    }
}
