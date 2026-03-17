package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: EditarEmpleadoDAO
 * Tablas escritas:    correos, telefonos, perfil_usuario, usuarios
 * Tablas leídas:      roles, generos (en el UPDATE via JOIN)
 * Usado por:          EditarEmpleadoServlet, EliminarEmpleadoServlet
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Busca un usuario por ID para prellenar el formulario de edición,
 * verifica unicidad de correo y teléfono excluyendo al propio usuario,
 * y aplica los cambios en 4 tablas dentro de una transacción.
 *
 * ¿POR QUÉ LA VERIFICACIÓN DE UNICIDAD EXCLUYE AL USUARIO ACTUAL?
 * -----------------------------------------------------------------
 * Si el admin edita a un empleado sin cambiar su correo, la verificación
 * "¿existe este correo?" devolvería true (el propio correo del empleado
 * ya está en la BD). Eso daría un falso error "correo ya en uso".
 *
 * correoExisteEnOtro() y telefonoExisteEnOtro() buscan duplicados
 * solo en usuarios con ID distinto al que se está editando, evitando
 * ese falso positivo.
 *
 * ¿POR QUÉ LA CONTRASEÑA ES OPCIONAL EN LA EDICIÓN?
 * ---------------------------------------------------
 * Un admin puede querer cambiar solo el nombre o el teléfono de un
 * empleado sin tocar su contraseña. Si el campo llega vacío, el UPDATE
 * omite la columna contrasena con una query sin ese campo.
 * Si llega con contenido, se hashea y se incluye en el UPDATE.
 */
public class EditarEmpleadoDAO {

    /**
     * Busca un usuario por ID y retorna sus datos completos.
     * Se usa para prellenar el formulario de edición en el GET.
     *
     * El JOIN de 6 tablas trae todo lo necesario para el formulario:
     * nombre, correo, teléfono, género, rol, estado.
     *
     * @param id  ID del usuario a buscar
     * @return    objeto Usuario con todos sus datos, o null si no existe
     * @throws SQLException si hay error al consultar la BD
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
                    try { u.setNombreEmprendimiento(rs.getString("nombre_emprendimiento")); } catch (Exception ignored) {}
                    return u;
                }
            }
        }
        return null; // El usuario no existe
    }

    /**
     * Verifica si un correo ya existe en la BD en un usuario distinto al indicado.
     *
     * El JOIN con usuarios permite excluir el propio registro del usuario
     * que se está editando (WHERE u.id != idUsuarioActual).
     *
     * @param correo          correo a verificar (se normaliza a minúsculas)
     * @param idUsuarioActual ID del usuario que se está editando (se excluye)
     * @return                true si el correo ya lo usa otro usuario
     * @throws SQLException   si hay error al consultar la BD
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

    /**
     * Verifica si un teléfono ya existe en la BD en un usuario distinto al indicado.
     *
     * @param telefono        teléfono a verificar
     * @param idUsuarioActual ID del usuario que se está editando (se excluye)
     * @return                true si el teléfono ya lo usa otro usuario
     * @throws SQLException   si hay error al consultar la BD
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

    /**
     * Actualiza los datos del usuario en 4 tablas dentro de una transacción.
     *
     * ORDEN DE ACTUALIZACIÓN:
     * 1. correos         → UPDATE usando JOIN con usuarios para localizar la fila
     * 2. telefonos       → UPDATE usando JOIN con perfil_usuario
     * 3. perfil_usuario  → UPDATE nombre_completo y id_genero
     * 4. usuarios        → UPDATE estado, y opcionalmente contrasena y id_rol
     *
     * ¿POR QUÉ UPDATE ... JOIN EN LOS PASOS 1 Y 2?
     * ----------------------------------------------
     * No se tiene el id de la fila en correos o telefonos, solo el idUsuario.
     * El JOIN dentro del UPDATE permite localizar la fila correcta en una
     * sola operación, sin necesitar una SELECT previa para obtener el id.
     *
     * ¿POR QUÉ 4 QUERIES DISTINTAS PARA EL PASO 4?
     * -----------------------------------------------
     * Hay 4 combinaciones posibles:
     *   - Con/sin nueva contraseña
     *   - Solicitante es/no es SuperAdmin (puede/no puede cambiar el rol)
     * Cada combinación necesita un SQL diferente. Se construye la query
     * correcta con condicionales en Java para no ejecutar UPDATEs innecesarios.
     *
     * @param idUsuario        ID del usuario a actualizar
     * @param nombreCompleto   nuevo nombre completo
     * @param telefono         nuevo teléfono
     * @param genero           nombre del género (se convierte a ID via JOIN)
     * @param correo           nuevo correo (se normaliza a minúsculas)
     * @param nuevaContrasena  nueva contraseña en texto plano, o vacío para no cambiar
     * @param estado           "Activo" o "Inactivo"
     * @param rol              nombre del rol (solo se aplica si esSuperAdmin = true)
     * @param esSuperAdmin     true si el solicitante puede cambiar el rol
     * @throws SQLException    si hay error de BD o violación de constraint
     */
    public void actualizar(int idUsuario, String nombreCompleto, String telefono,
                           String genero, String correo, String nuevaContrasena,
                           String estado, String rol, boolean esSuperAdmin,
                           int idEmprendimiento) throws SQLException {

        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false); // Transacción: los 4 UPDATEs son atómicos
            try {

                // ── Paso 1: actualizar correo ─────────────────────────────
                // UPDATE con JOIN: localiza la fila en correos a través de usuarios
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE correos c JOIN usuarios u ON u.id_correo = c.id " +
                        "SET c.correo = ? WHERE u.id = ?")) {
                    ps.setString(1, correo.trim().toLowerCase());
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }

                // ── Paso 2: actualizar teléfono ───────────────────────────
                // UPDATE con JOIN: localiza la fila en telefonos a través de perfil_usuario
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE telefonos t JOIN perfil_usuario p ON p.id_telefono = t.id " +
                        "SET t.telefono = ? WHERE p.id_usuario = ?")) {
                    ps.setString(1, telefono.trim());
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }

                // ── Paso 3: actualizar perfil (nombre + género) ───────────
                // JOIN con generos para convertir nombre de género a su ID en un solo UPDATE
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

                // ── Paso 4: actualizar usuarios (estado + rol + contraseña opcional) ─
                // Se construyen 4 queries según 2 variables binarias:
                //   ¿Hay nueva contraseña? × ¿Es SuperAdmin?
                if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
                    // Hay nueva contraseña → hashear antes de guardar
                    String hash = UsuarioDAO.hashSHA256(nuevaContrasena);
                    String sqlU = esSuperAdmin
                        ? "UPDATE usuarios SET estado = ?, contrasena = ?, id_rol = (SELECT id FROM roles WHERE nombre = ?), id_emprendimiento = ? WHERE id = ?"
                        : "UPDATE usuarios SET estado = ?, contrasena = ? WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlU)) {
                        ps.setString(1, estado);
                        ps.setString(2, hash);
                        if (esSuperAdmin) { ps.setString(3, rol); ps.setInt(4, idEmprendimiento); ps.setInt(5, idUsuario); }
                        else              { ps.setInt(3, idUsuario); }
                        ps.executeUpdate();
                    }
                } else {
                    // Sin nueva contraseña → omitir la columna contrasena del UPDATE
                    String sqlU = esSuperAdmin
                        ? "UPDATE usuarios SET estado = ?, id_rol = (SELECT id FROM roles WHERE nombre = ?), id_emprendimiento = ? WHERE id = ?"
                        : "UPDATE usuarios SET estado = ? WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sqlU)) {
                        ps.setString(1, estado);
                        if (esSuperAdmin) { ps.setString(2, rol); ps.setInt(3, idEmprendimiento); ps.setInt(4, idUsuario); }
                        else              { ps.setInt(2, idUsuario); }
                        ps.executeUpdate();
                    }
                }

                con.commit(); // Todos los UPDATEs exitosos → confirmar

            } catch (SQLException e) {
                con.rollback(); // Algo falló → deshacer todos los cambios
                throw e;
            }
        }
    }
}
