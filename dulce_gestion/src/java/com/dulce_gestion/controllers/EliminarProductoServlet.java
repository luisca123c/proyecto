package com.dulce_gestion.controllers;

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
 * POST /productos/eliminar → inactiva el producto en vez de borrarlo.
 * El producto pasa a estado 'Inactivo' y deja de aparecer en el catálogo
 * y el carrito. No se puede reactivar desde la UI (solo directo en BD).
 */
public class EliminarProductoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath();

        // Verificar sesión y rol
        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (usuario == null) { response.sendRedirect(ctx + "/login"); return; }

        String rol = usuario.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(ctx + "/dashboard"); return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) { response.sendRedirect(ctx + "/productos"); return; }

        try {
            int id = Integer.parseInt(idParam);

            // Inactivar el producto en vez de borrarlo
            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(
                         "UPDATE productos SET estado = 'Inactivo' WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            response.sendRedirect(ctx + "/productos?exito=eliminado");

        } catch (NumberFormatException e) {
            response.sendRedirect(ctx + "/productos");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(ctx + "/productos?error=eliminacion");
        }
    }
}
