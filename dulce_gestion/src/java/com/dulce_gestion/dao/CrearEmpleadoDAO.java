package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: CrearEmpleadoDAO
 * Tablas escritas:    correos, telefonos, usuarios, perfil_usuario
 * Tablas leídas:      roles, generos, permisos, rol_permiso
 * Usado por:          NuevoEmpleadoServlet
 * ============================================================
 *
 */
public class CrearEmpleadoDAO {

    /**
     * Verifica si un correo ya está registrado en la BD.
     *
     * Se consulta la tabla correos directamente (no usuarios) porque
     * el correo es una entidad independiente con su propia tabla.
     * El correo se normaliza a minúsculas para evitar duplicados por capitalización.
     *
     * @param correo  correo a verificar (se normaliza internamente)
     * @return        true si el correo ya existe, false si está disponible
     * @throws SQLException si hay error al consultar la BD
     */
    public boolean correoExiste(String correo) throws SQLException {
        String sql = "SELECT id FROM correos WHERE correo = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase()); // Normalizar antes de buscar
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true si hay al menos una fila
            }
        }
    }

    /**
     * Verifica si un teléfono ya está registrado en la BD.
     *
     * @param telefono  teléfono a verificar
     * @return          true si ya existe, false si está disponible
     * @throws SQLException si hay error al consultar la BD
     */
    public boolean telefonoExiste(String telefono) throws SQLException {
        String sql = "SELECT id FROM telefonos WHERE telefono = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, telefono.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Crea el usuario completo en 4 tablas dentro de una transacción.
     *
     * ORDEN DE INSERCIÓN (respetando las FK):
     * 1. correos         → retorna idCorreo
     * 2. telefonos       → retorna idTelefono
     * 3. usuarios        → usa idCorreo, retorna idUsuario
     * 4. perfil_usuario  → usa idUsuario + idTelefono
     * 5. rol_permiso     → asigna permisos por defecto al rol
     *
     */
    public void crear(String nombreCompleto, String telefono, int idGenero,
                      String correo, String contrasena, String estado,
                      int idRol, int idEmprendimiento) throws SQLException {

        // Hashear la contraseña antes de guardarla. Nunca se guarda en texto plano.
        String hashContrasena = UsuarioDAO.hashSHA256(contrasena);

        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false); // Iniciar transacción: todo o nada

            try {
                // ── Paso 1: insertar correo ───────────────────────────────
                int idCorreo;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO correos (correo) VALUES (?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, correo.trim().toLowerCase()); // Siempre minúsculas
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idCorreo = rs.getInt(1); // ID generado por AUTO_INCREMENT
                    }
                }

                // ── Paso 2: insertar teléfono ─────────────────────────────
                int idTelefono;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO telefonos (telefono) VALUES (?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, telefono.trim());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idTelefono = rs.getInt(1);
                    }
                }

                // ── Paso 3: insertar usuario (usa idCorreo del paso 1) ────
                int idUsuario;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO usuarios (id_correo, estado, contrasena, id_rol, id_emprendimiento) VALUES (?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idCorreo);
                    ps.setString(2, estado);
                    ps.setString(3, hashContrasena); // Hash SHA-256, nunca texto plano
                    ps.setInt(4, idRol);
                    ps.setInt(5, idEmprendimiento);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idUsuario = rs.getInt(1);
                    }
                }

                // ── Paso 4: insertar perfil (usa idUsuario + idTelefono) ──
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO perfil_usuario (nombre_completo, id_usuario, id_telefono, id_genero) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, nombreCompleto.trim());
                    ps.setInt(2, idUsuario);
                    ps.setInt(3, idTelefono);
                    ps.setInt(4, idGenero);
                    ps.executeUpdate();
                }

                // ── Paso 5: asignar permisos del rol automáticamente ──────
                // Inserta los permisos del rol que aún no estén asignados.
                // WHERE NOT IN evita duplicados si el rol ya tenía algunos permisos.
                try (PreparedStatement ps = con.prepareStatement("""
                        INSERT INTO rol_permiso (id_rol, id_permiso)
                        SELECT ?, id FROM permisos
                        WHERE id NOT IN (SELECT id_permiso FROM rol_permiso WHERE id_rol = ?)
                        """)) {
                    ps.setInt(1, idRol);
                    ps.setInt(2, idRol);
                    ps.executeUpdate();
                }

                con.commit(); // Todo salió bien → confirmar los 5 INSERTs

            } catch (SQLException e) {
                con.rollback(); // Algo falló → deshacer todo (ninguna tabla queda modificada)
                throw e;        // Re-lanzar para que el Servlet muestre el error
            }
        }
    }

    /**
     * Obtiene el ID de un rol por su nombre.
     *
     * NuevoEmpleadoServlet recibe el nombre del rol como texto
     * desde el formulario ("Empleado", "Administrador") y necesita
     * convertirlo al ID numérico para la FK en la tabla usuarios.
     *
     * @param nombreRol  nombre del rol (ej: "Empleado")
     * @return           ID del rol en la tabla roles
     * @throws SQLException si el rol no existe en la BD
     */
    public int obtenerIdRol(String nombreRol) throws SQLException {
        String sql = "SELECT id FROM roles WHERE nombre = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreRol);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("Rol no encontrado: " + nombreRol);
    }

    /**
     * Obtiene el ID de un género por su nombre.
     *
     * Igual que obtenerIdRol(), convierte el texto del formulario
     * ("Masculino", "Femenino") al ID numérico para la FK.
     *
     * @param nombreGenero  nombre del género (ej: "Masculino")
     * @return              ID del género en la tabla generos
     * @throws SQLException si el género no existe en la BD
     */
    public int obtenerIdGenero(String nombreGenero) throws SQLException {
        String sql = "SELECT id FROM generos WHERE nombre = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreGenero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("Género no encontrado: " + nombreGenero);
    }
}
