package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * DAO para obtener y actualizar información de perfil de usuarios
 */
public class PerfilDAO {

    /*
     * Obtiene el perfil completo de un usuario por su ID
     */
    public Usuario obtenerPerfil(int idUsuario) throws SQLException {
        String sql = """
                SELECT 
                    u.id,
                    c.correo,
                    u.estado,
                    u.id_rol,
                    r.nombre AS nombre_rol,
                    p.nombre_completo,
                    t.telefono,
                    g.nombre AS nombre_genero,
                    p.id_genero,
                    p.id_telefono,
                    p.fecha_creacion,
                    p.fecha_actualizacion
                FROM usuarios u
                JOIN correos c ON c.id = u.id_correo
                JOIN roles r ON r.id = u.id_rol
                JOIN perfil_usuario p ON p.id_usuario = u.id
                JOIN telefonos t ON t.id = p.id_telefono
                JOIN generos g ON g.id = p.id_genero
                WHERE u.id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    /*
     * Obtiene lista de todos los usuarios (para SuperAdmin)
     */
    public List<Usuario> listarTodosUsuarios() throws SQLException {
        String sql = """
                SELECT 
                    u.id,
                    c.correo,
                    u.estado,
                    u.id_rol,
                    r.nombre AS nombre_rol,
                    p.nombre_completo,
                    t.telefono,
                    g.nombre AS nombre_genero,
                    p.id_genero,
                    p.id_telefono
                FROM usuarios u
                JOIN correos c ON c.id = u.id_correo
                JOIN roles r ON r.id = u.id_rol
                JOIN perfil_usuario p ON p.id_usuario = u.id
                JOIN telefonos t ON t.id = p.id_telefono
                JOIN generos g ON g.id = p.id_genero
                ORDER BY p.nombre_completo
                """;

        List<Usuario> usuarios = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }
        }
        return usuarios;
    }

    /*
     * Obtiene lista de generos para select HTML
     */
    public List<String[]> listarGeneros() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM generos ORDER BY nombre";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
            }
        }
        return lista;
    }

    /*
     * Actualiza el perfil del usuario (nombre, teléfono, género, correo)
     * Retorna true si la actualización fue exitosa
     */
    public boolean actualizarPerfil(int idUsuario, String nombreCompleto, 
                                     String telefono, int idGenero, String correo) throws SQLException {
        
        // Primero obtener IDs de correo, teléfono e ID de perfil
        String sqlObtener = """
                SELECT u.id_correo, p.id_telefono, p.id
                FROM usuarios u
                JOIN perfil_usuario p ON p.id_usuario = u.id
                WHERE u.id = ?
                """;

        int idCorreo = -1;
        int idTelefono = -1;
        int idPerfil = -1;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlObtener)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idCorreo = rs.getInt("id_correo");
                    idTelefono = rs.getInt("id_telefono");
                    idPerfil = rs.getInt("id");
                }
            }
        }

        if (idCorreo == -1 || idTelefono == -1 || idPerfil == -1) {
            return false;
        }

        // Actualizar correo
        String sqlCorreo = "UPDATE correos SET correo = ? WHERE id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlCorreo)) {
            ps.setString(1, correo.toLowerCase().trim());
            ps.setInt(2, idCorreo);
            ps.executeUpdate();
        }

        // Actualizar teléfono
        String sqlTelefono = "UPDATE telefonos SET telefono = ? WHERE id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlTelefono)) {
            ps.setString(1, telefono.trim());
            ps.setInt(2, idTelefono);
            ps.executeUpdate();
        }

        // Actualizar perfil (nombre, género)
        String sqlPerfil = """
                UPDATE perfil_usuario 
                SET nombre_completo = ?, 
                    id_genero = ?,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlPerfil)) {
            ps.setString(1, nombreCompleto.trim());
            ps.setInt(2, idGenero);
            ps.setInt(3, idPerfil);
            ps.executeUpdate();
        }

        return true;
    }

    /*
     * Cambia la contraseña del usuario
     * Valida que la contraseña actual sea correcta
     */
    public boolean cambiarContrasena(int idUsuario, String contrasennaActual, 
                                      String contrasenaNueva) throws SQLException {
        
        // Hashear las contraseñas
        String hashActual = UsuarioDAO.hashSHA256(contrasennaActual);
        String hashNueva = UsuarioDAO.hashSHA256(contrasenaNueva);

        if (hashActual == null || hashNueva == null) {
            return false;
        }

        // Verificar que la contraseña actual sea correcta
        String sqlVerificar = "SELECT id FROM usuarios WHERE id = ? AND contrasena = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlVerificar)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, hashActual);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // La contraseña actual no coincide
                    return false;
                }
            }
        }

        // Actualizar a la nueva contraseña
        String sqlActualizar = "UPDATE usuarios SET contrasena = ? WHERE id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlActualizar)) {
            ps.setString(1, hashNueva);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }

        return true;
    }

    /*
     * Mapea un ResultSet a objeto Usuario
     */
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setCorreo(rs.getString("correo"));
        u.setEstado(rs.getString("estado"));
        u.setIdRol(rs.getInt("id_rol"));
        u.setNombreRol(rs.getString("nombre_rol"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setTelefono(rs.getString("telefono"));
        u.setGenero(rs.getString("nombre_genero"));
        return u;
    }
}
