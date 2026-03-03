package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;
import com.dulce_gestion.dao.UsuarioDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * DAO para crear un nuevo usuario (Empleado o Administrador).
 * Inserta en 3 tablas en orden: correos → telefonos → usuarios → perfil_usuario.
 * Usa una sola conexión con transacción para garantizar integridad.
 */
public class CrearEmpleadoDAO {

    /*
     * Verifica si un correo ya existe en la BD.
     * Retorna true si ya está registrado.
     */
    public boolean correoExiste(String correo) throws SQLException {
        String sql = "SELECT id FROM correos WHERE correo = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /*
     * Verifica si un teléfono ya existe en la BD.
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

    /*
     * Crea el usuario completo en una transacción.
     * Inserta: correos → telefonos → usuarios → perfil_usuario.
     * La contraseña se hashea con SHA-256 antes de guardar.
     */
    public void crear(String nombreCompleto, String telefono, int idGenero,
                      String correo, String contrasena, String estado,
                      int idRol) throws SQLException {

        String hashContrasena = UsuarioDAO.hashSHA256(contrasena);

        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false); // inicio de transacción

            try {
                // 1. Insertar correo
                int idCorreo;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO correos (correo) VALUES (?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, correo.trim().toLowerCase());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idCorreo = rs.getInt(1);
                    }
                }

                // 2. Insertar teléfono
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

                // 3. Insertar usuario
                int idUsuario;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO usuarios (id_correo, estado, contrasena, id_rol) VALUES (?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idCorreo);
                    ps.setString(2, estado);
                    ps.setString(3, hashContrasena);
                    ps.setInt(4, idRol);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idUsuario = rs.getInt(1);
                    }
                }

                // 4. Insertar perfil_usuario
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO perfil_usuario (nombre_completo, id_usuario, id_telefono, id_genero) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, nombreCompleto.trim());
                    ps.setInt(2, idUsuario);
                    ps.setInt(3, idTelefono);
                    ps.setInt(4, idGenero);
                    ps.executeUpdate();
                }

                // 5. Asignar permisos del rol automáticamente
                try (PreparedStatement ps = con.prepareStatement(
                        """
                        INSERT INTO rol_permiso (id_rol, id_permiso)
                        SELECT ?, id FROM permisos
                        WHERE id NOT IN (SELECT id_permiso FROM rol_permiso WHERE id_rol = ?)
                        """)) {
                    ps.setInt(1, idRol);
                    ps.setInt(2, idRol);
                    ps.executeUpdate();
                }

                con.commit(); // todo bien → confirmar

            } catch (SQLException e) {
                con.rollback(); // algo falló → deshacer todo
                throw e;
            }
        }
    }

    /*
     * Obtiene el id de un rol por nombre.
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

    /*
     * Obtiene el id de un género por nombre.
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
