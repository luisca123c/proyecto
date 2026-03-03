package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EliminarProductoDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/*
 * Servlet para POST /productos/eliminar.
 * Solo acepta POST para evitar eliminaciones accidentales por URL.
 * Solo accesible para SuperAdministrador y Administrador.
 */
public class EliminarProductoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath();

        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) { response.sendRedirect(ctx + "/login"); return; }

        String rol = usuario.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(ctx + "/productos");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(ctx + "/productos");
            return;
        }

        try {
            int id = Integer.parseInt(idParam.trim());
            new EliminarProductoDAO().eliminar(id);
            response.sendRedirect(ctx + "/productos?exito=eliminado");
        } catch (NumberFormatException e) {
            response.sendRedirect(ctx + "/productos");
        } catch (SQLException e) {
            // La BD puede rechazar si el producto tiene ventas asociadas
            e.printStackTrace();
            response.sendRedirect(ctx + "/productos?error=eliminacion");
        }
    }
}
