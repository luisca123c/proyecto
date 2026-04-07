package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO especializado en la edición y gestión de empleados existentes.
 *
 * Esta clase maneja las operaciones de consulta, validación y actualización
 * de datos de empleados a través de múltiples tablas relacionadas.
 *
 * Tablas modificadas: correos, telefonos, perfil_usuario, usuarios
 * Tablas consultadas: roles, generos (en UPDATE via JOIN)
 * Usado por: EditarEmpleadoServlet, EliminarEmpleadoServlet
 *
 * Características importantes:
 * - Consultas complejas con JOINs de múltiples tablas
 * - Validación de unicidad excluyendo el registro actual
 * - Actualizaciones transaccionales de 4 tablas
 * - Uso de COALESCE para manejo de valores nulos
 */
public class EditarEmpleadoDAO {

    /**
     * Busca un usuario por ID y retorna sus datos completos.
     * Se usa para prellenar el formulario de edición en las peticiones GET.
     *
     * El JOIN de 6 tablas trae toda la información necesaria:
     * - usuarios: datos principales (id, estado, rol, emprendimiento)
     * - correos: correo electrónico
     * - perfil_usuario: nombre completo
     * - telefonos: número telefónico
     * - roles: nombre del rol
     * - generos: nombre del género
     * - emprendimientos: nombre del emprendimiento (opcional)
     *
     * COALESCE: Función SQL que devuelve el primer valor NO NULL.
     * Se usa para id_emprendimiento: si es NULL devuelve 0, sino el valor real.
     *
     * @param id ID del usuario a buscar
     * @return objeto Usuario con todos sus datos, o null si no existe
     * @throws SQLException si hay error al consultar la base de datos
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
                       g.nombre    AS genero,
                       COALESCE(u.id_emprendimiento, 0) AS id_emprendimiento,
                       e.nombre    AS nombre_emprendimiento
                FROM usuarios u
                JOIN correos        c ON c.id         = u.id_correo
                JOIN roles          r ON r.id         = u.id_rol
                JOIN perfil_usuario p ON p.id_usuario = u.id
                JOIN telefonos      t ON t.id         = p.id_telefono
                JOIN generos        g ON g.id         = p.id_genero
                LEFT JOIN emprendimientos e ON e.id   = u.id_emprendimiento
                WHERE u.id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Mapear ResultSet a objeto Usuario con todos los campos
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setCorreo(rs.getString("correo"));
                    u.setEstado(rs.getString("estado"));
                    u.setIdRol(rs.getInt("id_rol"));
                    u.setNombreRol(rs.getString("nombre_rol"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setTelefono(rs.getString("telefono"));
                    u.setGenero(rs.getString("genero"));
                    u.setIdEmprendimiento(rs.getInt("id_emprendimiento"));
                    // El nombre del emprendimiento puede ser null (LEFT JOIN)
                    try { u.setNombreEmprendimiento(rs.getString("nombre_emprendimiento")); } catch (Exception ignored) {}
                    return u;
                }
            }
        }
        return null; // El usuario no existe
    }

    /**
     * Verifica si un correo ya existe en la base de datos en un usuario distinto.
     *
     * Esta validación es crucial durante la edición para permitir que el usuario
     * mantenga su correo actual pero evitar que use uno ya existente.
     *
     * El JOIN con usuarios permite excluir el propio registro del usuario
     * que se está editando (WHERE u.id != idUsuarioActual).
     *
     * @param correo          correo a verificar (se normaliza a minúsculas)
     * @param idUsuarioActual ID del usuario que se está editando (se excluye)
     * @return                true si el correo ya lo usa otro usuario
     * @throws SQLException   si hay error al consultar la base de datos
     */
    public boolean correoExisteEnOtro(String correo, int idUsuarioActual) throws SQLException {
        String sql = """
                SELECT c.id FROM correos c
                JOIN usuarios u ON u.id_correo = c.id
                WHERE c.correo = ? AND u.id != ?
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase()); // Normalizar correo
            ps.setInt(2, idUsuarioActual); // Excluir el usuario actual
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true si hay coincidencia con otro usuario
            }
        }
    }

    /**
     * Verifica si un teléfono ya existe en la base de datos en un usuario distinto.
     *
     * Funciona de manera similar a correoExisteEnOtro(), usando JOIN
     * para excluir el registro actual del usuario que se está editando.
     *
     * @param telefono        teléfono a verificar (se normaliza con trim)
     * @param idUsuarioActual ID del usuario que se está editando (se excluye)
     * @return                true si el teléfono ya lo usa otro usuario
     * @throws SQLException   si hay error al consultar la base de datos
     */
    public boolean telefonoExisteEnOtro(String telefono, int idUsuarioActual) throws SQLException {
        String sql = """
                SELECT t.id FROM telefonos t
                JOIN perfil_usuario p ON p.id_telefono = t.id
                WHERE t.telefono = ? AND p.id_usuario != ?
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, telefono.trim()); // Eliminar espacios en blanco
            ps.setInt(2, idUsuarioActual); // Excluir el usuario actual
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true si hay coincidencia con otro usuario
            }
        }
    }

    /**
     * Actualiza los datos del usuario en 4 tablas dentro de una transacción.
     *
     * ORDEN DE ACTUALIZACIÓN (respetando claves foráneas):
     * 1. correos         → UPDATE usando JOIN con usuarios para localizar la fila
     * 2. telefonos       → UPDATE usando JOIN con perfil_usuario
     * 3. perfil_usuario  → UPDATE nombre_completo y id_genero
     * 4. usuarios        → UPDATE estado, y opcionalmente contrasena y id_rol
     *
     * Transacción ACID: todos los UPDATEs se ejecutan o ninguno.
     *
     * @param idUsuario        ID del usuario a actualizar
     * @param nombreCompleto    nuevo nombre completo
     * @param telefono         nuevo número telefónico
     * @param genero           nuevo género (nombre, se convierte a ID)
     * @param correo           nuevo correo (se normaliza a minúsculas)
     * @param nuevaContrasena nueva contraseña (null = no cambiar)
     * @param estado           nuevo estado del usuario
     * @param rol              nuevo rol (solo para SuperAdmin)
     * @param esSuperAdmin      true si puede cambiar rol y emprendimiento
     * @param idEmprendimiento ID del emprendimiento (solo para SuperAdmin)
     * @throws SQLException si hay error en cualquier paso de la transacción
     */
    public void actualizar(int idUsuario, String nombreCompleto, String telefono,
                           String genero, String correo, String nuevaContrasena,
                           String estado, String rol, boolean esSuperAdmin,
                           int idEmprendimiento) throws SQLException {

        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false); // Iniciar transacción: los 4 UPDATEs son atómicos
            try {

                // ── Paso 1: actualizar correo (entidad independiente) ───────────────────────
                // UPDATE con JOIN: localiza la fila en correos a través de usuarios
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE correos c JOIN usuarios u ON u.id_correo = c.id " +
                        "SET c.correo = ? WHERE u.id = ?")) {
                    ps.setString(1, correo.trim().toLowerCase()); // Normalizar correo a minúsculas
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }

                // ── Paso 2: actualizar teléfono (entidad independiente) ───────────────────────
                // UPDATE con JOIN: localiza la fila en telefonos a través de perfil_usuario
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE telefonos t JOIN perfil_usuario p ON p.id_telefono = t.id " +
                        "SET t.telefono = ? WHERE p.id_usuario = ?")) {
                    ps.setString(1, telefono.trim()); // Eliminar espacios en blanco
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }

                // ── Paso 3: actualizar perfil (nombre + género) ───────────────────────────
                // JOIN con generos para convertir nombre de género a su ID en un solo UPDATE
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE perfil_usuario p " +
                        "JOIN generos g ON g.nombre = ? " +
                        "SET p.nombre_completo = ?, p.id_genero = g.id " +
                        "WHERE p.id_usuario = ?")) {
                    ps.setString(1, genero); // Nombre del género para buscar ID
                    ps.setString(2, nombreCompleto.trim()); // Eliminar espacios
                    ps.setInt(3, idUsuario);
                    ps.executeUpdate();
                }

                // ── Paso 4: actualizar usuarios (estado + rol + contraseña opcional) ─────────────
                // Se construyen 2 queries según 2 variables binarias:
                if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
                    // Hay nueva contraseña → hashear antes de guardar
                    String hash = UsuarioDAO.hashSHA256(nuevaContrasena);
                    String sqlU = esSuperAdmin
                        ? "UPDATE usuarios SET estado = ?, contrasena = ?, id_rol = (SELECT id FROM roles WHERE nombre = ?), id_emprendimiento = ? WHERE id = ?"
                        : "UPDATE usuarios SET estado = ?, contrasena = ? WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlU)) {
                        ps.setString(1, estado);
                        ps.setString(2, hash); // Contraseña hasheada SHA-256
                        if (esSuperAdmin) { 
                            ps.setString(3, rol); // Nuevo rol
                            ps.setInt(4, idEmprendimiento); // Nuevo emprendimiento
                            ps.setInt(5, idUsuario); 
                        } else { 
                            ps.setInt(3, idUsuario); 
                        }
                        ps.executeUpdate();
                    }
                } else {
                    // Sin nueva contraseña → omitir la columna contrasena del UPDATE
                    String sqlU = esSuperAdmin
                        ? "UPDATE usuarios SET estado = ?, id_rol = (SELECT id FROM roles WHERE nombre = ?), id_emprendimiento = ? WHERE id = ?"
                        : "UPDATE usuarios SET estado = ? WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlU)) {
                        ps.setString(1, estado);
                        if (esSuperAdmin) { 
                            ps.setString(2, rol); // Nuevo rol
                            ps.setInt(3, idEmprendimiento); // Nuevo emprendimiento
                            ps.setInt(4, idUsuario); 
                        } else { 
                            ps.setInt(2, idUsuario); 
                        }
                        ps.executeUpdate();
                    }
                }

                con.commit(); // Todos los UPDATEs exitosos → confirmar la transacción completa

            } catch (SQLException e) {
                con.rollback(); // Algo falló → deshacer todos los cambios
                throw e; // Re-lanzar para que el Servlet muestre el error apropiado
            }
        }
    }
}
