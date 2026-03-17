package com.dulce_gestion.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * ============================================================
 * SERVLET: ImagenServlet
 * URL:     /uploads/*
 * MÉTODOS: GET
 * ============================================================
 *
 */
@WebServlet("/uploads/*")
public class ImagenServlet extends HttpServlet {

    /**
     * GET /uploads/productos/producto_X.jpg → sirve el archivo de imagen.
     *
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: obtener la parte dinámica de la URL ──────────────────
        // Para /uploads/productos/producto_3.jpg → pathInfo = "/productos/producto_3.jpg"
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            // Sin ruta específica → no hay imagen que servir
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
            return;
        }

        // ── Paso 2: construir la ruta física del archivo ─────────────────
        // getRealPath("/") retorna la ruta absoluta de la raíz del deploy
        File webDir  = new File(getServletContext().getRealPath("/"));
        // Concatenar con "assets/images" + la parte dinámica de la URL
        // Ej: .../build/web/assets/images/productos/producto_3.jpg
        File archivo = new File(webDir, "assets/images" + pathInfo);

        // ── Paso 3: verificar seguridad contra path traversal ─────────────
        // Carpeta base que se considera segura para servir archivos
        File base = new File(webDir, "assets/images");

        // getCanonicalPath() resuelve "../" y retorna la ruta real absoluta
        if (!archivo.getCanonicalPath().startsWith(base.getCanonicalPath())) {
            // La ruta sale del directorio permitido → posible ataque de path traversal
            response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden
            return;
        }

        // ── Paso 4: verificar que el archivo existe ───────────────────────
        if (!archivo.exists() || !archivo.isFile()) {
            // El archivo no existe o es un directorio
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
            return;
        }

        // ── Paso 5: detectar tipo MIME y configurar headers ───────────────
        // probeContentType detecta "image/jpeg", "image/png", etc. según la extensión
        String mimeType = Files.probeContentType(archivo.toPath());
        if (mimeType == null) mimeType = "application/octet-stream"; // Fallback para tipo desconocido

        response.setContentType(mimeType);                              // "image/jpeg"
        response.setContentLengthLong(archivo.length());               // Tamaño del archivo en bytes
        response.setHeader("Cache-Control", "public, max-age=3600");   // Cachear 1 hora

        // ── Paso 6: enviar los bytes del archivo al navegador ─────────────
        // Files.copy() usa NIO para una copia eficiente en memoria
        // getOutputStream() retorna el stream de respuesta al navegador
        Files.copy(archivo.toPath(), response.getOutputStream());
    }
}
