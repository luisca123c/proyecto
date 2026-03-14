package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el módulo de Compras de Insumos.
 * Tabla: compras_insumos (id, id_usuario, descripcion, total, id_metodo_pago, fecha_compra)
 * Patrón idéntico al de GastosDAO pero sobre una sola tabla simple.
 */
public class ComprasDAO {

    // ── Modelo interno ─────────────────────────────────────────────────────
    public static class FilaCompra {
        public int        id;
        public String     fecha;
        public String     fechaRaw;      // yyyy-MM-dd para el input date del modal editar
        public String     descripcion;
        public String     metodoPago;
        public int        idMetodoPago;
        public String     registradoPor;
        public BigDecimal total;
    }

    // ── Listar todas las compras ───────────────────────────────────────────
    public List<FilaCompra> listar() throws SQLException {
        String sql =
            "SELECT ci.id, DATE_FORMAT(ci.fecha_compra,'%d/%m/%Y %H:%i') AS fecha, " +
            "DATE_FORMAT(ci.fecha_compra,'%Y-%m-%d') AS fecha_raw, " +
            "ci.descripcion, mp.nombre AS metodo_pago, ci.id_metodo_pago, " +
            "p.nombre_completo AS registrado_por, ci.total " +
            "FROM compras_insumos ci " +
            "JOIN metodo_pago mp ON mp.id = ci.id_metodo_pago " +
            "JOIN perfil_usuario p ON p.id_usuario = ci.id_usuario " +
            "ORDER BY ci.fecha_compra DESC";

        List<FilaCompra> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FilaCompra fc = new FilaCompra();
                fc.id            = rs.getInt("id");
                fc.fecha         = rs.getString("fecha");
                fc.fechaRaw      = rs.getString("fecha_raw");
                fc.descripcion   = rs.getString("descripcion");
                fc.metodoPago    = rs.getString("metodo_pago");
                fc.idMetodoPago  = rs.getInt("id_metodo_pago");
                fc.registradoPor = rs.getString("registrado_por");
                fc.total         = rs.getBigDecimal("total");
                lista.add(fc);
            }
        }
        return lista;
    }

    // ── Buscar una compra por ID (para el modal editar) ────────────────────
    public FilaCompra obtenerPorId(int id) throws SQLException {
        String sql =
            "SELECT ci.id, DATE_FORMAT(ci.fecha_compra,'%d/%m/%Y %H:%i') AS fecha, " +
            "DATE_FORMAT(ci.fecha_compra,'%Y-%m-%d') AS fecha_raw, " +
            "ci.descripcion, mp.nombre AS metodo_pago, ci.id_metodo_pago, " +
            "p.nombre_completo AS registrado_por, ci.total " +
            "FROM compras_insumos ci " +
            "JOIN metodo_pago mp ON mp.id = ci.id_metodo_pago " +
            "JOIN perfil_usuario p ON p.id_usuario = ci.id_usuario " +
            "WHERE ci.id = ?";

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FilaCompra fc = new FilaCompra();
                    fc.id            = rs.getInt("id");
                    fc.fecha         = rs.getString("fecha");
                    fc.fechaRaw      = rs.getString("fecha_raw");
                    fc.descripcion   = rs.getString("descripcion");
                    fc.metodoPago    = rs.getString("metodo_pago");
                    fc.idMetodoPago  = rs.getInt("id_metodo_pago");
                    fc.registradoPor = rs.getString("registrado_por");
                    fc.total         = rs.getBigDecimal("total");
                    return fc;
                }
            }
        }
        return null;
    }

    // ── Registrar nueva compra ─────────────────────────────────────────────
    public void registrar(int idUsuario, String descripcion,
                          BigDecimal total, int idMetodoPago,
                          String fechaDatetime) throws SQLException {
        String sql =
            "INSERT INTO compras_insumos (id_usuario, descripcion, total, id_metodo_pago, fecha_compra) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, descripcion);
            ps.setBigDecimal(3, total);
            ps.setInt(4, idMetodoPago);
            ps.setString(5, fechaDatetime);
            ps.executeUpdate();
        }
    }

    // ── Editar compra existente ────────────────────────────────────────────
    public void editar(int id, String descripcion,
                       BigDecimal total, int idMetodoPago,
                       String fechaDatetime) throws SQLException {
        String sql =
            "UPDATE compras_insumos SET descripcion=?, total=?, id_metodo_pago=?, fecha_compra=? " +
            "WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, descripcion);
            ps.setBigDecimal(2, total);
            ps.setInt(3, idMetodoPago);
            ps.setString(4, fechaDatetime);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    // ── Métodos de pago ────────────────────────────────────────────────────
    public List<String[]> listarMetodosPago() throws SQLException {
        String sql = "SELECT id, nombre FROM metodo_pago ORDER BY id";
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
            }
        }
        return lista;
    }
}
