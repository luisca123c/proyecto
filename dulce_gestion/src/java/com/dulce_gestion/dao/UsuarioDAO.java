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
 * ============================================================
 * DAO: UsuarioDAO
 * Tabla principal:    usuarios
 * Tablas del JOIN:    correos, roles, perfil_usuario
 * Usado por:          LoginServlet
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Concentra las operaciones de autenticación y hashing de contraseñas.
 * Es el DAO raíz del sistema: sin él ningún usuario puede entrar.
 *
 * ¿POR QUÉ SE SEPARA LA LÓGICA DE BD DEL SERVLET?
 * -------------------------------------------------
 * LoginServlet solo debería leer el formulario, llamar al DAO y
 * decidir qué hacer con el resultado.
 * Si el SQL estuviera en el Servlet, cualquier cambio de esquema
 * (nueva tabla, nuevo JOIN) obligaría a tocar el Servlet.
 * Al estar aquí, el Servlet queda limpio y este DAO es reusable
 * desde cualquier parte del proyecto.
 *
 * ¿POR QUÉ hashSHA256() ES ESTÁTICO (static)?
 * ---------------------------------------------
 * Otros DAOs también hashean contraseñas (CrearEmpleadoDAO,
 * EditarEmpleadoDAO, PerfilDAO). Al ser estático pueden llamarlo
 * sin instanciar UsuarioDAO:
 *   UsuarioDAO.hashSHA256(contrasena)
 */
public class UsuarioDAO {

    /**
     * Autentica un usuario verificando correo y contraseña contra la BD.
     *
     * FLUJO INTERNO:
     * 1. Hashear la contraseña recibida con SHA-256.
     * 2. Buscar en la BD una fila donde correo + hash coincidan.
     * 3. Si hay resultado → construir y retornar el objeto Usuario.
     *    Si no → retornar null (credenciales inválidas).
     *
     * ¿POR QUÉ UN JOIN DE 4 TABLAS?
     * --------------------------------
     * La BD normaliza la información del usuario:
     *   usuarios        → id, estado, contrasena, id_rol, id_correo
     *   correos         → id, correo  (separado para evitar duplicados)
     *   roles           → id, nombre  (SuperAdministrador / Administrador / Empleado)
     *   perfil_usuario  → id, nombre_completo, id_usuario, ...
     *
     * Con un solo JOIN se trae todo lo necesario para poblar el objeto
     * Usuario y guardarlo en sesión, sin consultas adicionales.
     *
     * ¿POR QUÉ PreparedStatement CON "?"?
     * -------------------------------------
     * Previene inyección SQL. Si el correo fuera concatenado directamente
     * en el SQL, un usuario podría escribir: ' OR '1'='1 y acceder sin
     * contraseña. Con PreparedStatement, MySQL trata el input como valor
     * literal, nunca como parte del SQL.
     *
     * ¿POR QUÉ try-with-resources?
     * -----------------------------
     * Cierra la conexión automáticamente al salir del bloque, incluso
     * si ocurre una excepción. Sin esto → fuga de conexiones → el pool
     * se agota y la aplicación deja de responder.
     *
     * @param correo     correo del usuario (se normaliza con trim + toLowerCase)
     * @param contrasena contraseña en texto plano (se hashea internamente)
     * @return           objeto Usuario con todos sus datos, o null si las
     *                   credenciales son incorrectas
     * @throws SQLException si hay error al conectar o consultar la BD
     */
    public Usuario autenticar(String correo, String contrasena) throws SQLException {

        String hashContrasena = hashSHA256(contrasena);
        if (hashContrasena == null) return null;

        // JOIN entre 5 tablas para obtener todos los campos del usuario en una consulta.
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

            ps.setString(1, correo.trim().toLowerCase()); // Sin espacios y todo minúsculas
            ps.setString(2, hashContrasena);              // Hash de 64 chars hex

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Hay coincidencia → construir el objeto con los datos devueltos
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

        return null; // Sin coincidencia → credenciales incorrectas
    }

    /**
     * Convierte una cadena de texto a su hash SHA-256 en formato hexadecimal.
     *
     * ¿QUÉ ES SHA-256?
     * -----------------
     * Función hash criptográfica unidireccional: dada cualquier entrada,
     * produce siempre el mismo resultado de 64 caracteres hexadecimales.
     * No se puede "deshacer" el hash para obtener el texto original.
     *
     * Ejemplo:
     *   "hola123" → "a29c491e...8f4b2d6c"  (siempre el mismo resultado)
     *   "Hola123" → "2b7e151f...f4b3c2a1"  (completamente diferente)
     *
     * ¿POR QUÉ StandardCharsets.UTF_8?
     * ----------------------------------
     * texto.getBytes() sin argumento usa el charset del sistema operativo,
     * que varía entre Windows, Linux y Mac. Con UTF_8 explícito el hash
     * es idéntico en todas las plataformas: una contraseña creada en
     * Windows funciona igual en Linux.
     *
     * ¿POR QUÉ String.format("%02x", b)?
     * ------------------------------------
     * MessageDigest retorna 32 bytes en bruto. %02x convierte cada byte
     * a su representación hexadecimal de exactamente 2 dígitos:
     *   byte 10  → "0a"   (sin el cero a la izquierda quedaría "a")
     *   byte 255 → "ff"
     * 32 bytes × 2 chars/byte = 64 caracteres hex en total.
     *
     * ¿POR QUÉ RuntimeException Y NO SQLException?
     * ----------------------------------------------
     * SHA-256 está incluido en todos los JDKs desde Java 1.4.
     * Si no está disponible hay un problema grave en la JVM, no un
     * error de BD recuperable. RuntimeException no obliga a los
     * llamadores a capturar algo que nunca debería ocurrir.
     *
     * @param texto  cadena a hashear (contraseña en texto plano)
     * @return       hash SHA-256 en hexadecimal (64 caracteres)
     * @throws RuntimeException si SHA-256 no está disponible en la JVM
     */
    /**
     * Retorna el ID del primer administrador activo de un emprendimiento.
     * Se usa cuando el SuperAdmin registra un gasto/compra en nombre de
     * un emprendimiento — el registro queda a nombre del admin del negocio.
     * Si no hay admin activo, retorna el id del primer usuario activo.
     * Si no hay ninguno, retorna 0.
     */
    public int obtenerAdminDeEmprendimiento(int idEmprendimiento) throws java.sql.SQLException {
        // Primero intentar obtener un Administrador activo
        String sql = "SELECT u.id FROM usuarios u " +
                     "JOIN roles r ON r.id = u.id_rol " +
                     "WHERE u.id_emprendimiento = ? AND u.estado = 'Activo' " +
                     "AND r.nombre = 'Administrador' LIMIT 1";
        try (java.sql.Connection con = com.dulce_gestion.utils.DB.obtenerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEmprendimiento);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // Si no hay admin, usar cualquier usuario activo del emprendimiento
        String sql2 = "SELECT id FROM usuarios WHERE id_emprendimiento = ? AND estado = 'Activo' LIMIT 1";
        try (java.sql.Connection con = com.dulce_gestion.utils.DB.obtenerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(sql2)) {
            ps.setInt(1, idEmprendimiento);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Devuelve el idEmprendimiento real del usuario consultando la BD.
     * Se usa como fallback cuando la sesión tiene idEmprendimiento=0
     * por haber iniciado sesión antes del fix que lo cargaba en login.
     *
     * Si el usuario es SuperAdministrador devuelve 0 (sin emprendimiento).
     *
     * @param idUsuario ID del usuario en sesión
     * @return id_emprendimiento del usuario, o 0 si es SuperAdmin / no tiene
     */
    public int resolverIdEmprendimiento(int idUsuario) throws java.sql.SQLException {
        String sql = "SELECT id_emprendimiento FROM usuarios WHERE id = ?";
        try (java.sql.Connection con = com.dulce_gestion.utils.DB.obtenerConexion();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int val = rs.getInt("id_emprendimiento");
                    return rs.wasNull() ? 0 : val;
                }
            }
        }
        return 0;
    }

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
