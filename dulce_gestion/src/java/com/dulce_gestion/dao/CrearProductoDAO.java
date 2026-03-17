package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ============================================================
 * DAO: CrearProductoDAO
 * Tabla escrita:  productos
 * Usado por:      NuevoProductoServlet
 * ============================================================
 *
 */
public class CrearProductoDAO {

    /**
     * Inserta un nuevo producto en la BD y retorna su ID generado.
     *
     * @param nombre           nombre del producto (se aplica trim)
     * @param descripcion      descripción opcional (null se guarda como "")
     * @param stock            stock inicial (validado como >= 0 en el Servlet)
     * @param precio           precio unitario (BigDecimal para exactitud)
     * @param estado           "Disponible", "Agotado" o "Inactivo"
     * @param fechaVencimiento fecha en formato yyyy-MM-dd
     * @param idCategoria      FK a tabla categorias
     * @param idUnidad         FK a tabla unidad_medida
     * @return                 ID generado por AUTO_INCREMENT del nuevo producto
     * @throws SQLException    si hay error al insertar o si no se obtiene el ID
     */
    public int crear(String nombre, String descripcion, int stock,
                     BigDecimal precio, String estado, String fechaVencimiento,
                     int idCategoria, int idUnidad, int idEmprendimiento) throws SQLException {

        String sql = """
                INSERT INTO productos
                    (nombre, descripcion, stock_actual, precio_unitario,
                     estado, fecha_vencimiento, id_categoria, id_unidad, id_emprendimiento)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        // Statement.RETURN_GENERATED_KEYS indica al driver JDBC que guarde
        // el ID generado por AUTO_INCREMENT para recuperarlo después
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, nombre.trim());
            ps.setString(2, descripcion != null ? descripcion.trim() : ""); // null → string vacío
            ps.setInt(3, stock);
            ps.setBigDecimal(4, precio);
            ps.setString(5, estado);
            ps.setString(6, fechaVencimiento);
            ps.setInt(7, idCategoria);
            ps.setInt(8, idUnidad);
            ps.setInt(9, idEmprendimiento);
            ps.executeUpdate();

            // Recuperar el ID generado por MySQL
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }

        // Si executeUpdate() no lanzó excepción pero no hay ID generado,
        // algo inesperado ocurrió en la BD
        throw new SQLException("No se obtuvo el ID del producto creado.");
    }
}
