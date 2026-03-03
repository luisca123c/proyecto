package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * DAO para eliminar un usuario del sistema.
 *
 * Orden de eliminación (respetando las FK de la BD):
 *   1. Obtener id_correo e id_telefono del usuario
 *   2. Eliminar perfil_usuario → libera la FK hacia telefonos
 *   3. Eliminar telefonos
 *   4. Eliminar correos → CASCADE elimina usuarios y carrito
 *
 * Si el usuario tiene registros en detalle_compra (ON DELETE RESTRICT),
 * la BD lanzará SQLException y se hace rollback automáticamente.
 */
public class EliminarEmpleadoDAO {

    /*
     * Elimina el usuario y todos sus datos relacionados en una transacción.
     * Lanza SQLException si el usuario tiene compras registradas.
     */
    public void eliminar(int idUsuario) throws SQLException {

        // Paso 1: obtener los IDs relacionados antes de borrar
        int idCorreo   = -1;
        int idTelefono = -1;

        String sqlIds = """
                SELECT u.id_correo, p.id_telefono
                FROM usuarios u
                JOIN perfil_usuario p ON p.id_usuario = u.id
                WHERE u.id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlIds)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idCorreo   = rs.getInt("id_correo");
                    idTelefono = rs.getInt("id_telefono");
                }
            }
        }

        // Si no se encontró el usuario, no hay nada que eliminar
        if (idCorreo == -1) return;

        // Paso 2-4: eliminar en transacción para garantizar consistencia
        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                // Eliminar perfil_usuario (libera la referencia a telefonos)
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM perfil_usuario WHERE id_usuario = ?")) {
                    ps.setInt(1, idUsuario);
                    ps.executeUpdate();
                }

                // Eliminar teléfono
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM telefonos WHERE id = ?")) {
                    ps.setInt(1, idTelefono);
                    ps.executeUpdate();
                }

                // Eliminar correo → CASCADE elimina usuarios y carrito
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM correos WHERE id = ?")) {
                    ps.setInt(1, idCorreo);
                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException e) {
                con.rollback();
                throw e; // Re-lanzar para que el Servlet muestre el error
            }
        }
    }
}
