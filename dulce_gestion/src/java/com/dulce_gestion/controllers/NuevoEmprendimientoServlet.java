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
 * GET  /emprendimientos/nuevo → muestra el formulario
 * POST /emprendimientos/nuevo → crea el emprendimiento
 * Solo SuperAdministrador.
 */
public class NuevoEmprendimientoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/emprendimientos/nuevo.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!soloSuperAdmin(req, res)) return;
        req.getRequestDispatcher(VISTA).forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!soloSuperAdmin(req, res)) return;
        req.setCharacterEncoding("UTF-8");

        String nombre    = req.getParameter("nombre");
        String nit       = req.getParameter("nit");
        String direccion = req.getParameter("direccion");
        String ciudad    = req.getParameter("ciudad");
        String telefono  = req.getParameter("telefono");
        String correo    = req.getParameter("correo");

        // Validación básica servidor
        if (nombre == null || nombre.isBlank()) {
            req.setAttribute("error", "El nombre del emprendimiento es obligatorio.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }

        try {
            new EmprendimientoDAO().crear(nombre, nit, direccion, ciudad, telefono, correo);
            res.sendRedirect(req.getContextPath() + "/emprendimientos?exito=creado");
        } catch (SQLException e) {
            e.printStackTrace();
            String msg = e.getMessage() != null && e.getMessage().contains("Duplicate")
                    ? "Ya existe un emprendimiento con ese NIT."
                    : "Error al guardar el emprendimiento: " + e.getMessage();
            req.setAttribute("error", msg);
            req.getRequestDispatcher(VISTA).forward(req, res);
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
