package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EditarEmpleadoDAO;
import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * POST /empleados/eliminar → inactiva al empleado en vez de borrarlo.
 * Si tiene ventas u otras dependencias, igual se puede inactivar
 * porque solo cambia el campo estado de 'Activo' a 'Inactivo'.
 */
public class EliminarEmpleadoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath();

        HttpSession session = request.getSession(false);
        Usuario solicitante = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (solicitante == null) { response.sendRedirect(ctx + "/login"); return; }

        String rolSol = solicitante.getNombreRol();
        if (!"SuperAdministrador".equals(rolSol) && !"Administrador".equals(rolSol)) {
            response.sendRedirect(ctx + "/dashboard"); return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) { response.sendRedirect(ctx + "/empleados"); return; }

        int idUsuario;
        try { idUsuario = Integer.parseInt(idParam); }
        catch (NumberFormatException e) { response.sendRedirect(ctx + "/empleados"); return; }

        try {
            Usuario objetivo = new EditarEmpleadoDAO().buscarPorId(idUsuario);
            if (objetivo == null) { response.sendRedirect(ctx + "/empleados?error=noexiste"); return; }

            String rolObj = objetivo.getNombreRol();
            boolean tienePermiso =
                ("SuperAdministrador".equals(rolSol) &&
                    ("Administrador".equals(rolObj) || "Empleado".equals(rolObj)))
                ||
                ("Administrador".equals(rolSol) && "Empleado".equals(rolObj));

            if (!tienePermiso) { response.sendRedirect(ctx + "/empleados?error=sinpermiso"); return; }

            // Inactivar en vez de borrar
            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(
                         "UPDATE usuarios SET estado = 'Inactivo' WHERE id = ?")) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }

            response.sendRedirect(ctx + "/empleados?exito=eliminado");

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(ctx + "/empleados?error=eliminacion");
        }
    }
}
