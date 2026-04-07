package com.dulce_gestion.dao;

import com.dulce_gestion.controllers.NuevoEmpleadoServlet;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO especializado en la creación completa de empleados en el sistema.
 *
 * Esta clase maneja el proceso complejo de crear un usuario completo,
 * involucrando múltiples tablas relacionadas y manteniendo la integridad
 * referencial a través de transacciones ACID.
 *
 * @see NuevoEmpleadoServlet Servlet que consume este DAO
 */
public class CrearEmpleadoDAO {

    /**
     * Verifica si un correo ya está registrado en la base de datos.
     *
     * Esta validación es crucial porque el correo funciona como
     * identificador único para el inicio de sesión. Se consulta la tabla
     * {@code correos} directamente (no {@code usuarios}) porque el correo
     * es una entidad independiente con su propia tabla.
     *
     * El correo se convierte a minúsculas antes de buscar para evitar
     * duplicados por diferencias de capitalización (ej: "Juan@email.com"
     * vs "juan@email.com").
     *
     * @param correo correo electrónico a verificar (se normaliza internamente)
     * @return true si el correo ya existe, false si está disponible
     * @throws SQLException si hay error al consultar la base de datos
     */
    public boolean correoExiste(String correo) throws SQLException {
        String sql = "SELECT id FROM correos WHERE correo = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase()); // Normalizar: eliminar espacios + minúsculas
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true si hay al menos una coincidencia
            }
        }
    }

    /**
     * Verifica si un teléfono ya está registrado en la base de datos.
     *
     * Similar al correo, el teléfono es una entidad independiente
     * en su propia tabla {@code telefonos} para permitir múltiples
     * contactos por usuario en el futuro.
     *
     * @param teléfono número telefónico a verificar (se normaliza con trim)
     * @return true si el teléfono ya existe, false si está disponible
     * @throws SQLException si hay error al consultar la base de datos
     */
    public boolean telefonoExiste(String telefono) throws SQLException {
        String sql = "SELECT id FROM telefonos WHERE telefono = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, telefono.trim()); // Eliminar espacios en blanco
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true si hay al menos una coincidencia
            }
        }
    }

    /**
     * Crea el usuario completo en 5 tablas relacionadas dentro de una transacción.
     *
     * ORDEN DE INSERCIÓN (respetando las FK):
     * 1. correos         → retorna idCorreo
     * 2. telefonos       → retorna idTelefono
     * 3. usuarios        → usa idCorreo, retorna idUsuario
     * 4. perfil_usuario  → usa idUsuario + idTelefono
     * 5. rol_permiso     → asigna permisos por defecto al rol
     *
     * @param nombreCompleto    nombre completo del empleado
     * @param telefono         número telefónico del empleado
     * @param idGenero        ID del género (FK a tabla {@code generos})
     * @param correo          correo electrónico (se normaliza a minúsculas)
     * @param contrasena       contraseña en texto plano (se hashea internamente)
     * @param estado           estado del usuario (ej: "Activo", "Inactivo")
     * @param idRol           ID del rol (FK a tabla {@code roles})
     * @param idEmprendimiento ID del emprendimiento (FK a tabla {@code emprendimientos})
     * @throws SQLException si hay error en cualquier paso de la transacción
     */
    public void crear(String nombreCompleto, String telefono, int idGenero,
                      String correo, String contrasena, String estado,
                      int idRol, int idEmprendimiento) throws SQLException {

        // Hashear la contraseña antes de guardarla. Nunca se guarda en texto plano.
        String hashContrasena = UsuarioDAO.hashSHA256(contrasena);

        try (Connection con = DB.obtenerConexion()) {
            // autocommit es una propiedad de la conexión que determina si las operaciones se ejecutan de forma inmediata 
            // o en una transacción. Por defecto es true, lo que significa que cada operación se ejecuta de forma inmediata.
            // Al establecerlo en false, se inicia una transacción que permite que todas las operaciones se ejecuten 
            // de forma atómica, es decir, todas se ejecutan o ninguna se ejecuta.
            con.setAutoCommit(false); // Iniciar transacción: todo o nada se ejecuta

            try {
                // ── Paso 1: insertar correo (entidad independiente) ───────────────────────
                // INSERT INTO correos: tabla independiente para normalizar correos
                // Un correo puede ser referenciado por múltiples usuarios (duplicados permitidos)
                int idCorreo;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO correos (correo) VALUES (?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, correo.trim().toLowerCase()); // Normalizar a minúsculas
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idCorreo = rs.getInt(1); // ID generado por AUTO_INCREMENT
                    }
                }

                // ── Paso 2: insertar teléfono (entidad independiente) ─────────────────────
                // INSERT INTO telefonos: tabla independiente para normalizar teléfonos
                // Un teléfono puede ser referenciado por múltiples usuarios (duplicados permitidos)
                int idTelefono;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO telefonos (telefono) VALUES (?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, telefono.trim()); // Mantener formato original del teléfono
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idTelefono = rs.getInt(1);
                    }
                }

                // ── Paso 3: insertar usuario (usa idCorreo del paso 1) ───────────────────
                // INSERT INTO usuarios: tabla principal del sistema de autenticación
                // Contiene credenciales y relaciones básicas del usuario
                int idUsuario;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO usuarios (id_correo, estado, contrasena, id_rol, id_emprendimiento) VALUES (?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idCorreo); // FK: referencia al correo creado en paso 1
                    ps.setString(2, estado); // Estado inicial (ej: "Activo", "Inactivo")
                    ps.setString(3, hashContrasena); // Hash SHA-256, NUNCA guardar texto plano
                    ps.setInt(4, idRol); // FK: rol que determina permisos del usuario
                    ps.setInt(5, idEmprendimiento); // FK: emprendimiento al que pertenece
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idUsuario = rs.getInt(1); // ID del nuevo usuario para pasos siguientes
                    }
                }

                // ── Paso 4: insertar perfil (usa idUsuario + idTelefono) ───────────────────
                // INSERT INTO perfil_usuario: tabla con datos personales del usuario
                // Extiende la información básica de usuarios con datos demográficos
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO perfil_usuario (nombre_completo, id_usuario, id_telefono, id_genero) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, nombreCompleto.trim()); // Nombre para mostrar en la interfaz
                    ps.setInt(2, idUsuario); // FK: referencia al usuario creado en paso 3
                    ps.setInt(3, idTelefono); // FK: referencia al teléfono creado en paso 2
                    ps.setInt(4, idGenero); // FK: género para datos demográficos
                    ps.executeUpdate();
                }

                // ── Paso 5: asignar permisos del rol automáticamente ───────────────────────
                // INSERT INTO rol_permiso: asignación automática de permisos del rol
                // Esta inserción masiva copia todos los permisos definidos para el rol
                // y los asocia al usuario recién creado
                try (PreparedStatement ps = con.prepareStatement("""
                        INSERT INTO rol_permiso (id_rol, id_permiso)
                        SELECT ?, id FROM permisos
                        WHERE id NOT IN (SELECT id_permiso FROM rol_permiso WHERE id_rol = ?)
                        """)) {
                    ps.setInt(1, idRol); // Rol cuyos permisos se asignarán al usuario
                    ps.setInt(2, idRol); // Mismo rol para evitar duplicados con NOT IN
                    ps.executeUpdate();
                }

                con.commit(); // Todos los pasos exitosos → confirmar la transacción completa

            } catch (SQLException e) {
                con.rollback(); // Algo falló → deshacer TODO (ninguna tabla queda modificada)
                throw e;        // Re-lanzar para que el Servlet muestre el error apropiado
            }
        }
    }

    /**
     * Obtiene el ID de un rol por su nombre descriptivo.
     *
     * Este método es necesario porque el formulario HTML envía el nombre
     * del rol como texto ("Empleado", "Administrador", etc.) pero
     * la tabla {@code usuarios} requiere el ID numérico como clave foránea.
     *
     * @param nombreRol nombre del rol (ej: "Empleado", "Administrador")
     * @return          ID del rol en la tabla {@code roles}
     * @throws SQLException si el rol no existe en la base de datos
     */
    public int obtenerIdRol(String nombreRol) throws SQLException {
        String sql = "SELECT id FROM roles WHERE nombre = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreRol); // Búsqueda exacta del nombre del rol
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("Rol no encontrado: " + nombreRol);
    }

    /**
     * Obtiene el ID de un género por su nombre descriptivo.
     *
     * Funciona de manera similar a {@code obtenerIdRol()}, convirtiendo
     * el texto del formulario ("Masculino", "Femenino", "Otro")
     * al ID numérico requerido por la clave foránea en
     * {@code perfil_usuario}.
     *
     * @param nombreGenero nombre del género (ej: "Masculino", "Femenino")
     * @return              ID del género en la tabla {@code generos}
     * @throws SQLException si el género no existe en la base de datos
     */
    public int obtenerIdGenero(String nombreGenero) throws SQLException {
        String sql = "SELECT id FROM generos WHERE nombre = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreGenero); // Búsqueda exacta del nombre del género
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("Género no encontrado: " + nombreGenero);
    }
}
