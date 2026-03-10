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

/**
 * ============================================================
 * SERVLET: EliminarProductoServlet
 * URL:     /productos/eliminar
 * MÉTODOS: POST (solo POST — sin GET)
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Elimina un producto del sistema. Para hacerlo correctamente, primero
 * borra el archivo físico de imagen del disco (si tiene), y luego
 * elimina el registro del producto en la BD.
 *
 * ¿POR QUÉ SOLO POST?
 * --------------------
 * Un GET /productos/eliminar?id=5 podría ser activado accidentalmente
 * por un bot, prefetch del navegador, o alguien que comparte el enlace.
 * Al requerir POST, solo un formulario con confirmación explícita del
 * usuario puede disparar la eliminación.
 *
 * ¿POR QUÉ SE BORRA LA IMAGEN ANTES DE BORRAR EL PRODUCTO?
 * ----------------------------------------------------------
 * Si borráramos el producto primero, la consulta para obtener el path
 * de la imagen ya no funcionaría (el registro ya no existe).
 *
 * El orden correcto es:
 *   1. Consultar el path de la imagen en imagenes_producto
 *   2. Borrar el archivo físico del disco
 *   3. Borrar el producto → CASCADE borra imagenes_producto automáticamente
 *
 * ¿QUÉ PASA SI EL PRODUCTO TIENE VENTAS?
 * ----------------------------------------
 * La tabla detalle_carrito y/o ventas pueden tener FK hacia productos
 * con ON DELETE RESTRICT. Si el producto ha sido vendido alguna vez,
 * MySQL rechaza el DELETE con SQLException.
 *
 * Se redirige a /productos?error=eliminacion para que el JSP muestre
 * un mensaje explicando que no se puede eliminar.
 */
public class EliminarProductoServlet extends HttpServlet {

    /**
     * POST /productos/eliminar → borra la imagen del disco y el producto de la BD.
     *
     * FLUJO PASO A PASO:
     *
     * Paso 1 — Verificar sesión activa y rol.
     *   Solo Admin y SuperAdmin pueden eliminar productos.
     *
     * Paso 2 — Leer y parsear el parámetro "id".
     *   Si falta o no es número → redirigir a la lista.
     *
     * Paso 3 — Borrar el archivo físico de imagen (si existe).
     *   Se consulta el path en imagenes_producto, se extrae el nombre
     *   del archivo y se borra de la carpeta de imágenes.
     *   Este paso usa try-catch independiente para que un error
     *   aquí no impida eliminar el producto.
     *
     * Paso 4 — Eliminar el producto en la BD.
     *   La tabla imagenes_producto tiene ON DELETE CASCADE →
     *   su registro se borra automáticamente.
     *
     * Paso 5 — Redirigir según resultado.
     *
     * @param request  contiene la sesión y el parámetro "id" del form POST
     * @param response para redirigir al resultado
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath();

        // ── Paso 1: verificar sesión y rol ───────────────────────────────
        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) {
            response.sendRedirect(ctx + "/login");
            return;
        }

        String rol = usuario.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            // Empleado → solo puede ver, no eliminar
            response.sendRedirect(ctx + "/productos");
            return;
        }

        // ── Paso 2: leer y parsear el ID ─────────────────────────────────
        String idParam = request.getParameter("id"); // Campo <input name="id"> del form
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(ctx + "/productos");
            return;
        }

        try {
            int id = Integer.parseInt(idParam.trim());

            // ── Paso 3: borrar el archivo físico de imagen ────────────────
            // Se hace con try-catch independiente para no bloquear el paso 4
            try {
                String urlRelativa = new ImagenProductoDAO().obtenerPath(id);
                if (urlRelativa != null && !urlRelativa.isBlank()) {
                    // Extraer solo el nombre del archivo del path relativo
                    String nombreArchivo = new File(urlRelativa).getName();

                    // Borrar de build/web/ (donde Tomcat sirve los archivos)
                    File img = new File(
                        Uploads.carpetaProductos(request.getServletContext()),
                        nombreArchivo
                    );
                    if (img.exists()) img.delete();

                    // Nota: no borramos de web/ fuente aquí porque el
                    // producto se va a eliminar y no tiene caso mantenerla
                }
            } catch (Exception ignored) {
                // Si falla la eliminación del archivo, el proceso continúa
                // El archivo quedará huérfano pero no bloqueará la BD
            }

            // ── Paso 4: eliminar el producto de la BD ─────────────────────
            // CASCADE elimina automáticamente el registro en imagenes_producto
            new EliminarProductoDAO().eliminar(id);

            // ── Paso 5a: éxito ────────────────────────────────────────────
            response.sendRedirect(ctx + "/productos?exito=eliminado");

        } catch (NumberFormatException e) {
            // ID no era número → volver a la lista sin error visible
            response.sendRedirect(ctx + "/productos");

        } catch (SQLException e) {
            // ── Paso 5b: error (ej: el producto tiene ventas con RESTRICT) ─
            e.printStackTrace();
            response.sendRedirect(ctx + "/productos?error=eliminacion");
        }
    }
}
