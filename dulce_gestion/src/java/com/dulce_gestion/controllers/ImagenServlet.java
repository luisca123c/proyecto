package com.dulce_gestion.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/*
 * Sirve imágenes de productos.
 * Las imágenes están en: web/assets/images/productos/
 * Son accesibles directamente como recursos estáticos, pero este
 * servlet actúa como fallback para URLs del tipo /uploads/productos/...
 * (compatibilidad con registros antiguos en BD).
 *
 * URL: /dulce_gestion/uploads/productos/producto_X.jpg
 */
@WebServlet("/uploads/*")
public class ImagenServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Buscar primero en assets/images (ruta nueva)
        File webDir  = new File(getServletContext().getRealPath("/"));
        File archivo = new File(webDir, "assets/images" + pathInfo);

        // Seguridad: evitar path traversal
        File base = new File(webDir, "assets/images");
        if (!archivo.getCanonicalPath().startsWith(base.getCanonicalPath())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!archivo.exists() || !archivo.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = Files.probeContentType(archivo.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        response.setContentType(mimeType);
        response.setContentLengthLong(archivo.length());
        response.setHeader("Cache-Control", "public, max-age=3600");
        Files.copy(archivo.toPath(), response.getOutputStream());
    }
}
