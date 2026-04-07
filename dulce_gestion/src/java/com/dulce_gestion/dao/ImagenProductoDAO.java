package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO especializado en la gestión de imágenes de productos.
 *
 * Esta clase maneja las operaciones CRUD para las imágenes asociadas
 * a productos, incluyendo almacenamiento, actualización y eliminación.
 *
 * Tabla principal: imagenes_producto
 *
 * Características importantes:
 * - Operaciones UPSERT (INSERT o UPDATE según exista)
 * - Manejo de rutas de archivos y texto alternativo (alt)
 * - Integración con sistema de archivos para gestión de imágenes
 * - Validación de existencia antes de operaciones
 *
 * Usado por: NuevoProductoServlet, EditarProductoServlet, EliminarProductoServlet
 */
public class ImagenProductoDAO {

    // =========================================================
    // CONSULTA - Obtener ruta de imagen
    // =========================================================
    /**
     * Retorna la ruta de imagen registrada para un producto.
     *
     * Se usa en EditarProductoServlet y EliminarProductoServlet para conocer
     * el nombre del archivo antes de borrarlo del disco físico.
     *
     * @param idProducto ID del producto a consultar
     * @return ruta relativa de la imagen (ej: "assets/images/productos/producto_3.jpg"),
     *         o null si el producto no tiene imagen registrada
     * @throws SQLException si hay error al consultar la BD
     */
    public String obtenerPath(int idProducto) throws SQLException {
        String sql = "SELECT path_imagen FROM imagenes_producto WHERE id_producto = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                // Expresión ternaria: si hay fila retorna path, sino retorna null
                return rs.next() ? rs.getString("path_imagen") : null;
            }
        }
    }

    // =========================================================
    // UPSERT - Insertar o actualizar imagen
    // =========================================================
    /**
     * Guarda o reemplaza la imagen de un producto (operación UPSERT).
     *
     * Implementa lógica de UPSERT:
     * - Si ya existe un registro para el producto → UPDATE (reemplazar ruta y alt)
     * - Si no existe → INSERT (primera imagen del producto)
     *
     * @param idProducto ID del producto asociado
     * @param pathImagen ruta relativa de la imagen en el sistema de archivos
     * @param altImagen  texto alternativo para accesibilidad
     * @throws SQLException si hay error en la operación
     */
    public void guardarOActualizar(int idProducto, String pathImagen, String altImagen)
            throws SQLException {

        // Paso 1: Verificar si ya existe un registro para este producto
        String sqlCheck = "SELECT id FROM imagenes_producto WHERE id_producto = ?";
        boolean existe = false;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlCheck)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                existe = rs.next(); // true si hay al menos una fila
            }
        }

        // Paso 2: Elegir SQL según si ya existe (UPDATE) o no (INSERT)
        String sql = existe
            ? "UPDATE imagenes_producto SET path_imagen = ?, alt_imagen = ? WHERE id_producto = ?"
            : "INSERT INTO imagenes_producto (path_imagen, alt_imagen, id_producto) VALUES (?, ?, ?)";

        // Paso 3: Ejecutar la operación (parámetros en el mismo orden para ambos casos)
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pathImagen); // Ruta de la imagen
            ps.setString(2, altImagen);  // Texto alternativo
            ps.setInt(3, idProducto);    // ID del producto
            ps.executeUpdate();
        }
    }

    // =========================================================
    // ELIMINACIÓN - Borrar registro de imagen
    // =========================================================
    /**
     * Elimina el registro de imagen de un producto.
     *
     * Importante: Solo borra el registro en la BD. El archivo físico en disco
     * debe borrarse ANTES de llamar a este método (en el Servlet), ya que
     * después de este DELETE ya no se puede consultar la ruta.
     *
     * Nota: Al eliminar un producto completo, imagenes_producto se limpia
     * automáticamente por CASCADE. Este método se usa solo cuando se
     * quiere eliminar la imagen de un producto que se va a mantener.
     *
     * @param idProducto ID del producto cuya imagen se quiere eliminar
     * @throws SQLException si hay error al eliminar
     */
    public void eliminar(int idProducto) throws SQLException {
        String sql = "DELETE FROM imagenes_producto WHERE id_producto = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.executeUpdate();
        }
    }
}
