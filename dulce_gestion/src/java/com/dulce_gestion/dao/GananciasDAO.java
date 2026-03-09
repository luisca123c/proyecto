package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * DAO para el módulo de Ganancias.
 * Soporta 3 modos de período:
 *   "semana"  → lunes–domingo de la semana actual
 *   "mes"     → mes actual completo
 *   "YYYY-MM" → mes específico (hasta 12 meses atrás)
 */
public class GananciasDAO {

    public static class FilaVenta {
        public int        id;
        public String     fecha;
        public String     metodoPago;
        public String     realizadaPor;
        public BigDecimal total;
    }

    public static class FilaGasto {
        public int        id;
        public String     fecha;
        public String     descripcion;
        public String     metodoPago;
        public BigDecimal total;
    }

    public static class ResumenPeriodo {
        public BigDecimal      totalVentas  = BigDecimal.ZERO;
        public BigDecimal      totalGastos  = BigDecimal.ZERO;
        public BigDecimal      ganancia     = BigDecimal.ZERO;
        public String          fechaInicio;
        public String          fechaFin;
        public String          labelPeriodo; // texto para mostrar al usuario
        public List<FilaVenta> ventas       = new ArrayList<>();
        public List<FilaGasto> gastos       = new ArrayList<>();
    }

    /* ─── Método principal ─────────────────────────────────── */
    /*
     * periodo:
     *   "semana"  → semana actual
     *   "mes"     → mes actual
     *   "YYYY-MM" → mes específico
     */
    public ResumenPeriodo obtenerResumen(int idUsuario, boolean esAdminOSuperAdmin, String periodo)
            throws SQLException {

        ResumenPeriodo r = new ResumenPeriodo();

        // 1. Calcular rango de fechas según el período
        try (Connection con = DB.obtenerConexion();
             Statement st = con.createStatement()) {

            String sql;
            if ("semana".equals(periodo)) {
                sql = "SELECT " +
                      "DATE(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)) AS inicio, " +
                      "DATE(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY)) AS fin, " +
                      "CONCAT('Semana ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '%d/%m'), " +
                      "' - ', DATE_FORMAT(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY), '%d/%m/%Y')) AS label";
            } else if ("mes".equals(periodo) || periodo == null || periodo.isBlank()) {
                sql = "SELECT " +
                      "DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01')) AS inicio, " +
                      "DATE(LAST_DAY(CURDATE())) AS fin, " +
                      "DATE_FORMAT(CURDATE(), '%M %Y') AS label";
            } else {
                // Formato YYYY-MM
                sql = "SELECT " +
                      "DATE('" + periodo + "-01') AS inicio, " +
                      "DATE(LAST_DAY('" + periodo + "-01')) AS fin, " +
                      "DATE_FORMAT('" + periodo + "-01', '%M %Y') AS label";
            }

            try (ResultSet rs = st.executeQuery(sql)) {
                rs.next();
                r.fechaInicio  = rs.getString("inicio");
                r.fechaFin     = rs.getString("fin");
                r.labelPeriodo = rs.getString("label");
            }
        }

        // 2. Ventas del período
        String sqlVentas = esAdminOSuperAdmin
            ? "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
              "mp.nombre AS metodo_pago, pu.nombre_completo AS realizada_por, v.total_venta " +
              "FROM ventas v JOIN metodo_pago mp ON mp.id=v.id_metodo_pago " +
              "JOIN carrito c ON c.id=v.id_carrito " +
              "JOIN usuarios u ON u.id=c.id_usuario " +
              "JOIN perfil_usuario pu ON pu.id_usuario=u.id " +
              "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? ORDER BY v.fecha_venta DESC"
            : "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
              "mp.nombre AS metodo_pago, pu.nombre_completo AS realizada_por, v.total_venta " +
              "FROM ventas v JOIN metodo_pago mp ON mp.id=v.id_metodo_pago " +
              "JOIN carrito c ON c.id=v.id_carrito " +
              "JOIN usuarios u ON u.id=c.id_usuario " +
              "JOIN perfil_usuario pu ON pu.id_usuario=u.id " +
              "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? AND c.id_usuario=? ORDER BY v.fecha_venta DESC";

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlVentas)) {
            ps.setString(1, r.fechaInicio);
            ps.setString(2, r.fechaFin);
            if (!esAdminOSuperAdmin) ps.setInt(3, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaVenta fv   = new FilaVenta();
                    fv.id          = rs.getInt("id");
                    fv.fecha       = rs.getString("fecha");
                    fv.metodoPago  = rs.getString("metodo_pago");
                    fv.realizadaPor = rs.getString("realizada_por");
                    fv.total       = rs.getBigDecimal("total_venta");
                    r.ventas.add(fv);
                    r.totalVentas  = r.totalVentas.add(fv.total);
                }
            }
        }

        // 3. Gastos del período (solo admins)
        if (esAdminOSuperAdmin) {
            String sqlGastos =
                "SELECT g.id, DATE_FORMAT(g.fecha_gasto,'%d/%m/%Y %H:%i') AS fecha, " +
                "dc.descripcion, mp.nombre AS metodo_pago, g.total_gasto " +
                "FROM gastos g JOIN detalle_compra dc ON dc.id=g.id_detalle_compra " +
                "JOIN metodo_pago mp ON mp.id=g.id_metodo_pago " +
                "WHERE DATE(g.fecha_gasto) BETWEEN ? AND ? ORDER BY g.fecha_gasto DESC";
            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(sqlGastos)) {
                ps.setString(1, r.fechaInicio);
                ps.setString(2, r.fechaFin);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FilaGasto fg   = new FilaGasto();
                        fg.id          = rs.getInt("id");
                        fg.fecha       = rs.getString("fecha");
                        fg.descripcion = rs.getString("descripcion");
                        fg.metodoPago  = rs.getString("metodo_pago");
                        fg.total       = rs.getBigDecimal("total_gasto");
                        r.gastos.add(fg);
                        r.totalGastos  = r.totalGastos.add(fg.total);
                    }
                }
            }
        }

        r.ganancia = r.totalVentas.subtract(r.totalGastos);
        return r;
    }

    /* ─── Lista de meses disponibles (12 meses atrás) ──────── */
    public List<String[]> listarMesesDisponibles() throws SQLException {
        // Genera los últimos 12 meses como [YYYY-MM, Nombre Mes YYYY]
        String sql =
            "SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL seq MONTH), '%Y-%m') AS valor, " +
            "DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL seq MONTH), '%M %Y') AS etiqueta " +
            "FROM (SELECT 0 AS seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 " +
            "UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 " +
            "UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11) meses " +
            "ORDER BY seq";
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new String[]{rs.getString("valor"), rs.getString("etiqueta")});
        }
        return lista;
    }
}
