package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el módulo de Ganancias.
 *
 * <p>Calcula el resumen financiero de un período (semana, mes actual o mes específico)
 * consultando ventas, gastos y compras de insumos. El emprendimiento se resuelve
 * con {@code COALESCE} sobre la columna explícita de la tabla y el emprendimiento
 * del usuario registrador, de modo que los registros creados por el SuperAdmin
 * con un emprendimiento asignado queden correctamente incluidos en el filtro.</p>
 *
 * <p>Tablas leídas: {@code ventas}, {@code carrito}, {@code usuarios},
 * {@code perfil_usuario}, {@code metodo_pago}, {@code gastos},
 * {@code detalle_compra}, {@code compras_insumos}, {@code emprendimientos}.</p>
 */
public class GananciasDAO {

    // =========================================================
    // MODELOS INTERNOS
    // =========================================================

    /** Proyección de una venta del período para la tabla del JSP. */
    public static class FilaVenta {
        public int        id;
        /** Fecha formateada {@code dd/MM/yyyy HH:mm}. */
        public String     fecha;
        public String     metodoPago;
        public String     realizadaPor;
        public BigDecimal total;
    }

    /** Proyección de un gasto del período para la tabla del JSP. */
    public static class FilaGasto {
        public int        id;
        public String     fecha;
        public String     descripcion;
        public String     metodoPago;
        public BigDecimal total;
    }

    /** Proyección de una compra de insumos del período para la tabla del JSP. */
    public static class FilaCompra {
        public int        id;
        public String     fecha;
        public String     descripcion;
        public String     metodoPago;
        public BigDecimal total;
    }

    /**
     * Agrupa todos los datos calculados para el período solicitado.
     * {@code GananciasServlet} deposita este objeto en el request con nombre
     * {@code "resumen"} y el JSP accede directamente a sus campos.
     */
    public static class ResumenPeriodo {
        public BigDecimal       totalVentas  = BigDecimal.ZERO;
        public BigDecimal       totalGastos  = BigDecimal.ZERO;
        public BigDecimal       totalCompras = BigDecimal.ZERO;
        public BigDecimal       ganancia     = BigDecimal.ZERO;
        public String           fechaInicio;
        public String           fechaFin;
        public String           labelPeriodo;
        public List<FilaVenta>  ventas   = new ArrayList<>();
        public List<FilaGasto>  gastos   = new ArrayList<>();
        public List<FilaCompra> compras  = new ArrayList<>();
    }

    // =========================================================
    // MÉTODO PRINCIPAL
    // =========================================================

    /**
     * Calcula el resumen financiero completo para el período indicado.
     *
     * <p>El filtro por emprendimiento usa {@code COALESCE} sobre la columna
     * explícita de cada tabla y el emprendimiento del usuario registrador,
     * garantizando que los registros del SuperAdmin queden incluidos
     * cuando tienen un emprendimiento asignado.</p>
     *
     * @param idUsuario        ID del usuario en sesión (para filtrar empleados).
     * @param esAdminOSuperAdmin {@code true} si el rol es Administrador o SuperAdministrador.
     * @param periodo          {@code "semana"}, {@code "mes"} o {@code "YYYY-MM"}.
     * @param idEmprendimiento filtro de emprendimiento; {@code 0} retorna todos.
     * @return resumen con totales y listas de transacciones del período.
     * @throws SQLException si falla alguna consulta.
     */
    public ResumenPeriodo obtenerResumen(int idUsuario, boolean esAdminOSuperAdmin,
                                         String periodo, int idEmprendimiento)
            throws SQLException {

        ResumenPeriodo r = new ResumenPeriodo();

        // ── Fase 1: calcular el rango de fechas ───────────────────────────
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

        // ── Fase 2: ventas del período ────────────────────────────────────
        // El emprendimiento se resuelve con COALESCE(v.id_emprendimiento, u.id_emprendimiento)
        // para incluir ventas registradas por el SuperAdmin con emprendimiento asignado.
        {
            boolean filtrarEmp = esAdminOSuperAdmin && idEmprendimiento > 0;
            String sqlVentas =
                "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
                "mp.nombre AS metodo_pago, pu.nombre_completo AS realizada_por, v.total_venta " +
                "FROM ventas v " +
                "JOIN metodo_pago mp ON mp.id = v.id_metodo_pago " +
                "JOIN carrito c ON c.id = v.id_carrito " +
                "JOIN usuarios u ON u.id = c.id_usuario " +
                "JOIN perfil_usuario pu ON pu.id_usuario = u.id " +
                "WHERE DATE(v.fecha_venta) BETWEEN ? AND ? " +
                (filtrarEmp
                    ? "AND COALESCE(v.id_emprendimiento, u.id_emprendimiento) = ? "
                    : (!esAdminOSuperAdmin ? "AND c.id_usuario = ? " : "")) +
                "ORDER BY v.fecha_venta DESC";

            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(sqlVentas)) {
                ps.setString(1, r.fechaInicio);
                ps.setString(2, r.fechaFin);
                if (filtrarEmp)           ps.setInt(3, idEmprendimiento);
                else if (!esAdminOSuperAdmin) ps.setInt(3, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FilaVenta fv    = new FilaVenta();
                        fv.id           = rs.getInt("id");
                        fv.fecha        = rs.getString("fecha");
                        fv.metodoPago   = rs.getString("metodo_pago");
                        fv.realizadaPor = rs.getString("realizada_por");
                        fv.total        = rs.getBigDecimal("total_venta");
                        r.ventas.add(fv);
                        r.totalVentas   = r.totalVentas.add(fv.total);
                    }
                }
            }
        }

        // ── Fase 3: gastos del período (solo Admin y SuperAdmin) ──────────
        // El emprendimiento se resuelve con COALESCE(dc.id_emprendimiento, u.id_emprendimiento).
        if (esAdminOSuperAdmin) {
            boolean filtrarEmp = idEmprendimiento > 0;
            String sqlGastos =
                "SELECT g.id, DATE_FORMAT(g.fecha,'%d/%m/%Y %H:%i') AS fecha, " +
                "g.descripcion, mp.nombre AS metodo_pago, g.total " +
                "FROM gastos g " +
                "JOIN metodo_pago mp ON mp.id = g.id_metodo_pago " +
                "JOIN usuarios u ON u.id = g.id_usuario " +
                "WHERE DATE(g.fecha) BETWEEN ? AND ? " +
                (filtrarEmp ? "AND COALESCE(g.id_emprendimiento, u.id_emprendimiento) = ? " : "") +
                "ORDER BY g.fecha DESC";

            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(sqlGastos)) {
                ps.setString(1, r.fechaInicio);
                ps.setString(2, r.fechaFin);
                if (filtrarEmp) ps.setInt(3, idEmprendimiento);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FilaGasto fg   = new FilaGasto();
                        fg.id          = rs.getInt("id");
                        fg.fecha       = rs.getString("fecha");
                        fg.descripcion = rs.getString("descripcion");
                        fg.metodoPago  = rs.getString("metodo_pago");
                        fg.total       = rs.getBigDecimal("total");
                        r.gastos.add(fg);
                        r.totalGastos  = r.totalGastos.add(fg.total);
                    }
                }
            }
        }

        // ── Fase 4: compras de insumos del período (solo Admin y SuperAdmin) ──
        // El emprendimiento se resuelve con COALESCE(ci.id_emprendimiento, u.id_emprendimiento).
        if (esAdminOSuperAdmin) {
            boolean filtrarEmp = idEmprendimiento > 0;
            String sqlCompras =
                "SELECT ci.id, DATE_FORMAT(ci.fecha_compra,'%d/%m/%Y %H:%i') AS fecha, " +
                "ci.descripcion, mp.nombre AS metodo_pago, ci.total " +
                "FROM compras_insumos ci " +
                "JOIN metodo_pago mp ON mp.id = ci.id_metodo_pago " +
                "JOIN usuarios u ON u.id = ci.id_usuario " +
                "WHERE DATE(ci.fecha_compra) BETWEEN ? AND ? " +
                (filtrarEmp ? "AND COALESCE(ci.id_emprendimiento, u.id_emprendimiento) = ? " : "") +
                "ORDER BY ci.fecha_compra DESC";

            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(sqlCompras)) {
                ps.setString(1, r.fechaInicio);
                ps.setString(2, r.fechaFin);
                if (filtrarEmp) ps.setInt(3, idEmprendimiento);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FilaCompra fc  = new FilaCompra();
                        fc.id          = rs.getInt("id");
                        fc.fecha       = rs.getString("fecha");
                        fc.descripcion = rs.getString("descripcion");
                        fc.metodoPago  = rs.getString("metodo_pago");
                        fc.total       = rs.getBigDecimal("total");
                        r.compras.add(fc);
                        r.totalCompras = r.totalCompras.add(fc.total);
                    }
                }
            }
        }

        r.ganancia = r.totalVentas.subtract(r.totalGastos).subtract(r.totalCompras);
        return r;
    }

    /** Retrocompatibilidad: calcula sin filtro de emprendimiento. */
    public ResumenPeriodo obtenerResumen(int idUsuario, boolean esAdmin, String periodo)
            throws SQLException {
        return obtenerResumen(idUsuario, esAdmin, periodo, 0);
    }

    // =========================================================
    // LISTA DE MESES DISPONIBLES
    // =========================================================

    /**
     * Retorna los últimos 12 meses para el {@code <select>} del filtro de período.
     *
     * @return lista de pares {@code [valor, etiqueta]}, ej: {@code ["2025-03", "Marzo 2025"]}.
     * @throws SQLException si falla la consulta.
     */
    public List<String[]> listarMesesDisponibles() throws SQLException {
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
