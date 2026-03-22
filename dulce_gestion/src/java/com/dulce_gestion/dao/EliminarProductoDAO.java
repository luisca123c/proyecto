package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: EliminarProductoDAO
 * Tabla escrita:  productos (UPDATE estado = 'Inactivo')
 * Usado por:      EliminarProductoServlet
 *
 * La eliminación es LÓGICA: el producto pasa a estado
 * 'Inactivo' en vez de borrarse físicamente, preservando
 * el historial de ventas asociado.
 * ============================================================
 */
public class EliminarProductoDAO {

    /**
     * Inactiva el producto con el ID dado (eliminación lógica).
     * Un producto inactivo no aparece en catálogo ni en el carrito.
     *
     * @param idProducto  ID del producto a inactivar
     * @throws SQLException si ocurre un error de base de datos
     */
    public void inactivar(int idProducto) throws SQLException {
        String sql = "UPDATE productos SET estado = 'Inactivo' WHERE id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.executeUpdate();
        }
    }
}
