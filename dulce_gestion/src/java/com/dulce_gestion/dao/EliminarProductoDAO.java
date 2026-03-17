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
