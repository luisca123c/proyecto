package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: EliminarEmpleadoDAO
 * Tabla escrita:  usuarios (UPDATE estado = 'Inactivo')
 * Usado por:      EliminarEmpleadoServlet
 *
 * La eliminación es LÓGICA: cambia el estado del usuario a
 * 'Inactivo' en vez de borrar el registro físicamente.
 * Esto preserva el historial de ventas y transacciones.
 * ============================================================
 */
public class EliminarEmpleadoDAO {

    /**
     * Inactiva el usuario con el ID dado (eliminación lógica).
     *
     * @param idUsuario  ID del usuario a inactivar
     * @throws SQLException si ocurre un error de base de datos
     */
    public void inactivar(int idUsuario) throws SQLException {
        String sql = "UPDATE usuarios SET estado = 'Inactivo' WHERE id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }
}
