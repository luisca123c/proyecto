package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO especializado en la autenticación y gestión de usuarios.
 *
 * Esta clase maneja la autenticación de usuarios y operaciones
 * relacionadas con roles y emprendimientos, incluyendo seguridad
 * con hashing SHA-256 para contraseñas.
 *
 * Tabla principal: usuarios
 * Tablas del JOIN: correos, roles, perfil_usuario, emprendimientos
 *
 * Características importantes:
 * - Autenticación segura con hashing SHA-256
 * - Consultas complejas con múltiples JOINs para datos de sesión
 * - Normalización de datos (trim, lowercase) para correos
 * - Manejo de emprendimientos para SuperAdmin (puede ser null)
 * - Métodos helper para resolución de emprendimientos
 *
 * Usado por: LoginServlet
 */
public class UsuarioDAO {

    // =========================================================
    // AUTENTICACIÓN - Login de usuarios
    // =========================================================
    /**
     * Autentica un usuario verificando correo y contraseña contra la BD.
     *
     * Proceso de autenticación:
     * 1. Hashear la contraseña con SHA-256
     * 2. Normalizar el correo (trim + lowercase)
     * 3. Buscar usuario con múltiples JOINs para obtener datos completos
     * 4. Retornar objeto Usuario con datos de sesión si coincide
     *
     * @param correo     correo del usuario (normalizado internamente)
     * @param contrasena contraseña en texto plano
     * @return Usuario con todos sus datos de sesión, o null si no coincide
     * @throws SQLException si hay error en la consulta
     */
    public Usuario autenticar(String correo, String contrasena) throws SQLException {

        // Paso 1: Hashear la contraseña con SHA-256 para comparación segura
        String hashContrasena = hashSHA256(contrasena);
        if (hashContrasena == null) return null; // Error en hashring

        // Paso 2: Consulta con JOINs para obtener todos los datos necesarios para la sesión
        // LEFT JOIN emprendimientos porque SuperAdmin tiene id_emprendimiento = NULL
        String sql = """
                SELECT u.id,
                       c.correo,
                       u.estado,
                       u.id_rol,
                       r.nombre    AS nombre_rol,
                       p.nombre_completo,
                       COALESCE(u.id_emprendimiento, 0) AS id_emprendimiento,
                       e.nombre    AS nombre_emprendimiento
                FROM usuarios u
                JOIN correos           c ON c.id         = u.id_correo
                JOIN roles             r ON r.id         = u.id_rol
                JOIN perfil_usuario    p ON p.id_usuario = u.id
                LEFT JOIN emprendimientos e ON e.id      = u.id_emprendimiento
                WHERE c.correo     = ?
                  AND u.contrasena = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, correo.trim().toLowerCase()); // Normalizar correo: sin espacios y minúsculas
            ps.setString(2, hashContrasena);              // Hash de 64 caracteres hexadecimales

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Paso 3: Construir objeto Usuario con datos de sesión
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setCorreo(rs.getString("correo"));
                    u.setEstado(rs.getString("estado"));           // "Activo" o "Inactivo"
                    u.setIdRol(rs.getInt("id_rol"));
                    u.setNombreRol(rs.getString("nombre_rol"));    // "SuperAdministrador", etc.
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setIdEmprendimiento(rs.getInt("id_emprendimiento"));
                    u.setNombreEmprendimiento(rs.getString("nombre_emprendimiento"));
                    return u;
                }
            }
        }

        return null; // Sin coincidencia: credenciales incorrectas o usuario no existe
    }

    // =========================================================
    // EMPRENDIMIENTOS - Métodos para gestión de emprendimientos
    // =========================================================
    /**
     * Retorna el ID del primer administrador activo de un emprendimiento.
     *
     * Se usa cuando el SuperAdmin registra un gasto/compra en nombre de
     * un emprendimiento — el registro queda a nombre del admin del negocio.
     * Si no hay admin activo, retorna el ID del primer usuario activo.
     * Si no hay ninguno, retorna 0.
     *
     * @param idEmprendimiento ID del emprendimiento a consultar
     * @return ID del administrador activo, o 0 si no hay usuarios activos
     * @throws SQLException si hay error en la consulta
     */
    public int obtenerAdminDeEmprendimiento(int idEmprendimiento) throws java.sql.SQLException {
        // Paso 1: Intentar obtener un Administrador activo del emprendimiento
        String sql = "SELECT u.id FROM usuarios u " +
                     "JOIN roles r ON r.id = u.id_rol " +
                     "WHERE u.id_emprendimiento = ? AND u.estado = 'Activo' " +
                     "AND r.nombre = 'Administrador' LIMIT 1";
        try (java.sql.Connection con = com.dulce_gestion.utils.DB.obtenerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEmprendimiento);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1); // Retornar ID del admin encontrado
            }
        }
        
        // Paso 2: Si no hay admin, usar cualquier usuario activo del emprendimiento
        String sql2 = "SELECT id FROM usuarios WHERE id_emprendimiento = ? AND estado = 'Activo' LIMIT 1";
        try (java.sql.Connection con = com.dulce_gestion.utils.DB.obtenerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(sql2)) {
            ps.setInt(1, idEmprendimiento);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1); // Retornar ID del primer usuario activo
            }
        }
        return 0; // No hay usuarios activos en el emprendimiento
    }

    /**
     * Devuelve el idEmprendimiento real del usuario consultando la BD.
     *
     * Se usa como fallback cuando la sesión tiene idEmprendimiento=0
     * por haber iniciado sesión antes del fix que lo cargaba en login.
     *
     * Si el usuario es SuperAdministrador devuelve 0 (sin emprendimiento).
     *
     * @param idUsuario ID del usuario en sesión
     * @return id_emprendimiento del usuario, o 0 si es SuperAdmin / no tiene
     * @throws SQLException si hay error en la consulta
     */
    public int resolverIdEmprendimiento(int idUsuario) throws java.sql.SQLException {
        String sql = "SELECT id_emprendimiento FROM usuarios WHERE id = ?";
        try (java.sql.Connection con = com.dulce_gestion.utils.DB.obtenerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int val = rs.getInt("id_emprendimiento");
                    return rs.wasNull() ? 0 : val; // Retornar 0 si es NULL (SuperAdmin)
                }
            }
        }
        return 0; // Usuario no encontrado
    }

    // =========================================================
    // SEGURIDAD - Hashing de contraseñas
    // =========================================================
    /**
     * Convierte una cadena de texto a su hash SHA-256 en formato hexadecimal.
     *
     * Proceso de hashing:
     * 1. Obtener instancia de MessageDigest con algoritmo SHA-256
     * 2. Convertir texto a bytes UTF-8 y calcular hash (32 bytes)
     * 3. Convertir cada byte a representación hexadecimal de 2 dígitos
     * 4. Retornar cadena de 64 caracteres hexadecimales
     *
     * @param texto texto plano a hashear (ej: contraseña)
     * @return hash SHA-256 en formato hexadecimal (64 caracteres),
     *         o lanza RuntimeException si SHA-256 no está disponible
     */
    public static String hashSHA256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Convertir el texto a bytes UTF-8 y calcular el hash (array de 32 bytes)
            byte[] bytes = md.digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convertir cada byte a su representación hex de 2 dígitos
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b)); // Ej: byte 10 → "0a"
            }
            return sb.toString(); // Cadena de 64 caracteres hexadecimales

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está en Java estándar. Si llega aquí el JDK está roto.
            throw new RuntimeException("SHA-256 no disponible.", e);
        }
    }
}
