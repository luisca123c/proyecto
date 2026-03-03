package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.CrearEmpleadoDAO;
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
 * Servlet para /empleados/nuevo
 * GET  → muestra el formulario vacío
 * POST → valida y crea el nuevo usuario en la BD
 *
 * Solo SuperAdministrador puede acceder a esta ruta.
 */
@WebServlet("/empleados/nuevo")
public class NuevoEmpleadoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/empleados/nuevo.jsp";

    /* ── GET: mostrar formulario ─────────────────────────────── */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarSuperAdmin(request, response)) return;
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    /* ── POST: procesar formulario ───────────────────────────── */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarSuperAdmin(request, response)) return;

        String nombreCompleto = request.getParameter("nombreCompleto");
        String telefono       = request.getParameter("telefono");
        String genero         = request.getParameter("genero");
        String correo         = request.getParameter("correo");
        String contrasena     = request.getParameter("contrasena");
        String estado         = request.getParameter("estado");
        String rol            = request.getParameter("rol");

        // Validación: todos los campos obligatorios
        if (estaVacio(nombreCompleto) || estaVacio(telefono) || estaVacio(genero)
                || estaVacio(correo) || estaVacio(contrasena)
                || estaVacio(estado) || estaVacio(rol)) {

            request.setAttribute("error", "Todos los campos son obligatorios.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        if (contrasena.trim().length() < 6) {
            request.setAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // Solo SuperAdmin puede crear Administradores
        HttpSession session = request.getSession(false);
        Usuario solicitante  = (Usuario) session.getAttribute("usuario");
        if ("Administrador".equals(rol)
                && !"SuperAdministrador".equals(solicitante.getNombreRol())) {
            request.setAttribute("error", "No tienes permiso para crear Administradores.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        CrearEmpleadoDAO dao = new CrearEmpleadoDAO();
        try {
            if (dao.correoExiste(correo)) {
                request.setAttribute("error", "El correo ya está registrado.");
                request.getRequestDispatcher(VISTA).forward(request, response);
                return;
            }
            if (dao.telefonoExiste(telefono)) {
                request.setAttribute("error", "El teléfono ya está registrado.");
                request.getRequestDispatcher(VISTA).forward(request, response);
                return;
            }

            int idRol    = dao.obtenerIdRol(rol);
            int idGenero = dao.obtenerIdGenero(genero);

            dao.crear(nombreCompleto, telefono, idGenero,
                      correo, contrasena, estado, idRol);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al guardar. Intenta de nuevo.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // Éxito → volver a la lista con mensaje
        response.sendRedirect(request.getContextPath() + "/empleados?exito=creado");
    }

    /* ── Helpers ─────────────────────────────────────────────── */

    private boolean verificarSuperAdmin(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        Usuario u = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null || !"SuperAdministrador".equals(u.getNombreRol())) {
            res.sendRedirect(req.getContextPath() + "/dashboard");
            return false;
        }
        return true;
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
