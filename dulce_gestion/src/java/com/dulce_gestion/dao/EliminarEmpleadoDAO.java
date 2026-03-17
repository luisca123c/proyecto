package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: EliminarEmpleadoDAO
 * Tablas escritas:    perfil_usuario, telefonos, correos
 * Tablas en CASCADE:  usuarios, carrito (se borran automáticamente)
 * Usado por:          EliminarEmpleadoServlet
 * ============================================================
 *
 */
public class EliminarEmpleadoDAO {

    /**
     * Elimina el usuario y todos sus datos relacionados en una transacción.
     *
     */
    public void eliminar(int idUsuario) throws SQLException {

        // ── Pre-paso: obtener los IDs relacionados antes de borrar ────────
        // Una vez que se empiece a borrar, los JOINs necesarios para
        // resolver estos IDs ya no funcionarán.
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

        // ── Transacción: los 3 DELETEs son atómicos ───────────────────────
        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false);
            try {

                // ── Paso 1: borrar perfil_usuario ─────────────────────────
                // Libera la referencia hacia telefonos para poder borrarlo después
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM perfil_usuario WHERE id_usuario = ?")) {
                    ps.setInt(1, idUsuario);
                    ps.executeUpdate();
                }

                // ── Paso 2: borrar teléfono ───────────────────────────────
                // Ya seguro: perfil_usuario ya no lo referencia
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM telefonos WHERE id = ?")) {
                    ps.setInt(1, idTelefono);
                    ps.executeUpdate();
                }

                // ── Paso 3: borrar correo (CASCADE → usuarios y carrito) ──
                // La FK usuarios.id_correo tiene ON DELETE CASCADE:
                // MySQL borra automáticamente la fila en usuarios al borrar correos.
                // Si el usuario tiene compras (ON DELETE RESTRICT), este paso falla.
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM correos WHERE id = ?")) {
                    ps.setInt(1, idCorreo);
                    ps.executeUpdate();
                }

                con.commit(); // Todo bien → confirmar los 3 DELETEs

            } catch (SQLException e) {
                con.rollback(); // Algo falló → deshacer (restaurar perfil y teléfono)
                throw e;        // Re-lanzar para que el Servlet muestre el error
            }
        }
    }
}
