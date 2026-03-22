package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EliminarProductoDAO;
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
 * SERVLET: EliminarProductoServlet
 * URL:     POST /productos/eliminar
 * MÉTODOS: POST
 * ============================================================
 *
 * Inactiva un producto (eliminación lógica).
 * Cambia el campo estado a 'Inactivo' — no borra el registro.
 * Un producto inactivo no aparece en catálogo ni en el carrito,
 * pero su historial de ventas queda intacto.
 *
 * Permisos: Administrador y SuperAdministrador.
 */
@WebServlet("/productos/eliminar")
public class EliminarProductoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath();

        // ── Paso 1: verificar sesión y rol ───────────────────────────────
        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (usuario == null) { response.sendRedirect(ctx + "/login"); return; }

        String rol = usuario.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(ctx + "/dashboard"); return;
        }

        // ── Paso 2: leer y validar el ID del producto a inactivar ────────
        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(ctx + "/productos"); return;
        }

        try {
            int idProducto = Integer.parseInt(idParam);

            // ── Paso 3: inactivar mediante el DAO ─────────────────────────
            new EliminarProductoDAO().inactivar(idProducto);

            response.sendRedirect(ctx + "/productos?exito=eliminado");

        } catch (NumberFormatException e) {
            response.sendRedirect(ctx + "/productos");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(ctx + "/productos?error=eliminacion");
        }
    }
}
