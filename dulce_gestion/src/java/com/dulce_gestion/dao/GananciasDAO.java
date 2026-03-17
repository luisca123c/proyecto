package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * DAO: GananciasDAO
 * Tablas leídas:  ventas, carrito, usuarios, perfil_usuario,
 *                 metodo_pago, gastos, detalle_compra
 * Usado por:      GananciasServlet
 * ============================================================
 *
 */
public class GananciasDAO {

    // =========================================================
    // MODELOS INTERNOS
    // =========================================================

    /** Representa una fila de la tabla de ventas del período. */
    public static class FilaVenta {
        public int        id;
        public String     fecha;        // Formateada: "15/03/2025 14:30"
        public String     metodoPago;   // Nombre del método: "Efectivo" o "Nequi"
        public String     realizadaPor; // Nombre completo del empleado que hizo la venta
        public BigDecimal total;
    }

    /** Representa una fila de la tabla de gastos del período. */
    public static class FilaGasto {
        public int        id;
        public String     fecha;        // Formateada: "15/03/2025 14:30"
        public String     descripcion;
        public String     metodoPago;
        public BigDecimal total;
    }

    /**
     * Agrupa todos los datos calculados para el período solicitado.
     * GananciasServlet pone este objeto en el request con nombre "resumen"
     * y el JSP accede directamente a sus campos: resumen.totalVentas, etc.
     */
    /** Representa una fila del historial de compras de insumos del período. */
    public static class FilaCompra {
        public int        id;
        public String     fecha;
        public String     descripcion;
        public String     metodoPago;
        public BigDecimal total;
    }

    public static class ResumenPeriodo {
        public BigDecimal      totalVentas  = BigDecimal.ZERO;
        public BigDecimal      totalGastos  = BigDecimal.ZERO;
        public BigDecimal      totalCompras = BigDecimal.ZERO;
        public BigDecimal      ganancia     = BigDecimal.ZERO; // totalVentas - totalGastos - totalCompras
        public String          fechaInicio;
        public String          fechaFin;
        public String          labelPeriodo;
        public List<FilaVenta> ventas       = new ArrayList<>();
        public List<FilaGasto> gastos       = new ArrayList<>();
        public List<FilaCompra> compras     = new ArrayList<>();
    }

    // =========================================================
    // MÉTODO PRINCIPAL
    // =========================================================

    /**
     * Calcula el resumen financiero completo para el período indicado.
     *
     */
    public ResumenPeriodo obtenerResumen(int idUsuario, boolean esAdminOSuperAdmin,
                                         String periodo, int idEmprendimiento)
            throws SQLException {

        ResumenPeriodo r = new ResumenPeriodo();

        // ── Fase 1: calcular el rango de fechas en SQL ────────────────────
        try (Connection con = DB.obtenerConexion();
             Statement st = con.createStatement()) {

            String sql;
            if ("semana".equals(periodo)) {
                // WEEKDAY(CURDATE()): 0=lunes, 1=martes, ..., 6=domingo
                // DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY) → lunes de esta semana
                // DATE_ADD(lunes, INTERVAL 6 DAY) → domingo de esta semana
                sql = "SELECT " +
                      "DATE(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)) AS inicio, " +
                      "DATE(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY)) AS fin, " +
                      "CONCAT('Semana ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '%d/%m'), " +
                      "' - ', DATE_FORMAT(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY), '%d/%m/%Y')) AS label";
            } else if ("mes".equals(periodo) || periodo == null || periodo.isBlank()) {
                // DATE_FORMAT(CURDATE(), '%Y-%m-01') → primer día del mes actual
                // LAST_DAY(CURDATE()) → último día del mes actual
                sql = "SELECT " +
                      "DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01')) AS inicio, " +
                      "DATE(LAST_DAY(CURDATE())) AS fin, " +
                      "DATE_FORMAT(CURDATE(), '%M %Y') AS label"; // "Marzo 2025"
            } else {
                // Mes específico en formato "YYYY-MM" → ej: "2025-01"
                sql = "SELECT " +
                      "DATE('" + periodo + "-01') AS inicio, " +
                      "DATE(LAST_DAY('" + periodo + "-01')) AS fin, " +
                      "DATE_FORMAT('" + periodo + "-01', '%M %Y') AS label";
            }

            try (ResultSet rs = st.executeQuery(sql)) {
                rs.next();
                r.fechaInicio  = rs.getString("inicio");
                r.fechaFin     = rs.getString("fin");
                r.labelPeriodo = rs.getString("label"); // Ej: "Semana 10/03 - 16/03/2025"
            }
        }

        // ── Fase 2: consultar ventas del período ──────────────────────────
        // Para Admin/SuperAdmin: todas las ventas (sin filtro por usuario)
        // Para Empleado: solo sus propias ventas (AND c.id_usuario = ?)
        // Admin/SuperAdmin filtra por emprendimiento; Empleado por su propio usuario
        String sqlVentas;
        int paramIdx3Ventas = 0; // 0=no param extra, >0=id a poner en ?3
        if (esAdminOSuperAdmin && idEmprendimiento > 0) {
            sqlVentas = "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
              "mp.nombre AS metodo_pago, pu.nombre_completo AS realizada_por, v.total_venta " +
              "FROM ventas v JOIN metodo_pago mp ON mp.id=v.id_metodo_pago " +
              "JOIN carrito c ON c.id=v.id_carrito " +
              "JOIN usuarios u ON u.id=c.id_usuario " +
              "JOIN perfil_usuario pu ON pu.id_usuario=u.id " +
              "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? AND u.id_emprendimiento=? ORDER BY v.fecha_venta DESC";
            paramIdx3Ventas = idEmprendimiento;
        } else if (esAdminOSuperAdmin) {
            sqlVentas = "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
              "mp.nombre AS metodo_pago, pu.nombre_completo AS realizada_por, v.total_venta " +
              "FROM ventas v JOIN metodo_pago mp ON mp.id=v.id_metodo_pago " +
              "JOIN carrito c ON c.id=v.id_carrito " +
              "JOIN usuarios u ON u.id=c.id_usuario " +
              "JOIN perfil_usuario pu ON pu.id_usuario=u.id " +
              "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? ORDER BY v.fecha_venta DESC";
        } else {
            sqlVentas = "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
              "mp.nombre AS metodo_pago, pu.nombre_completo AS realizada_por, v.total_venta " +
              "FROM ventas v JOIN metodo_pago mp ON mp.id=v.id_metodo_pago " +
              "JOIN carrito c ON c.id=v.id_carrito " +
              "JOIN usuarios u ON u.id=c.id_usuario " +
              "JOIN perfil_usuario pu ON pu.id_usuario=u.id " +
              "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? AND c.id_usuario=? ORDER BY v.fecha_venta DESC";
            paramIdx3Ventas = idUsuario;
        }

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlVentas)) {
            ps.setString(1, r.fechaInicio);
            ps.setString(2, r.fechaFin);
            if (paramIdx3Ventas > 0) ps.setInt(3, paramIdx3Ventas);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaVenta fv    = new FilaVenta();
                    fv.id           = rs.getInt("id");
                    fv.fecha        = rs.getString("fecha");
                    fv.metodoPago   = rs.getString("metodo_pago");
                    fv.realizadaPor = rs.getString("realizada_por");
                    fv.total        = rs.getBigDecimal("total_venta");
                    r.ventas.add(fv);
                    r.totalVentas   = r.totalVentas.add(fv.total); // Acumular total
                }
            }
        }

        // ── Fase 3: consultar gastos (solo Admin y SuperAdmin) ────────────
        // Los empleados no ven información de gastos del negocio
        if (esAdminOSuperAdmin) {
            String filtroEmpGastos = idEmprendimiento > 0
                ? "AND u.id_emprendimiento=? " : "";
            String sqlGastos =
                "SELECT g.id, DATE_FORMAT(g.fecha_gasto,'%d/%m/%Y %H:%i') AS fecha, " +
                "dc.descripcion, mp.nombre AS metodo_pago, g.total_gasto " +
                "FROM gastos g JOIN detalle_compra dc ON dc.id=g.id_detalle_compra " +
                "JOIN metodo_pago mp ON mp.id=g.id_metodo_pago " +
                "JOIN usuarios u ON u.id=dc.id_usuario " +
                "WHERE DATE(g.fecha_gasto) BETWEEN ? AND ? " + filtroEmpGastos +
                "ORDER BY g.fecha_gasto DESC";
            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(sqlGastos)) {
                ps.setString(1, r.fechaInicio);
                ps.setString(2, r.fechaFin);
                if (idEmprendimiento > 0) ps.setInt(3, idEmprendimiento);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FilaGasto fg    = new FilaGasto();
                        fg.id           = rs.getInt("id");
                        fg.fecha        = rs.getString("fecha");
                        fg.descripcion  = rs.getString("descripcion");
                        fg.metodoPago   = rs.getString("metodo_pago");
                        fg.total        = rs.getBigDecimal("total_gasto");
                        r.gastos.add(fg);
                        r.totalGastos   = r.totalGastos.add(fg.total); // Acumular total
                    }
                }
            }
        }

        // Ganancia neta = ingresos por ventas menos egresos por gastos
        // Fase 4 — Consultar compras de insumos del período (solo admins)
        if (esAdminOSuperAdmin) {
            String filtroEmpCompras = idEmprendimiento > 0
                ? "AND u2.id_emprendimiento=? " : "";
            String sqlCompras =
                "SELECT ci.id, DATE_FORMAT(ci.fecha_compra,'%d/%m/%Y %H:%i') AS fecha, " +
                "ci.descripcion, mp.nombre AS metodo_pago, ci.total " +
                "FROM compras_insumos ci " +
                "JOIN metodo_pago mp ON mp.id = ci.id_metodo_pago " +
                "JOIN usuarios u2 ON u2.id = ci.id_usuario " +
                "WHERE DATE(ci.fecha_compra) BETWEEN ? AND ? " + filtroEmpCompras +
                "ORDER BY ci.fecha_compra DESC";
            try (Connection conC = DB.obtenerConexion();
                 PreparedStatement psC = conC.prepareStatement(sqlCompras)) {
                psC.setString(1, r.fechaInicio);
                psC.setString(2, r.fechaFin);
                if (idEmprendimiento > 0) psC.setInt(3, idEmprendimiento);
                try (ResultSet rsC = psC.executeQuery()) {
                    while (rsC.next()) {
                        FilaCompra fc = new FilaCompra();
                        fc.id          = rsC.getInt("id");
                        fc.fecha       = rsC.getString("fecha");
                        fc.descripcion = rsC.getString("descripcion");
                        fc.metodoPago  = rsC.getString("metodo_pago");
                        fc.total       = rsC.getBigDecimal("total");
                        r.compras.add(fc);
                        r.totalCompras = r.totalCompras.add(fc.total);
                    }
                }
            }
        }

        r.ganancia = r.totalVentas.subtract(r.totalGastos).subtract(r.totalCompras);
        return r;
    }

    // =========================================================
    // LISTA DE MESES DISPONIBLES
    // =========================================================

    /**
     * Retorna los últimos 12 meses para el <select> del filtro de período.
     *
     */
    /** Retrocompatibilidad. */
    public ResumenPeriodo obtenerResumen(int idUsuario, boolean esAdmin, String periodo)
            throws SQLException {
        return obtenerResumen(idUsuario, esAdmin, periodo, 0);
    }

    public List<String[]> listarMesesDisponibles() throws SQLException {
        // Genera 12 filas usando UNION de los números 0 al 11
        // seq=0 → mes actual, seq=1 → mes pasado, etc.
        String sql =
            "SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL seq MONTH), '%Y-%m') AS valor, " +
            "DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL seq MONTH), '%M %Y') AS etiqueta " +
            "FROM (SELECT 0 AS seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 " +
            "UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 " +
            "UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11) meses " +
            "ORDER BY seq"; // seq=0 primero → mes actual al inicio de la lista

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
