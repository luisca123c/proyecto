package com.dulce_gestion.dao;

import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.utils.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de emprendimientos.
 * Solo el SuperAdministrador puede crear, editar e inactivar.
 * Admins y Empleados solo pueden ver información básica.
 */
public class EmprendimientoDAO {

    private static final String SELECT_BASE =
        "SELECT id, nombre, nit, direccion, ciudad, telefono, correo, estado, " +
        "DATE_FORMAT(fecha_creacion, '%d/%m/%Y') AS fecha_creacion " +
        "FROM emprendimientos ";

    // ── Listar todos ───────────────────────────────────────────────────

    public List<Emprendimiento> listarTodos() throws SQLException {
        return ejecutarLista(SELECT_BASE + "ORDER BY nombre");
    }

    public List<Emprendimiento> listarActivos() throws SQLException {
        return ejecutarLista(SELECT_BASE + "WHERE estado = 'Activo' ORDER BY nombre");
    }

    // ── Buscar por ID ──────────────────────────────────────────────────

    public Emprendimiento buscarPorId(int id) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    // ── Crear ──────────────────────────────────────────────────────────


    /**
     * Verifica si un NIT ya existe en otro emprendimiento.
     *
     * @param nit  NIT a verificar.
     * @param excludeId  ID a excluir (para edición); 0 en creación.
     * @return true si el NIT ya lo usa otro emprendimiento.
     * @throws SQLException si falla la consulta.
     */
    public boolean nitExisteEnOtro(String nit, int excludeId) throws SQLException {
        String sql = excludeId > 0
            ? "SELECT id FROM emprendimientos WHERE nit = ? AND id != ?"
            : "SELECT id FROM emprendimientos WHERE nit = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nit.trim());
            if (excludeId > 0) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Verifica si un teléfono ya existe en otro emprendimiento.
     *
     * @param telefono  teléfono a verificar.
     * @param excludeId  ID a excluir (para edición); 0 en creación.
     * @return true si el teléfono ya lo usa otro emprendimiento.
     * @throws SQLException si falla la consulta.
     */
    public boolean telefonoExisteEnOtro(String telefono, int excludeId) throws SQLException {
        String sql = excludeId > 0
            ? "SELECT id FROM emprendimientos WHERE telefono = ? AND id != ?"
            : "SELECT id FROM emprendimientos WHERE telefono = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, telefono.trim());
            if (excludeId > 0) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Verifica si un correo ya existe en otro emprendimiento.
     *
     * @param correo  correo a verificar.
     * @param excludeId  ID a excluir (para edición); 0 en creación.
     * @return true si el correo ya lo usa otro emprendimiento.
     * @throws SQLException si falla la consulta.
     */
    public boolean correoExisteEnOtro(String correo, int excludeId) throws SQLException {
        String sql = excludeId > 0
            ? "SELECT id FROM emprendimientos WHERE correo = ? AND id != ?"
            : "SELECT id FROM emprendimientos WHERE correo = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase());
            if (excludeId > 0) ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public void crear(String nombre, String nit, String direccion,
                      String ciudad, String telefono, String correo) throws SQLException {
        String sql = "INSERT INTO emprendimientos (nombre, nit, direccion, ciudad, telefono, correo) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setString(2, nit    != null && !nit.isBlank()       ? nit.trim()       : null);
            ps.setString(3, direccion != null && !direccion.isBlank() ? direccion.trim() : null);
            ps.setString(4, ciudad != null && !ciudad.isBlank()    ? ciudad.trim()    : null);
            ps.setString(5, telefono != null && !telefono.isBlank()? telefono.trim()  : null);
            ps.setString(6, correo != null && !correo.isBlank()    ? correo.trim()    : null);
            ps.executeUpdate();
        }
    }

    // ── Editar ─────────────────────────────────────────────────────────

    public void editar(int id, String nombre, String nit, String direccion,
                       String ciudad, String telefono, String correo) throws SQLException {
        String sql = "UPDATE emprendimientos SET nombre=?, nit=?, direccion=?, " +
                     "ciudad=?, telefono=?, correo=? WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setString(2, nit       != null && !nit.isBlank()       ? nit.trim()       : null);
            ps.setString(3, direccion != null && !direccion.isBlank() ? direccion.trim() : null);
            ps.setString(4, ciudad    != null && !ciudad.isBlank()    ? ciudad.trim()    : null);
            ps.setString(5, telefono  != null && !telefono.isBlank()  ? telefono.trim()  : null);
            ps.setString(6, correo    != null && !correo.isBlank()    ? correo.trim()    : null);
            ps.setInt(7, id);
            ps.executeUpdate();
        }
    }

    // ── Inactivar ──────────────────────────────────────────────────────

    public void inactivar(int id) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE emprendimientos SET estado = 'Inactivo' WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void activar(int id) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE emprendimientos SET estado = 'Activo' WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private List<Emprendimiento> ejecutarLista(String sql) throws SQLException {
        List<Emprendimiento> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Emprendimiento mapear(ResultSet rs) throws SQLException {
        Emprendimiento e = new Emprendimiento();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        e.setNit(rs.getString("nit"));
        e.setDireccion(rs.getString("direccion"));
        e.setCiudad(rs.getString("ciudad"));
        e.setTelefono(rs.getString("telefono"));
        e.setCorreo(rs.getString("correo"));
        e.setEstado(rs.getString("estado"));
        e.setFechaCreacion(rs.getString("fecha_creacion"));
        return e;
    }
}
