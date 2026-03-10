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
 * ¿QUÉ HACE?
 * ----------
 * Elimina un usuario y todos sus datos relacionados en una transacción,
 * respetando el orden que imponen las claves foráneas de la BD.
 *
 * ¿POR QUÉ NO SE PUEDE BORRAR DIRECTO CON DELETE FROM usuarios?
 * --------------------------------------------------------------
 * La BD tiene restricciones de integridad referencial (FK):
 *
 *   perfil_usuario.id_telefono → telefonos.id  (RESTRICT o CASCADE)
 *   usuarios.id_correo         → correos.id    (si correos se borra → CASCADE a usuarios)
 *
 * Si se borrara correos antes que perfil_usuario, la FK de perfil_usuario
 * hacia usuarios fallaría porque usuarios ya no existe.
 * Si se borrara directamente usuarios, podría quedar perfil_usuario huérfano.
 *
 * El orden correcto es:
 *   1. Obtener idCorreo e idTelefono (antes de borrar usuarios, que los referencia)
 *   2. Borrar perfil_usuario (libera la FK hacia telefonos)
 *   3. Borrar telefonos
 *   4. Borrar correos → CASCADE borra automáticamente usuarios y carrito
 *
 * ¿QUÉ PASA SI EL USUARIO TIENE COMPRAS REGISTRADAS?
 * ----------------------------------------------------
 * La tabla detalle_compra tiene:
 *   detalle_compra.id_usuario → usuarios.id  (ON DELETE RESTRICT)
 *
 * Si el usuario tiene compras, MySQL rechaza el DELETE en correos
 * (que haría CASCADE a usuarios) con una excepción de FK violada.
 * El ROLLBACK deshace los DELETEs previos y el Servlet muestra el error.
 */
public class EliminarEmpleadoDAO {

    /**
     * Elimina el usuario y todos sus datos relacionados en una transacción.
     *
     * FLUJO PASO A PASO:
     *
     * Pre-paso — Consultar idCorreo e idTelefono:
     *   Se necesitan ANTES de borrar porque el JOIN que los resuelve
     *   requiere que usuarios y perfil_usuario aún existan.
     *
     * Paso 1 — DELETE perfil_usuario:
     *   Libera las FKs de perfil_usuario hacia telefonos y generos.
     *   Sin este paso, el DELETE de telefonos fallaría.
     *
     * Paso 2 — DELETE telefonos:
     *   Ahora seguro porque perfil_usuario ya no lo referencia.
     *
     * Paso 3 — DELETE correos:
     *   La FK usuarios.id_correo tiene ON DELETE CASCADE →
     *   MySQL borra automáticamente la fila en usuarios.
     *   La FK carrito.id_usuario también puede tener CASCADE →
     *   se borran el carrito y sus detalles automáticamente.
     *
     * Si el usuario tiene compras (detalle_compra) con ON DELETE RESTRICT,
     * el paso 3 falla → ROLLBACK → los pasos 1 y 2 se deshacen.
     *
     * @param idUsuario  ID del usuario a eliminar
     * @throws SQLException si la BD rechaza la eliminación (ej: tiene compras)
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
