package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * DAO para editar un usuario existente.
 * Permite modificar: nombre, teléfono, género, correo, estado.
 * SuperAdmin puede además cambiar el rol.
 * La contraseña solo se actualiza si se envía un valor nuevo.
 */
public class EditarEmpleadoDAO {

    /*
     * Busca un usuario por ID y retorna sus datos para prellenar el formulario.
     */
    public Usuario buscarPorId(int id) throws SQLException {
        String sql = """
                SELECT u.id,
                       c.correo,
                       u.estado,
                       u.id_rol,
                       r.nombre    AS nombre_rol,
                       p.nombre_completo,
                       t.telefono,
                       g.nombre    AS genero
                FROM usuarios u
                JOIN correos        c ON c.id         = u.id_correo
                JOIN roles          r ON r.id         = u.id_rol
                JOIN perfil_usuario p ON p.id_usuario = u.id
                JOIN telefonos      t ON t.id         = p.id_telefono
                JOIN generos        g ON g.id         = p.id_genero
                WHERE u.id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setCorreo(rs.getString("correo"));
                    u.setEstado(rs.getString("estado"));
                    u.setIdRol(rs.getInt("id_rol"));
                    u.setNombreRol(rs.getString("nombre_rol"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setTelefono(rs.getString("telefono"));
                    u.setGenero(rs.getString("genero"));
                    return u;
                }
            }
        }
        return null;
    }

    /*
     * Verifica si un correo ya existe en otro usuario distinto al que se edita.
     */
    public boolean correoExisteEnOtro(String correo, int idUsuarioActual) throws SQLException {
        String sql = """
                SELECT c.id FROM correos c
                JOIN usuarios u ON u.id_correo = c.id
                WHERE c.correo = ? AND u.id != ?
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase());
            ps.setInt(2, idUsuarioActual);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /*
     * Verifica si un teléfono ya existe en otro usuario distinto al que se edita.
     */
    public boolean telefonoExisteEnOtro(String telefono, int idUsuarioActual) throws SQLException {
        String sql = """
                SELECT t.id FROM telefonos t
                JOIN perfil_usuario p ON p.id_telefono = t.id
                WHERE t.telefono = ? AND p.id_usuario != ?
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, telefono.trim());
            ps.setInt(2, idUsuarioActual);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /*
     * Actualiza los datos del usuario en una transacción.
     * Si nuevaContrasena está vacía, no se cambia.
     * Si esSuperAdmin es false, no se permite cambiar el rol.
     */
    public void actualizar(int idUsuario, String nombreCompleto, String telefono,
                           String genero, String correo, String nuevaContrasena,
                           String estado, String rol, boolean esSuperAdmin) throws SQLException {

        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                // 1. Actualizar correo
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE correos c JOIN usuarios u ON u.id_correo = c.id " +
                        "SET c.correo = ? WHERE u.id = ?")) {
                    ps.setString(1, correo.trim().toLowerCase());
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }

                // 2. Actualizar teléfono
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE telefonos t JOIN perfil_usuario p ON p.id_telefono = t.id " +
                        "SET t.telefono = ? WHERE p.id_usuario = ?")) {
                    ps.setString(1, telefono.trim());
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }

                // 3. Actualizar perfil_usuario (nombre y género)
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE perfil_usuario p " +
                        "JOIN generos g ON g.nombre = ? " +
                        "SET p.nombre_completo = ?, p.id_genero = g.id " +
                        "WHERE p.id_usuario = ?")) {
                    ps.setString(1, genero);
                    ps.setString(2, nombreCompleto.trim());
                    ps.setInt(3, idUsuario);
                    ps.executeUpdate();
                }

                // 4. Actualizar usuarios (estado, rol y opcionalmente contraseña)
                if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
                    String hash = UsuarioDAO.hashSHA256(nuevaContrasena);
                    String sqlU = esSuperAdmin
                        ? "UPDATE usuarios SET estado = ?, contrasena = ?, id_rol = (SELECT id FROM roles WHERE nombre = ?) WHERE id = ?"
                        : "UPDATE usuarios SET estado = ?, contrasena = ? WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlU)) {
                        ps.setString(1, estado);
                        ps.setString(2, hash);
                        if (esSuperAdmin) {
                            ps.setString(3, rol);
                            ps.setInt(4, idUsuario);
                        } else {
                            ps.setInt(3, idUsuario);
                        }
                        ps.executeUpdate();
                    }
                } else {
                    String sqlU = esSuperAdmin
                        ? "UPDATE usuarios SET estado = ?, id_rol = (SELECT id FROM roles WHERE nombre = ?) WHERE id = ?"
                        : "UPDATE usuarios SET estado = ? WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlU)) {
                        ps.setString(1, estado);
                        if (esSuperAdmin) {
                            ps.setString(2, rol);
                            ps.setInt(3, idUsuario);
                        } else {
                            ps.setInt(2, idUsuario);
                        }
                        ps.executeUpdate();
                    }
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
