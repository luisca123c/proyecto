package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO especializado en la eliminación lógica de productos.
 *
 * Esta clase maneja el proceso de inactivación de productos mediante
 * eliminación lógica, preservando el historial de ventas y relaciones.
 *
 * Tabla modificada: productos (UPDATE estado = 'Inactivo')
 * Usado por: EliminarProductoServlet
 *
 * Características importantes:
 * - Eliminación lógica: no borra registros físicamente
 * - Preservación de historial: mantiene ventas y transacciones
 * - Simplicidad: una sola operación UPDATE
 * - Seguridad: mantiene integridad referencial
 */
public class EliminarProductoDAO {

    /**
     * Inactiva el producto con el ID especificado mediante eliminación lógica.
     *
     * Este método implementa eliminación lógica en lugar de física por las siguientes razones:
     * - Preserva el historial de ventas y transacciones del producto
     * - Mantiene la integridad referencial con detalle_carrito y ventas
     * - Permite reactivar el producto si es necesario en el futuro
     * - Cumple con requisitos de auditoría y cumplimiento normativo
     *
     * Un producto inactivo no aparecerá en catálogo, no podrá agregarse
     * al carrito ni ser vendido, pero sus datos permanecerán en el
     * sistema para fines históricos y reportes.
     *
     * @param idProducto ID del producto que se desea inactivar
     * @throws SQLException si ocurre error al actualizar la base de datos
     */
    public void inactivar(int idProducto) throws SQLException {
        String sql = "UPDATE productos SET estado = 'Inactivo' WHERE id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto); // Asignar el ID del producto a inactivar
            ps.executeUpdate(); // Ejecutar la actualización del estado
        }
    }
}
