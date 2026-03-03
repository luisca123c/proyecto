package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * DAO (Data Access Object) de usuarios.
 * Concentra las consultas SQL de la tabla usuarios,
 * separando esa logica de los Servlets.
 */
public class UsuarioDAO {

    /*
     * Busca un usuario en la BD con el correo y contrasena dados.
     * La contrasena se hashea con SHA-256 antes de compararla,
     * porque en la BD nunca se guarda en texto plano.
     *
     * Usa PreparedStatement con "?" para evitar inyeccion SQL.
     * Retorna el Usuario si las credenciales son correctas, null si no.
     */
    public Usuario autenticar(String correo, String contrasena) throws SQLException {

        String hashContrasena = hashSHA256(contrasena);
        if (hashContrasena == null) return null;

        // JOIN entre 4 tablas para traer todos los datos del usuario en una sola consulta
        String sql = """
                SELECT u.id,
                       c.correo,
                       u.estado,
                       u.id_rol,
                       r.nombre    AS nombre_rol,
                       p.nombre_completo
                FROM usuarios u
                JOIN correos        c ON c.id         = u.id_correo
                JOIN roles          r ON r.id         = u.id_rol
                JOIN perfil_usuario p ON p.id_usuario = u.id
                WHERE c.correo     = ?
                  AND u.contrasena = ?
                """;

        // try-with-resources cierra la conexion automaticamente al salir del bloque
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

    /*
     * Convierte un texto a su hash SHA-256 en hexadecimal (64 caracteres).
     * Es unidireccional: no se puede recuperar la contrasena original a partir del hash.
     */
    public static String hashSHA256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b)); // Cada byte a 2 digitos hex
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible.", e);
        }
    }
}
