package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*
 * DAO para eliminar un producto.
 * Si el producto está referenciado en detalle_compra u otras tablas
 * con ON DELETE RESTRICT, la BD lanzará SQLException y se hace rollback.
 */
public class EliminarProductoDAO {

    public void eliminar(int idProducto) throws SQLException {
        String sql = "DELETE FROM productos WHERE id = ?";

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.executeUpdate();
        }
    }
}
