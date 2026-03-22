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
 * GET  /emprendimientos/nuevo → muestra el formulario
 * POST /emprendimientos/nuevo → crea el emprendimiento
 * Solo SuperAdministrador.
 */
@WebServlet("/emprendimientos/nuevo")
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

        // Validación completa en servidor
        if (nombre == null || nombre.isBlank()) {
            req.setAttribute("error", "El nombre del emprendimiento es obligatorio.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (nit == null || nit.isBlank()) {
            req.setAttribute("error", "El NIT es obligatorio.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (direccion == null || direccion.isBlank()) {
            req.setAttribute("error", "La dirección es obligatoria.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (ciudad == null || ciudad.isBlank()) {
            req.setAttribute("error", "La ciudad es obligatoria.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (telefono == null || telefono.isBlank()) {
            req.setAttribute("error", "El teléfono es obligatorio.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (correo == null || correo.isBlank()) {
            req.setAttribute("error", "El correo es obligatorio.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }

        // Validaciones de formato
        if (!nit.trim().matches("[0-9]{6,15}(-[0-9])?")) {
            req.setAttribute("error", "El NIT tiene un formato inválido. Ej: 900123456-1");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (!telefono.trim().matches("[0-9+\\-\\s()]{7,20}")) {
            req.setAttribute("error", "El teléfono tiene un formato inválido.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (!ciudad.trim().matches("[a-zA-Z\u00C0-\u024F\\s]{2,100}")) {
            req.setAttribute("error", "La ciudad solo puede contener letras y espacios.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (!direccion.trim().matches("[a-zA-Z0-9\u00C0-\u024F\\s#\\-\\.\u00b0]+")) {
            req.setAttribute("error", "La dirección contiene caracteres no permitidos.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }
        if (!correo.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            req.setAttribute("error", "El correo tiene un formato inválido.");
            req.setAttribute("form",  req.getParameterMap());
            req.getRequestDispatcher(VISTA).forward(req, res);
            return;
        }

        try {
            new EmprendimientoDAO().crear(nombre.trim(), nit.trim(), direccion.trim(),
                                          ciudad.trim(), telefono.trim(), correo.trim());
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
