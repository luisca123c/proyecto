package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * DAO para insertar un nuevo producto.
 * Retorna el ID generado para que el Servlet pueda asociar la imagen.
 */
public class CrearProductoDAO {

    public int crear(String nombre, String descripcion, int stock,
                     BigDecimal precio, String estado, String fechaVencimiento,
                     int idCategoria, int idUnidad) throws SQLException {

        String sql = """
                INSERT INTO productos
                    (nombre, descripcion, stock_actual, precio_unitario,
                     estado, fecha_vencimiento, id_categoria, id_unidad)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nombre.trim());
            ps.setString(2, descripcion != null ? descripcion.trim() : "");
            ps.setInt(3, stock);
            ps.setBigDecimal(4, precio);
            ps.setString(5, estado);
            ps.setString(6, fechaVencimiento);
            ps.setInt(7, idCategoria);
            ps.setInt(8, idUnidad);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("No se obtuvo el ID del producto creado.");
    }
}
