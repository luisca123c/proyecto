package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de tablas de configuración del sistema.
 * Solo accesible por SuperAdministrador.
 * Tablas: categorias, unidad_medida, metodo_pago
 */
public class ConfiguracionDAO {

    // ── Modelo genérico (id, nombre, descripcion opcional) ──────────────
    public static class Fila {
        public int     id;
        public String  nombre;
        public String  descripcion; // null para unidad_medida y metodo_pago
        public boolean activo = true;
    }

    // ════════════════════════════════════════════════════════════
    // CATEGORÍAS
    // ════════════════════════════════════════════════════════════

    public List<Fila> listarCategorias() throws SQLException {
        return listar("SELECT id, nombre, descripcion, activo FROM categorias ORDER BY nombre");
    }

    public void crearCategoria(String nombre, String descripcion) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO categorias (nombre, descripcion) VALUES (?, ?)")) {
            ps.setString(1, nombre.trim());
            ps.setString(2, descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null);
            ps.executeUpdate();
        }
    }

    public void editarCategoria(int id, String nombre, String descripcion, boolean activo) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE categorias SET nombre=?, descripcion=?, activo=? WHERE id=?")) {
            ps.setString(1, nombre.trim());
            ps.setString(2, descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null);
            ps.setInt(3, activo ? 1 : 0);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public void eliminarCategoria(int id) throws SQLException {
        ejecutar("UPDATE categorias SET activo=0 WHERE id=?", id);
    }

    // ════════════════════════════════════════════════════════════
    // UNIDADES DE MEDIDA
    // ════════════════════════════════════════════════════════════

    public List<Fila> listarUnidades() throws SQLException {
        return listar("SELECT id, nombre, NULL AS descripcion, activo FROM unidad_medida ORDER BY nombre");
    }

    public void crearUnidad(String nombre) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO unidad_medida (nombre) VALUES (?)")) {
            ps.setString(1, nombre.trim());
            ps.executeUpdate();
        }
    }

    public void editarUnidad(int id, String nombre, boolean activo) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE unidad_medida SET nombre=?, activo=? WHERE id=?")) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, activo ? 1 : 0);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void eliminarUnidad(int id) throws SQLException {
        ejecutar("UPDATE unidad_medida SET activo=0 WHERE id=?", id);
    }

    // ════════════════════════════════════════════════════════════
    // MÉTODOS DE PAGO
    // ════════════════════════════════════════════════════════════

    public List<Fila> listarMetodos() throws SQLException {
        return listar("SELECT id, nombre, NULL AS descripcion, activo FROM metodo_pago ORDER BY nombre");
    }

    public void crearMetodo(String nombre) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO metodo_pago (nombre) VALUES (?)")) {
            ps.setString(1, nombre.trim());
            ps.executeUpdate();
        }
    }

    public void editarMetodo(int id, String nombre, boolean activo) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE metodo_pago SET nombre=?, activo=? WHERE id=?")) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, activo ? 1 : 0);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void eliminarMetodo(int id) throws SQLException {
        ejecutar("UPDATE metodo_pago SET activo=0 WHERE id=?", id);
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private List<Fila> listar(String sql) throws SQLException {
        List<Fila> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Fila f = new Fila();
                f.id          = rs.getInt("id");
                f.nombre      = rs.getString("nombre");
                f.descripcion = rs.getString("descripcion");
                f.activo      = rs.getInt("activo") == 1;
                lista.add(f);
            }
        }
        return lista;
    }

    private void ejecutar(String sql, int id) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
