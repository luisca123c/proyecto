package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EliminarProductoDAO;
import com.dulce_gestion.dao.ImagenProductoDAO;
import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.Uploads;
import java.io.File;

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

            // Borrar imagen física antes de eliminar el producto
            try {
                String urlRelativa = new ImagenProductoDAO().obtenerPath(id);
                if (urlRelativa != null && !urlRelativa.isBlank()) {
                    String nombreArchivo = new File(urlRelativa).getName();
                    File img = new File(
                        Uploads.carpetaProductos(request.getServletContext()),
                        nombreArchivo
                    );
                    if (img.exists()) img.delete();
                }
            } catch (Exception ignored) {}

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
