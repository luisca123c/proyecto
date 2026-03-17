package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: EditarProductoDAO
 * Tabla escrita:  productos
 * Usado por:      EditarProductoServlet
 * ============================================================
 *
 */
public class EditarProductoDAO {

    /**
     * Actualiza todos los campos de datos de un producto.
     *
     * El UPDATE incluye 8 campos. Si alguno no cambió, igual se actualiza
     * con el mismo valor (no se hace UPDATE selectivo por campo modificado),
     * lo cual simplifica el código sin impacto significativo en rendimiento
     * para tablas de este tamaño.
     *
     * @param id               ID del producto a actualizar
     * @param nombre           nuevo nombre
     * @param descripcion      nueva descripción (null se guarda como "")
     * @param stock            nuevo stock
     * @param precio           nuevo precio (BigDecimal para exactitud monetaria)
     * @param estado           nuevo estado ("Disponible", "Agotado", "Inactivo")
     * @param fechaVencimiento nueva fecha en formato yyyy-MM-dd
     * @param idCategoria      nueva categoría (FK)
     * @param idUnidad         nueva unidad de medida (FK)
     * @throws SQLException    si hay error al actualizar o si el ID no existe
     */
    /**
     * Actualiza los datos del producto. Si esSuperAdmin e idEmprendimiento > 0,
     * también reasigna el emprendimiento.
     */
    public void actualizar(int id, String nombre, String descripcion, int stock,
                           BigDecimal precio, String estado, String fechaVencimiento,
                           int idCategoria, int idUnidad,
                           boolean esSuperAdmin, int idEmprendimiento) throws SQLException {

        String sql = esSuperAdmin && idEmprendimiento > 0
            ? """
                UPDATE productos
                SET nombre = ?, descripcion = ?, stock_actual = ?,
                    precio_unitario = ?, estado = ?, fecha_vencimiento = ?,
                    id_categoria = ?, id_unidad = ?, id_emprendimiento = ?
                WHERE id = ?
                """
            : """
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
            if (esSuperAdmin && idEmprendimiento > 0) {
                ps.setInt(9, idEmprendimiento);
                ps.setInt(10, id);
            } else {
                ps.setInt(9, id);
            }
            ps.executeUpdate();
        }
    }

    /** Retrocompatibilidad: actualiza sin cambiar el emprendimiento. */
    public void actualizar(int id, String nombre, String descripcion, int stock,
                           BigDecimal precio, String estado, String fechaVencimiento,
                           int idCategoria, int idUnidad) throws SQLException {
        actualizar(id, nombre, descripcion, stock, precio, estado,
                   fechaVencimiento, idCategoria, idUnidad, false, 0);
    }
}
