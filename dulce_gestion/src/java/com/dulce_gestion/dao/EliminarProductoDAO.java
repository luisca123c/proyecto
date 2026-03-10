package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: EliminarProductoDAO
 * Tabla escrita:     productos
 * Tablas en CASCADE: imagenes_producto (se borra automáticamente)
 * Usado por:         EliminarProductoServlet
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Elimina un producto de la BD. El archivo de imagen físico se borra
 * ANTES de llamar a este DAO (en el Servlet), porque una vez eliminado
 * el producto ya no se puede consultar su path de imagen.
 *
 * ¿POR QUÉ imagenes_producto SE BORRA SOLO?
 * -------------------------------------------
 * La tabla imagenes_producto tiene una FK hacia productos con
 * ON DELETE CASCADE:
 *   imagenes_producto.id_producto → productos.id  (CASCADE)
 *
 * Al borrar el producto, MySQL elimina automáticamente la fila
 * en imagenes_producto sin necesitar un DELETE explícito.
 *
 * ¿QUÉ PASA SI EL PRODUCTO TIENE VENTAS?
 * ----------------------------------------
 * La tabla detalle_carrito puede tener:
 *   detalle_carrito.id_producto → productos.id  (ON DELETE RESTRICT)
 *
 * Si el producto aparece en alguna venta histórica, MySQL rechaza
 * el DELETE con una excepción de FK violada (RESTRICT). EliminarProductoServlet
 * captura esa excepción y redirige con ?error=eliminacion.
 */
public class EliminarProductoDAO {

    /**
     * Elimina el producto con el ID dado.
     * CASCADE elimina automáticamente su registro en imagenes_producto.
     *
     * @param idProducto  ID del producto a eliminar
     * @throws SQLException si la BD rechaza la eliminación (ej: tiene ventas)
     */
    public void eliminar(int idProducto) throws SQLException {
        // DELETE simple: la integridad referencial la maneja la BD con CASCADE/RESTRICT
        String sql = "DELETE FROM productos WHERE id = ?";

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.executeUpdate();
        }
    }
}
