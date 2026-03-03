package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EditarEmpleadoDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/*
 * Servlet para /empleados/editar?id=X
 * GET  → carga los datos del usuario y muestra el formulario
 * POST → valida y actualiza los datos
 *
 * SuperAdmin puede editar Admins y Empleados.
 * Admin solo puede editar Empleados.
 */
@WebServlet("/empleados/editar")
public class EditarEmpleadoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/empleados/editar.jsp";

    /* ── GET ──────────────────────────────────────────────── */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario solicitante = getSolicitante(request, response);
        if (solicitante == null) return;

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/empleados");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            EditarEmpleadoDAO dao = new EditarEmpleadoDAO();
            Usuario objetivo = dao.buscarPorId(id);

            if (objetivo == null || !puedeEditar(solicitante, objetivo)) {
                response.sendRedirect(request.getContextPath() + "/empleados");
                return;
            }

            request.setAttribute("objetivo", objetivo);
            request.setAttribute("esSuperAdmin",
                "SuperAdministrador".equals(solicitante.getNombreRol()));
            request.getRequestDispatcher(VISTA).forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/empleados");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/empleados");
        }
    }

    /* ── POST ─────────────────────────────────────────────── */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario solicitante = getSolicitante(request, response);
        if (solicitante == null) return;

        boolean esSuperAdmin = "SuperAdministrador".equals(solicitante.getNombreRol());

        String idParam        = request.getParameter("id");
        String nombreCompleto = request.getParameter("nombreCompleto");
        String telefono       = request.getParameter("telefono");
        String genero         = request.getParameter("genero");
        String correo         = request.getParameter("correo");
        String nuevaContrasena = request.getParameter("contrasena");
        String estado         = request.getParameter("estado");
        String rol            = request.getParameter("rol");

        // Validación básica
        if (estaVacio(idParam) || estaVacio(nombreCompleto) || estaVacio(telefono)
                || estaVacio(genero) || estaVacio(correo) || estaVacio(estado)) {
            reenviar(request, response, "Todos los campos son obligatorios (excepto contraseña).", idParam);
            return;
        }

        if (nuevaContrasena != null && !nuevaContrasena.isBlank() && nuevaContrasena.trim().length() < 6) {
            reenviar(request, response, "La nueva contraseña debe tener al menos 6 caracteres.", idParam);
            return;
        }

        int idUsuario;
        try {
            idUsuario = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/empleados");
            return;
        }

        EditarEmpleadoDAO dao = new EditarEmpleadoDAO();
        try {
            Usuario objetivo = dao.buscarPorId(idUsuario);
            if (objetivo == null || !puedeEditar(solicitante, objetivo)) {
                response.sendRedirect(request.getContextPath() + "/empleados");
                return;
            }

            if (dao.correoExisteEnOtro(correo, idUsuario)) {
                reenviar(request, response, "El correo ya está en uso por otro usuario.", idParam);
                return;
            }
            if (dao.telefonoExisteEnOtro(telefono, idUsuario)) {
                reenviar(request, response, "El teléfono ya está en uso por otro usuario.", idParam);
                return;
            }

            // Admin no puede cambiar el rol — usar el rol actual del objetivo
            String rolFinal = esSuperAdmin ? rol : objetivo.getNombreRol();

            dao.actualizar(idUsuario, nombreCompleto, telefono, genero,
                           correo, nuevaContrasena, estado, rolFinal, esSuperAdmin);

        } catch (SQLException e) {
            e.printStackTrace();
            reenviar(request, response, "Error al guardar los cambios. Intenta de nuevo.", idParam);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/empleados?exito=editado");
    }

    /* ── Helpers ──────────────────────────────────────────── */

    private Usuario getSolicitante(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        Usuario u = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        String rol = u.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            res.sendRedirect(req.getContextPath() + "/dashboard");
            return null;
        }
        return u;
    }

    /*
     * SuperAdmin puede editar Admins y Empleados.
     * Admin solo puede editar Empleados.
     */
    private boolean puedeEditar(Usuario solicitante, Usuario objetivo) {
        String rolSol = solicitante.getNombreRol();
        String rolObj = objetivo.getNombreRol();
        if ("SuperAdministrador".equals(rolSol)) {
            return "Administrador".equals(rolObj) || "Empleado".equals(rolObj);
        }
        if ("Administrador".equals(rolSol)) {
            return "Empleado".equals(rolObj);
        }
        return false;
    }

    private boolean estaVacio(String v) { return v == null || v.isBlank(); }

    private void reenviar(HttpServletRequest req, HttpServletResponse res,
                          String error, String idParam)
            throws ServletException, IOException {
        try {
            int id = Integer.parseInt(idParam);
            EditarEmpleadoDAO dao = new EditarEmpleadoDAO();
            Usuario objetivo = dao.buscarPorId(id);
            req.setAttribute("objetivo", objetivo);

            HttpSession session = req.getSession(false);
            Usuario sol = (Usuario) session.getAttribute("usuario");
            req.setAttribute("esSuperAdmin",
                "SuperAdministrador".equals(sol.getNombreRol()));
        } catch (Exception ignored) {}

        req.setAttribute("error", error);
        req.getRequestDispatcher(VISTA).forward(req, res);
    }
}
