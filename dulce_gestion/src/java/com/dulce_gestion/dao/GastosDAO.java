package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * DAO para el módulo de Gastos.
 * Tablas involucradas: compras → detalle_compra → gastos
 *
 * Editar actualiza:
 *   - compras.total_compra, compras.fecha_compra
 *   - detalle_compra.descripcion
 *   - gastos.total_gasto, gastos.fecha_gasto, gastos.id_metodo_pago
 */
public class GastosDAO {

    /* ─── Modelo ───────────────────────────────────────────── */
    public static class FilaGasto {
        public int        id;               // gastos.id
        public int        idDetalleCompra;  // para editar
        public int        idCompra;         // para editar
        public String     fecha;            // formateada para mostrar
        public String     fechaRaw;         // yyyy-MM-dd para el input
        public String     descripcion;
        public int        idMetodoPago;
        public String     metodoPago;
        public String     registradoPor;
        public BigDecimal total;
    }

    /* ─── Listar todos ─────────────────────────────────────── */
    public List<FilaGasto> listar() throws SQLException {
        String sql =
            "SELECT g.id, g.id_detalle_compra, dc.id_compra, " +
            "DATE_FORMAT(g.fecha_gasto,'%d/%m/%Y %H:%i') AS fecha, " +
            "DATE_FORMAT(g.fecha_gasto,'%Y-%m-%d') AS fecha_raw, " +
            "dc.descripcion, g.id_metodo_pago, mp.nombre AS metodo_pago, " +
            "pu.nombre_completo AS registrado_por, g.total_gasto " +
            "FROM gastos g " +
            "JOIN detalle_compra dc ON dc.id=g.id_detalle_compra " +
            "JOIN metodo_pago mp ON mp.id=g.id_metodo_pago " +
            "JOIN usuarios u ON u.id=dc.id_usuario " +
            "JOIN perfil_usuario pu ON pu.id_usuario=u.id " +
            "ORDER BY g.fecha_gasto DESC";
        List<FilaGasto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FilaGasto f = new FilaGasto();
                f.id              = rs.getInt("id");
                f.idDetalleCompra = rs.getInt("id_detalle_compra");
                f.idCompra        = rs.getInt("id_compra");
                f.fecha           = rs.getString("fecha");
                f.fechaRaw        = rs.getString("fecha_raw");
                f.descripcion     = rs.getString("descripcion");
                f.idMetodoPago    = rs.getInt("id_metodo_pago");
                f.metodoPago      = rs.getString("metodo_pago");
                f.registradoPor   = rs.getString("registrado_por");
                f.total           = rs.getBigDecimal("total_gasto");
                lista.add(f);
            }
        }
        return lista;
    }

    /* ─── Obtener uno por id ───────────────────────────────── */
    public FilaGasto obtenerPorId(int idGasto) throws SQLException {
        String sql =
            "SELECT g.id, g.id_detalle_compra, dc.id_compra, " +
            "DATE_FORMAT(g.fecha_gasto,'%d/%m/%Y %H:%i') AS fecha, " +
            "DATE_FORMAT(g.fecha_gasto,'%Y-%m-%d') AS fecha_raw, " +
            "dc.descripcion, g.id_metodo_pago, mp.nombre AS metodo_pago, " +
            "pu.nombre_completo AS registrado_por, g.total_gasto " +
            "FROM gastos g " +
            "JOIN detalle_compra dc ON dc.id=g.id_detalle_compra " +
            "JOIN metodo_pago mp ON mp.id=g.id_metodo_pago " +
            "JOIN usuarios u ON u.id=dc.id_usuario " +
            "JOIN perfil_usuario pu ON pu.id_usuario=u.id " +
            "WHERE g.id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGasto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Gasto no encontrado.");
                FilaGasto f = new FilaGasto();
                f.id              = rs.getInt("id");
                f.idDetalleCompra = rs.getInt("id_detalle_compra");
                f.idCompra        = rs.getInt("id_compra");
                f.fecha           = rs.getString("fecha");
                f.fechaRaw        = rs.getString("fecha_raw");
                f.descripcion     = rs.getString("descripcion");
                f.idMetodoPago    = rs.getInt("id_metodo_pago");
                f.metodoPago      = rs.getString("metodo_pago");
                f.registradoPor   = rs.getString("registrado_por");
                f.total           = rs.getBigDecimal("total_gasto");
                return f;
            }
        }
    }

    /* ─── Listar métodos de pago ───────────────────────────── */
    public List<String[]> listarMetodosPago() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement("SELECT id, nombre FROM metodo_pago ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    /* ─── Registrar (transacción 3 tablas) ─────────────────── */
    public void registrar(int idUsuario, String descripcion,
                          BigDecimal total, int idMetodoPago,
                          String fechaGasto) throws SQLException {
        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                int idCompra;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO compras (fecha_compra, total_compra) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, fechaGasto);
                    ps.setBigDecimal(2, total);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); idCompra = rs.getInt(1); }
                }
                int idDetalle;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO detalle_compra (id_usuario, descripcion, id_compra) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idUsuario);
                    ps.setString(2, descripcion);
                    ps.setInt(3, idCompra);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); idDetalle = rs.getInt(1); }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO gastos (id_detalle_compra, id_metodo_pago, fecha_gasto, total_gasto) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, idDetalle);
                    ps.setInt(2, idMetodoPago);
                    ps.setString(3, fechaGasto);
                    ps.setBigDecimal(4, total);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) { con.rollback(); throw e; }
        }
    }

    /* ─── Editar (actualiza las 3 tablas en transacción) ────── */
    public void editar(int idGasto, int idDetalleCompra, int idCompra,
                       String descripcion, BigDecimal total,
                       int idMetodoPago, String fechaGasto) throws SQLException {
        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                // 1. compras
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE compras SET fecha_compra=?, total_compra=? WHERE id=?")) {
                    ps.setString(1, fechaGasto);
                    ps.setBigDecimal(2, total);
                    ps.setInt(3, idCompra);
                    ps.executeUpdate();
                }
                // 2. detalle_compra
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE detalle_compra SET descripcion=? WHERE id=?")) {
                    ps.setString(1, descripcion);
                    ps.setInt(2, idDetalleCompra);
                    ps.executeUpdate();
                }
                // 3. gastos
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE gastos SET id_metodo_pago=?, fecha_gasto=?, total_gasto=? WHERE id=?")) {
                    ps.setInt(1, idMetodoPago);
                    ps.setString(2, fechaGasto);
                    ps.setBigDecimal(3, total);
                    ps.setInt(4, idGasto);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) { con.rollback(); throw e; }
        }
    }
}
