package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO especializado en la eliminación lógica de empleados.
 *
 * Esta clase maneja el proceso de inactivación de usuarios mediante
 * eliminación lógica, preservando el historial y relaciones.
 *
 * Tabla modificada: usuarios (UPDATE estado = 'Inactivo')
 * Usado por: EliminarEmpleadoServlet
 *
 * Características importantes:
 * - Eliminación lógica: no borra registros físicamente
 * - Preservación de historial: mantiene ventas y transacciones
 * - Simplicidad: una sola operación UPDATE
 * - Seguridad: mantiene integridad// =========================================================
    // ELIMINACIÓN LÓGICA - Inactivación de empleados
    // =========================================================
 *
 * Características importantes:
 * - Eliminación lógica: no borra registros físicamente
 * - Preservación de historial: mantiene ventas y transacciones
 * - Simplicidad: una sola operación UPDATE
 * - Seguridad: mantiene integridad referencial
 */
public class EliminarEmpleadoDAO {

    /**
     * Inactiva el usuario con el ID especificado mediante eliminación lógica.
     *
     * Este método implementa eliminación lógica en lugar de física por las siguientes razones:
     * - Preserva el historial de ventas y transacciones del usuario
     * - Mantiene la integridad referencial con otras tablas
     * - Permite reactivar el usuario si es necesario en el futuro
     * - Cumple con requisitos de auditoría y cumplimiento normativo
     *
     * El usuario inactivado no podrá iniciar sesión pero sus datos
     * permanecerán en el sistema para fines históricos.
     *
     * @param idUsuario ID del usuario que se desea inactivar
     * @throws SQLException si ocurre error al actualizar la base de datos
     */
    public void inactivar(int idUsuario) throws SQLException {
        String sql = "UPDATE usuarios SET estado = 'Inactivo' WHERE id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario); // Asignar el ID del usuario a inactivar
            ps.executeUpdate(); // Ejecutar la actualización del estado
        }
    }
}
