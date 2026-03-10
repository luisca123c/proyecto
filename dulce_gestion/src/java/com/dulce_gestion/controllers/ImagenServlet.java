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
 * ¿QUÉ HACE?
 * ----------
 * Sirve imágenes de productos desde la carpeta assets/images/
 * del deploy de Tomcat. Actúa como un servidor de archivos estáticos
 * para las imágenes subidas por los administradores.
 *
 * ¿POR QUÉ EXISTE ESTE SERVLET SI TOMCAT YA SIRVE ESTÁTICOS?
 * ------------------------------------------------------------
 * Las imágenes en assets/images/productos/ son accesibles directamente
 * como recursos estáticos con URL:
 *   /dulce_gestion/assets/images/productos/producto_3.jpg
 *
 * Sin embargo, registros ANTIGUOS en la BD pueden tener paths en formato:
 *   /uploads/productos/producto_3.jpg
 *
 * Este servlet mapea esa URL antigua (/uploads/*) y busca el archivo
 * en assets/images/, funcionando como un capa de compatibilidad.
 *
 * ¿QUÉ ES request.getPathInfo()?
 * --------------------------------
 * El servlet está mapeado a /uploads/*.
 * Cuando llega una petición a /uploads/productos/producto_3.jpg:
 *   getServletPath()  → "/uploads"
 *   getPathInfo()     → "/productos/producto_3.jpg"
 *
 * Se usa getPathInfo() para extraer la parte dinámica de la URL
 * (el nombre del subdirectorio y el archivo).
 *
 * ¿QUÉ ES PATH TRAVERSAL Y CÓMO SE PREVIENE?
 * ---------------------------------------------
 * Path Traversal es un ataque en el que un usuario malintencionado
 * intenta acceder a archivos fuera del directorio permitido usando
 * secuencias como "../" en la URL:
 *   /uploads/../../../../etc/passwd
 *
 * La verificación de seguridad es:
 *   archivo.getCanonicalPath().startsWith(base.getCanonicalPath())
 *
 * getCanonicalPath() resuelve todos los "../" y "./" y retorna la
 * ruta absoluta real. Si el resultado no empieza con la carpeta base,
 * significa que intentaron salir del directorio → se rechaza con 403.
 *
 * ¿QUÉ ES Files.probeContentType()?
 * ------------------------------------
 * Detecta el tipo MIME del archivo según su extensión:
 *   .jpg → "image/jpeg"
 *   .png → "image/png"
 *   .gif → "image/gif"
 *
 * El Content-Type correcto le dice al navegador cómo mostrar el archivo.
 * Si retorna null (extensión desconocida), se usa "application/octet-stream"
 * como tipo genérico para descarga binaria.
 *
 * ¿QUÉ ES Cache-Control: public, max-age=3600?
 * -----------------------------------------------
 * Le indica al navegador y a los proxies que pueden cachear la imagen
 * por 3600 segundos (1 hora). Así, si el navegador ya descargó la imagen
 * anteriormente, la usa desde su caché sin hacer otra petición al servidor.
 * Mejora el rendimiento de la aplicación.
 */
@WebServlet("/uploads/*")
public class ImagenServlet extends HttpServlet {

    /**
     * GET /uploads/productos/producto_X.jpg → sirve el archivo de imagen.
     *
     * FLUJO PASO A PASO:
     *
     * Paso 1 — Obtener la parte dinámica de la URL (getPathInfo).
     *   Ej: "/productos/producto_3.jpg"
     *   Si es null o "/" → retornar 404.
     *
     * Paso 2 — Construir la ruta física del archivo.
     *   getRealPath("/") → ruta absoluta del deploy en el sistema de archivos.
     *   Ej: "D:\proyecto\build\web\"
     *   La ruta del archivo queda: build\web\assets\images\productos\producto_3.jpg
     *
     * Paso 3 — Verificar seguridad (prevención de path traversal).
     *   Si el path canónico del archivo no empieza con la carpeta base → 403 Forbidden.
     *
     * Paso 4 — Verificar que el archivo existe y es un archivo (no directorio).
     *   Si no existe → 404 Not Found.
     *
     * Paso 5 — Detectar el tipo MIME y configurar los headers de respuesta.
     *
     * Paso 6 — Copiar los bytes del archivo al output stream de la respuesta.
     *   Files.copy() hace una copia eficiente usando NIO (New I/O de Java).
     *
     * @param request  contiene la URL con la ruta de la imagen (/uploads/...)
     * @param response para enviar los headers y los bytes del archivo
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
