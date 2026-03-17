package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /emprendimientos/editar?id=X  → muestra formulario relleno
 * POST /emprendimientos/editar       → guarda cambios o cambia estado
 * Solo SuperAdministrador.
 */
public class EditarEmprendimientoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/emprendimientos/editar.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!soloSuperAdmin(req, res)) return;

        String idParam = req.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            res.sendRedirect(req.getContextPath() + "/emprendimientos"); return;
        }
        try {
            Emprendimiento emp = new EmprendimientoDAO().buscarPorId(Integer.parseInt(idParam));
            if (emp == null) { res.sendRedirect(req.getContextPath() + "/emprendimientos?error=noexiste"); return; }
            req.setAttribute("emprendimiento", emp);
            req.getRequestDispatcher(VISTA).forward(req, res);
        } catch (Exception e) {
            e.printStackTrace();
            res.sendRedirect(req.getContextPath() + "/emprendimientos");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!soloSuperAdmin(req, res)) return;
        req.setCharacterEncoding("UTF-8");

        String accion  = req.getParameter("accion");
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            res.sendRedirect(req.getContextPath() + "/emprendimientos"); return;
        }

        int id;
        try { id = Integer.parseInt(idParam); }
        catch (NumberFormatException e) { res.sendRedirect(req.getContextPath() + "/emprendimientos"); return; }

        EmprendimientoDAO dao = new EmprendimientoDAO();
        String ctx = req.getContextPath();

        try {
            if ("inactivar".equals(accion)) {
                dao.inactivar(id);
                res.sendRedirect(ctx + "/emprendimientos?exito=inactivado");
            } else if ("activar".equals(accion)) {
                dao.activar(id);
                res.sendRedirect(ctx + "/emprendimientos?exito=activado");
            } else {
                // Editar datos
                String nombre    = req.getParameter("nombre");
                if (nombre == null || nombre.isBlank()) {
                    Emprendimiento emp = dao.buscarPorId(id);
                    req.setAttribute("emprendimiento", emp);
                    req.setAttribute("error", "El nombre es obligatorio.");
                    req.getRequestDispatcher(VISTA).forward(req, res);
                    return;
                }
                dao.editar(id,
                    nombre,
                    req.getParameter("nit"),
                    req.getParameter("direccion"),
                    req.getParameter("ciudad"),
                    req.getParameter("telefono"),
                    req.getParameter("correo")
                );
                res.sendRedirect(ctx + "/emprendimientos?exito=editado");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                Emprendimiento emp = dao.buscarPorId(id);
                req.setAttribute("emprendimiento", emp);
                String msg = e.getMessage() != null && e.getMessage().contains("Duplicate")
                        ? "Ya existe un emprendimiento con ese NIT."
                        : "Error al guardar: " + e.getMessage();
                req.setAttribute("error", msg);
                req.getRequestDispatcher(VISTA).forward(req, res);
            } catch (SQLException ex) {
                res.sendRedirect(ctx + "/emprendimientos?error=general");
            }
        }
    }

    private boolean soloSuperAdmin(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession s = req.getSession(false);
        Usuario u = s != null ? (Usuario) s.getAttribute("usuario") : null;
        if (u == null) { res.sendRedirect(req.getContextPath() + "/login"); return false; }
        if (!"SuperAdministrador".equals(u.getNombreRol())) {
            res.sendRedirect(req.getContextPath() + "/dashboard"); return false;
        }
        return true;
    }
}
