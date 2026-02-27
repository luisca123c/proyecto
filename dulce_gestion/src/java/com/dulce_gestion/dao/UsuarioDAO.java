package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utls.DB;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO para operaciones de la tabla usuarios.
 */
public class UsuarioDAO {

    /**
     * Busca un usuario por correo y contraseña (hasheada en SHA-256).
     * Retorna el Usuario si las credenciales son correctas, null si no.
     */
    public Usuario autenticar(String correo, String contrasena) throws SQLException {

        String hashContrasena = hashSHA256(contrasena);
        if (hashContrasena == null) return null;

        String sql = """
                SELECT u.id,
                       c.correo,
                       u.estado,
                       u.id_rol,
                       r.nombre    AS nombre_rol,
                       p.nombre_completo
                FROM usuarios u
                JOIN correos        c ON c.id        = u.id_correo
                JOIN roles          r ON r.id        = u.id_rol
                JOIN perfil_usuario p ON p.id_usuario = u.id
                WHERE c.correo     = ?
                  AND u.contrasena = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, correo.trim().toLowerCase());
            ps.setString(2, hashContrasena);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setCorreo(rs.getString("correo"));
                    u.setEstado(rs.getString("estado"));
                    u.setIdRol(rs.getInt("id_rol"));
                    u.setNombreRol(rs.getString("nombre_rol"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    return u;
                }
            }
        }

        return null;
    }

    /**
     * Genera el hash SHA-256 de un texto y lo devuelve en hexadecimal.
     * Usa solo el JDK, sin librerías externas.
     */
    public static String hashSHA256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }
}
