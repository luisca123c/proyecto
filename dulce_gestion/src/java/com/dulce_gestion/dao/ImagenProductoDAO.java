package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: ImagenProductoDAO
 * Tabla:      imagenes_producto
 * Usado por:  NuevoProductoServlet, EditarProductoServlet,
 *             EliminarProductoServlet
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Gestiona los registros de la tabla imagenes_producto.
 * Cada producto puede tener máximo UNA imagen (relación 1 a 0..1).
 * En la tabla solo se guarda la ruta relativa del archivo y su
 * texto alternativo — el archivo físico lo maneja el Servlet + Uploads.java.
 *
 * ¿POR QUÉ SE GUARDA SOLO LA RUTA Y NO EL ARCHIVO EN SÍ?
 * --------------------------------------------------------
 * Guardar archivos en la BD (BLOB) consume mucho espacio y hace las
 * consultas lentas. Es una práctica estándar guardar el archivo en el
 * sistema de archivos y solo su ruta en la BD.
 * La ruta guardada es relativa al contexto de la app:
 *   "assets/images/productos/producto_3.jpg"
 * El JSP la usa como: <img src="${ctx}/assets/images/productos/producto_3.jpg">
 *
 * ¿QUÉ ES UN UPSERT?
 * -------------------
 * Un "UPSERT" (Update + Insert) es una operación que hace UPDATE si el
 * registro ya existe, o INSERT si no existe. guardarOActualizar() implementa
 * este patrón con dos queries: primero SELECT para verificar existencia,
 * luego UPDATE o INSERT según el resultado.
 * En MySQL también se podría usar INSERT ... ON DUPLICATE KEY UPDATE,
 * pero la versión explícita es más legible y fácil de depurar.
 */
public class ImagenProductoDAO {

    /**
     * Retorna el path_imagen registrado para un producto, o null si no tiene.
     *
     * Se usa en EditarProductoServlet y EliminarProductoServlet para conocer
     * el nombre del archivo antes de borrarlo del disco físico.
     *
     * @param idProducto  ID del producto
     * @return            ruta relativa de la imagen (ej: "assets/images/productos/producto_3.jpg"),
     *                    o null si el producto no tiene imagen registrada
     * @throws SQLException si hay error al consultar la BD
     */
    public String obtenerPath(int idProducto) throws SQLException {
        String sql = "SELECT path_imagen FROM imagenes_producto WHERE id_producto = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                // rs.next() retorna false si no hay fila → expresión ternaria retorna null
                return rs.next() ? rs.getString("path_imagen") : null;
            }
        }
    }

    /**
     * Guarda o reemplaza la imagen de un producto (UPSERT).
     *
     * Si ya existe un registro para el producto → UPDATE (reemplazar ruta y alt).
     * Si no existe → INSERT (primera imagen del producto).
     *
     * FLUJO:
     * 1. SELECT id para verificar si ya hay registro.
     * 2. Si existe → UPDATE path_imagen y alt_imagen.
     *    Si no existe → INSERT nuevo registro.
     *
     * ¿POR QUÉ DOS CONEXIONES SEPARADAS?
     * ------------------------------------
     * El SELECT y el UPDATE/INSERT usan conexiones distintas del pool.
     * Esto podría causar una condición de carrera si dos peticiones
     * simultáneas verifican la misma imagen al mismo tiempo. Para
     * un sistema de gestión de uso interno es aceptable. Si se
     * requiriera atomicidad, se usaría INSERT ... ON DUPLICATE KEY UPDATE.
     *
     * @param idProducto  ID del producto al que pertenece la imagen
     * @param pathImagen  ruta relativa del archivo (ej: "assets/images/productos/producto_3.jpg")
     * @param altImagen   texto alternativo para accesibilidad (<img alt="...">)
     * @throws SQLException si hay error al consultar o insertar/actualizar
     */
    public void guardarOActualizar(int idProducto, String pathImagen, String altImagen)
            throws SQLException {

        // Verificar si ya existe un registro para este producto
        String sqlCheck = "SELECT id FROM imagenes_producto WHERE id_producto = ?";
        boolean existe = false;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlCheck)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                existe = rs.next(); // true si hay al menos una fila
            }
        }

        // Elegir el SQL según si ya existe (UPDATE) o no (INSERT)
        String sql = existe
            ? "UPDATE imagenes_producto SET path_imagen = ?, alt_imagen = ? WHERE id_producto = ?"
            : "INSERT INTO imagenes_producto (path_imagen, alt_imagen, id_producto) VALUES (?, ?, ?)";

        // Los parámetros son los mismos en ambos casos, en el mismo orden: path, alt, id
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pathImagen);
            ps.setString(2, altImagen);
            ps.setInt(3, idProducto);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina el registro de imagen de un producto.
     *
     * Solo borra el registro en la BD. El archivo físico en disco
     * debe borrarse ANTES de llamar a este método (en el Servlet),
     * ya que después de este DELETE ya no se puede consultar la ruta.
     *
     * Nota: al eliminar un producto completo, imagenes_producto se limpia
     * automáticamente por CASCADE. Este método se usa solo cuando se
     * quiere eliminar la imagen de un producto que se va a mantener.
     *
     * @param idProducto  ID del producto cuya imagen se quiere eliminar
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
