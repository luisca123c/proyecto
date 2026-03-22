package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
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

/**
 * GET  /emprendimientos/editar?id=X  → muestra formulario relleno
 * POST /emprendimientos/editar       → guarda cambios o cambia estado
 * Solo SuperAdministrador.
 */
@WebServlet("/emprendimientos/editar")
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
                String nit       = req.getParameter("nit");
                String direccion = req.getParameter("direccion");
                String ciudad    = req.getParameter("ciudad");
                String telefono  = req.getParameter("telefono");
                String correo    = req.getParameter("correo");

                // Validación completa en servidor
                String campoError = null;
                if (nombre    == null || nombre.isBlank())    campoError = "El nombre es obligatorio.";
                else if (nit  == null || nit.isBlank())       campoError = "El NIT es obligatorio.";
                else if (direccion == null || direccion.isBlank()) campoError = "La dirección es obligatoria.";
                else if (ciudad == null || ciudad.isBlank())  campoError = "La ciudad es obligatoria.";
                else if (telefono == null || telefono.isBlank()) campoError = "El teléfono es obligatorio.";
                else if (correo == null || correo.isBlank())  campoError = "El correo es obligatorio.";

                // Validaciones de formato
                if (campoError == null && !nit.trim().matches("[0-9]{6,15}(-[0-9])?"))
                    campoError = "El NIT tiene un formato inválido. Ej: 900123456-1";
                if (campoError == null && !telefono.trim().matches("[0-9+\\-\\s()]{7,20}"))
                    campoError = "El teléfono tiene un formato inválido.";
                if (campoError == null && !ciudad.trim().matches("[a-zA-Z\u00C0-\u024F\\s]{2,100}"))
                    campoError = "La ciudad solo puede contener letras y espacios.";
                if (campoError == null && !direccion.trim().matches("[a-zA-Z0-9\u00C0-\u024F\\s#\\-\\.\u00b0]+"))
                    campoError = "La dirección contiene caracteres no permitidos.";
                if (campoError == null && !correo.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
                    campoError = "El correo tiene un formato inválido.";

                if (campoError != null) {
                    Emprendimiento emp = dao.buscarPorId(id);
                    req.setAttribute("emprendimiento", emp);
                    req.setAttribute("error", campoError);
                    req.getRequestDispatcher(VISTA).forward(req, res);
                    return;
                }

                dao.editar(id, nombre.trim(), nit.trim(), direccion.trim(),
                           ciudad.trim(), telefono.trim(), correo.trim());
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
