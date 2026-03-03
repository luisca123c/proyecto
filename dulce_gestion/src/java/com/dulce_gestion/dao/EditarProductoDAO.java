package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*
 * DAO para actualizar los datos de un producto existente.
 */
public class EditarProductoDAO {

    public void actualizar(int id, String nombre, String descripcion, int stock,
                           BigDecimal precio, String estado, String fechaVencimiento,
                           int idCategoria, int idUnidad) throws SQLException {

        String sql = """
                UPDATE productos
                SET nombre = ?, descripcion = ?, stock_actual = ?,
                    precio_unitario = ?, estado = ?, fecha_vencimiento = ?,
                    id_categoria = ?, id_unidad = ?
                WHERE id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre.trim());
            ps.setString(2, descripcion != null ? descripcion.trim() : "");
            ps.setInt(3, stock);
            ps.setBigDecimal(4, precio);
            ps.setString(5, estado);
            ps.setString(6, fechaVencimiento);
            ps.setInt(7, idCategoria);
            ps.setInt(8, idUnidad);
            ps.setInt(9, id);

            ps.executeUpdate();
        }
    }
}
