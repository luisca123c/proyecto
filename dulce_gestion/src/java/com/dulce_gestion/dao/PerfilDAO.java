package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO especializado en la gestión de perfiles de usuarios.
 *
 * Esta clase maneja las operaciones CRUD para perfiles de usuarios,
 * incluyendo datos personales, contacto y gestión de contraseñas.
 *
 * Tablas leídas: usuarios, correos, roles, perfil_usuario, telefonos, generos, emprendimientos
 * Tablas escritas: correos, telefonos, perfil_usuario, usuarios
 *
 * Características importantes:
 * - Consultas complejas con múltiples JOINs para obtener datos completos
 * - Actualizaciones coordinadas entre múltiples tablas
 * - Validación de existencia de usuarios antes de operaciones
 * - Manejo seguro de contraseñas con hashing SHA-256
 * - Normalización de datos (trim, lowercase)
 * - Control de acceso según rol de usuario
 *
 * Usado por: PerfilServlet, EditarPerfilServlet, VerPerfilServlet
 */
public class PerfilDAO {

    // =========================================================
    // CONSULTA - Obtener perfil completo de usuario
    // =========================================================
    /**
     * Carga el perfil completo de un usuario por su ID.
     *
     * Trae todos los campos necesarios para la pantalla de perfil:
     * datos básicos, teléfono, género, fechas de alta y actualización,
     * e información del emprendimiento asociado.
     *
     * @param idUsuario ID del usuario a consultar
     * @return objeto Usuario con todos los datos del perfil, o null si no existe
     * @throws SQLException si hay error en la consulta
     */
    public Usuario obtenerPerfil(int idUsuario) throws SQLException {
        // SQL con múltiples JOINs para obtener todos los datos del perfil en una sola consulta
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
                    p.fecha_actualizacion,
                    COALESCE(u.id_emprendimiento, 0) AS id_emprendimiento,
                    e.nombre AS nombre_emprendimiento
                FROM usuarios u
                JOIN correos c ON c.id = u.id_correo
                JOIN roles r ON r.id = u.id_rol
                JOIN perfil_usuario p ON p.id_usuario = u.id
                JOIN telefonos t ON t.id = p.id_telefono
                JOIN generos g ON g.id = p.id_genero
                LEFT JOIN emprendimientos e ON e.id = u.id_emprendimiento
                WHERE u.id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs); // Delegar el mapeo al helper privado
                }
            }
        }
        return null;
    }

    // =========================================================
    // LISTADO - Obtener todos los usuarios (SuperAdmin)
    // =========================================================
    /**
     * Lista todos los usuarios del sistema.
     *
     * Se usaría para una pantalla de administración general (SuperAdmin)
     * para tener una vista completa de todos los usuarios registrados.
     *
     * @return lista completa de usuarios ordenada por nombre completo
     * @throws SQLException si hay error al consultar la BD
     */
    public List<Usuario> listarTodosUsuarios() throws SQLException {
        // SQL para obtener datos básicos de todos los usuarios sin emprendimiento
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

    // =========================================================
    // LISTA DE GÉNEROS - Para selector del formulario
    // =========================================================
    /**
     * Retorna la lista de géneros disponibles para el selector del formulario.
     *
     * Devuelve una lista de pares [id, nombre] por cada género para que
     * el JSP pueda generar las opciones del select de forma dinámica.
     *
     * @return lista de [id, nombre] por cada género ordenada por nombre
     * @throws SQLException si hay error al consultar la BD
     */
    public List<String[]> listarGeneros() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM generos ORDER BY nombre";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // Crear par [id, nombre] para cada género
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
            }
        }
        return lista;
    }

    /**
     * Actualiza los datos personales del usuario: nombre, teléfono, género y correo.
     *
     * OPERACIONES:
     * 1. Obtener los IDs de la fila en correos, telefonos y perfil_usuario
     *    para el usuario dado.
     * 2. UPDATE correos   → nuevo correo (normalizado a minúsculas)
     * 3. UPDATE telefonos → nuevo teléfono
     * 4. UPDATE perfil_usuario → nuevo nombre, nuevo género, actualizar timestamp
     *
     */
    public boolean actualizarPerfil(int idUsuario, String nombreCompleto,
                                     String telefono, int idGenero, String correo) throws SQLException {

        // Obtener los IDs de las tablas relacionadas para poder hacer los UPDATEs
        String sqlObtener = """
                SELECT u.id_correo, p.id_telefono, p.id
                FROM usuarios u
                JOIN perfil_usuario p ON p.id_usuario = u.id
                WHERE u.id = ?
                """;

        int idCorreo = -1, idTelefono = -1, idPerfil = -1;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlObtener)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idCorreo   = rs.getInt("id_correo");
                    idTelefono = rs.getInt("id_telefono");
                    idPerfil   = rs.getInt("id");
                }
            }
        }

        // Si no se encontraron los IDs, el usuario no existe → no actualizar
        if (idCorreo == -1 || idTelefono == -1 || idPerfil == -1) {
            return false;
        }

        // Actualizar correo (normalizado a minúsculas sin espacios)
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE correos SET correo = ? WHERE id = ?")) {
            ps.setString(1, correo.toLowerCase().trim());
            ps.setInt(2, idCorreo);
            ps.executeUpdate();
        }

        // Actualizar teléfono
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE telefonos SET telefono = ? WHERE id = ?")) {
            ps.setString(1, telefono.trim());
            ps.setInt(2, idTelefono);
            ps.executeUpdate();
        }

        // Actualizar nombre, género y timestamp en perfil_usuario
        // CURRENT_TIMESTAMP registra automáticamente el momento de la modificación
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("""
                     UPDATE perfil_usuario
                     SET nombre_completo = ?,
                         id_genero = ?,
                         fecha_actualizacion = CURRENT_TIMESTAMP
                     WHERE id = ?
                     """)) {
            ps.setString(1, nombreCompleto.trim());
            ps.setInt(2, idGenero);
            ps.setInt(3, idPerfil);
            ps.executeUpdate();
        }

        return true;
    }

    // =========================================================
    // ACTUALIZACIÓN PARCIAL - Solo teléfono y correo (Empleados)
    // =========================================================
    /**
     * Actualiza solo el teléfono y el correo de un usuario.
     *
     * Usado por Empleados, que no pueden cambiar nombre ni género.
     * Realiza las mismas validaciones que el método completo pero
     * solo actualiza los campos permitidos para este rol.
     *
     * @param idUsuario ID del usuario a actualizar
     * @param telefono  nuevo número de teléfono
     * @param correo    nuevo correo electrónico
     * @return true si la actualización fue exitosa, false si el usuario no existe
     * @throws SQLException si hay error en las consultas
     */
    public boolean actualizarTelefonoYCorreo(int idUsuario, String telefono, String correo)
            throws SQLException {

        // Paso 1: Obtener los IDs de las tablas relacionadas para el usuario
        String sqlObtener = """
                SELECT u.id_correo, p.id_telefono
                FROM usuarios u
                JOIN perfil_usuario p ON p.id_usuario = u.id
                WHERE u.id = ?
                """;

        int idCorreo = -1, idTelefono = -1;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlObtener)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idCorreo   = rs.getInt("id_correo");
                    idTelefono = rs.getInt("id_telefono");
                }
            }
        }

        // Paso 2: Validar que se encontraron los IDs (usuario existe)
        if (idCorreo == -1 || idTelefono == -1) return false;

        // Paso 3: Actualizar correo (normalizado a minúsculas sin espacios)
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE correos SET correo = ? WHERE id = ?")) {
            ps.setString(1, correo.toLowerCase().trim());
            ps.setInt(2, idCorreo);
            ps.executeUpdate();
        }

        // Paso 4: Actualizar teléfono (normalizado sin espacios)
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE telefonos SET telefono = ? WHERE id = ?")) {
            ps.setString(1, telefono.trim());
            ps.setInt(2, idTelefono);
            ps.executeUpdate();
        }

        return true; // Actualización exitosa
    }

    // =========================================================
    // SEGURIDAD - Cambio de contraseña
    // =========================================================
    /**
     * Cambia la contraseña del usuario, verificando primero que la contraseña actual sea correcta.
     *
     * Proceso de cambio seguro:
     * 1. Hashear ambas contraseñas (actual y nueva) con SHA-256
     * 2. Verificar que la contraseña actual coincida con la almacenada
     * 3. Si la verificación pasa, actualizar con la nueva contraseña hasheada
     *
     * @param idUsuario         ID del usuario que cambia la contraseña
     * @param contrasennaActual contraseña actual en texto plano
     * @param contrasenaNueva   nueva contraseña en texto plano
     * @return true si el cambio fue exitoso, false si la contraseña actual es incorrecta
     * @throws SQLException si hay error en las consultas
     */
    public boolean cambiarContrasena(int idUsuario, String contrasennaActual,
                                      String contrasenaNueva) throws SQLException {

        // Paso 1: Hashear ambas contraseñas para comparación segura
        String hashActual = UsuarioDAO.hashSHA256(contrasennaActual);
        String hashNueva  = UsuarioDAO.hashSHA256(contrasenaNueva);

        if (hashActual == null || hashNueva == null) {
            return false; // No se pudo hashear (SHA-256 no disponible)
        }

        // Paso 2: Verificar que la contraseña actual sea correcta en la BD
        // Si no coincide, no se continúa con el cambio
        String sqlVerificar = "SELECT id FROM usuarios WHERE id = ? AND contrasena = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlVerificar)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, hashActual); // Hash de la contraseña actual ingresada
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // No hubo coincidencia → la contraseña actual es incorrecta
                    return false;
                }
            }
        }

        // Paso 3: La verificación pasó → actualizar con la nueva contraseña
        String sqlActualizar = "UPDATE usuarios SET contrasena = ? WHERE id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlActualizar)) {
            ps.setString(1, hashNueva); // Hash de la nueva contraseña
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }

        return true; // Contraseña cambiada exitosamente
    }

    // =========================================================
    // HELPER - Mapeo de ResultSet a objeto Usuario
    // =========================================================
    
    /**
     * Convierte una fila del ResultSet en un objeto Usuario.
     *
     * Mapea todos los campos del perfil incluyendo datos opcionales
     * de emprendimiento que pueden no estar presentes según la query.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto Usuario con todos los datos del perfil
     * @throws SQLException si hay error al acceder a los campos
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
        // Manejo seguro de campos opcionales según la query utilizada
        try {
            u.setIdEmprendimiento(rs.getInt("id_emprendimiento"));
            u.setNombreEmprendimiento(rs.getString("nombre_emprendimiento"));
        } catch (Exception ignored) {} // Columnas opcionales según la query
        return u;
    }
}
