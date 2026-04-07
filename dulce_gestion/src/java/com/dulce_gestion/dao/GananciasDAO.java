package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO especializado en el cálculo de ganancias y resúmenes financieros.
 *
 * Esta clase maneja el cálculo completo del resumen financiero para períodos
 * específicos (semana, mes actual o mes específico), consultando ventas,
 * gastos y compras de insumos para generar reportes detallados.
 *
 * Tablas leídas: ventas, carrito, usuarios, perfil_usuario, metodo_pago,
 * gastos, detalle_compra, compras_insumos, emprendimientos
 *
 * Características importantes:
 * - Cálculo de períodos variables (semana, mes, específico)
 * - Filtros por emprendimiento con COALESCE
 * - Uso de BigDecimal para precisión financiera
 * - Múltiples DTOs para diferentes tipos de transacciones
 * - Reportes completos con totales y listas detalladas
 *
 * El emprendimiento se resuelve con COALESCE sobre la columna explícita
 * de la tabla y el emprendimiento del usuario registrador, garantizando
 * que los registros del SuperAdmin queden incluidos correctamente.
 */
public class GananciasDAO {
    // dto: Data Transfer Object - objetos que transportan datos
    // =========================================================
    // MODELOS INTERNOS - DTOs para diferentes tipos de transacciones
    // =========================================================

    /**
     * DTO que representa una venta del período para mostrar en la tabla del JSP.
     *
     * Contiene los datos esenciales de una venta con fecha formateada
     * y el nombre del usuario que realizó la transacción.
     */
    public static class FilaVenta {
        public int        id;                    // ID único de la venta
        public String     fecha;                 // Fecha formateada dd/MM/yyyy HH:mm
        public String     metodoPago;            // Método de pago utilizado
        public String     realizadaPor;          // Nombre del usuario que vendió
        public BigDecimal total;                 // Monto total de la venta
    }

    /**
     * DTO que representa un gasto del período para mostrar en la tabla del JSP.
     *
     * Contiene los datos esenciales de un gasto con descripción
     * y método de pago utilizado.
     */
    public static class FilaGasto {
        public int        id;                    // ID único del gasto
        public String     fecha;                 // Fecha formateada dd/MM/yyyy HH:mm
        public String     descripcion;           // Descripción del gasto
        public String     metodoPago;            // Método de pago utilizado
        public BigDecimal total;                 // Monto total del gasto
    }

    /**
     * DTO que representa una compra de insumos del período para mostrar en la tabla del JSP.
     *
     * Contiene los datos esenciales de una compra con descripción
     * y método de pago utilizado.
     */
    public static class FilaCompra {
        public int        id;                    // ID único de la compra
        public String     fecha;                 // Fecha formateada dd/MM/yyyy HH:mm
        public String     descripcion;           // Descripción de la compra
        public String     metodoPago;            // Método de pago utilizado
        public BigDecimal total;                 // Monto total de la compra
    }

    /**
     * DTO que agrupa todos los datos calculados para el período solicitado.
     *
     * Este objeto es depositado en el request por GananciasServlet
     * con nombre "resumen" y accedido directamente desde el JSP.
     *
     * Contiene totales calculados, fechas del período y listas
     * detalladas de todos los tipos de transacciones.
     */
    public static class ResumenPeriodo {
        public BigDecimal       totalVentas  = BigDecimal.ZERO;  // Suma total de ventas
        public BigDecimal       totalGastos  = BigDecimal.ZERO;  // Suma total de gastos
        public BigDecimal       totalCompras = BigDecimal.ZERO;  // Suma total de compras
        public BigDecimal       ganancia     = BigDecimal.ZERO;  // Ganancia neta (ventas - gastos - compras)
        public String           fechaInicio;                     // Fecha de inicio del período
        public String           fechaFin;                        // Fecha de fin del período
        public String           labelPeriodo;                    // Etiqueta descriptiva del período
        public List<FilaVenta>  ventas   = new ArrayList<>();   // Lista de ventas del período
        public List<FilaGasto>  gastos   = new ArrayList<>();   // Lista de gastos del período
        public List<FilaCompra> compras  = new ArrayList<>();   // Lista de compras del período
    }

    // =========================================================
    // MÉTODO PRINCIPAL
    // =========================================================

    /**
     * Calcula el resumen financiero completo para el período indicado.
     *
     * El filtro por emprendimiento usa {@code COALESCE} sobre la columna
     * explícita de cada tabla y el emprendimiento del usuario registrador,
     * garantizando que los registros del SuperAdmin queden incluidos
     * cuando tienen un emprendimiento asignado.
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

        // ── Fase 1: calcular el rango de fechas según el período solicitado ───────────────────────
        try (Connection con = DB.obtenerConexion();
             Statement st = con.createStatement()) {

            String sql;
            if ("semana".equals(periodo)) {
                // Semana actual: desde lunes hasta domingo
                // SQL para calcular rango de la semana actual:
                // - DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY): obtiene el lunes de la semana actual
                // - DATE_ADD(..., INTERVAL 6 DAY): obtiene el domingo sumando 6 días al lunes
                // - CONCAT: crea etiqueta "Semana dd/mm - dd/mm/yyyy" para mostrar en UI
                sql = "SELECT " +
                      "DATE(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)) AS inicio, " +
                      "DATE(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY)) AS fin, " +
                      "CONCAT('Semana ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '%d/%m'), " +
                      "' - ', DATE_FORMAT(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 6 DAY), '%d/%m/%Y')) AS label";
            } else if ("mes".equals(periodo) || periodo == null || periodo.isBlank()) {
                // Mes actual: desde el primer día hasta el último día del mes
                // SQL para calcular rango del mes actual:
                // - DATE_FORMAT(CURDATE(), '%Y-%m-01'): formatea fecha actual como primer día del mes
                // - LAST_DAY(CURDATE()): obtiene el último día del mes actual
                // - DATE_FORMAT(CURDATE(), '%M %Y'): crea etiqueta "NombreMes Año" para mostrar en UI
                sql = "SELECT " +
                      "DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01')) AS inicio, " +
                      "DATE(LAST_DAY(CURDATE())) AS fin, " +
                      "DATE_FORMAT(CURDATE(), '%M %Y') AS label";
            } else {
                // Mes específico: formato YYYY-MM
                // SQL para calcular rango de un mes específico:
                // - '" + periodo + "-01': construye fecha del primer día del mes solicitado
                // - LAST_DAY(...): obtiene el último día del mes solicitado
                // - DATE_FORMAT(..., '%M %Y'): crea etiqueta "NombreMes Año" para mostrar en UI
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

        // ── Fase 2: obtener ventas del período ────────────────────────────────────────
        // El emprendimiento se resuelve con COALESCE(v.id_emprendimiento, u.id_emprendimiento)
        // para incluir ventas registradas por el SuperAdmin con emprendimiento asignado.
        {
            // Construir SQL dinámico según filtros de rol y emprendimiento
            boolean filtrarEmp = esAdminOSuperAdmin && idEmprendimiento > 0;
            // SQL para obtener ventas del período con JOINs múltiples:
            // - DATE_FORMAT: formatea fecha para presentación en UI
            // - JOINs: conecta ventas con método de pago, carrito, usuarios y perfil
            // - WHERE DATE(...): filtra por rango de fechas del período
            // - COALESCE: resuelve emprendimiento (explícito o del usuario)
            // - ORDER BY: ordena ventas más recientes primero
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
                ps.setString(1, r.fechaInicio); // Fecha de inicio del período
                ps.setString(2, r.fechaFin);    // Fecha de fin del período
                if (filtrarEmp)           ps.setInt(3, idEmprendimiento); // Filtro por emprendimiento
                else if (!esAdminOSuperAdmin) ps.setInt(3, idUsuario);        // Filtro por usuario (Empleado)
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // Mapear cada venta a DTO y acumular totales
                        FilaVenta fv    = new FilaVenta();
                        fv.id           = rs.getInt("id");
                        fv.fecha        = rs.getString("fecha");
                        fv.metodoPago   = rs.getString("metodo_pago");
                        fv.realizadaPor = rs.getString("realizada_por");
                        fv.total        = rs.getBigDecimal("total_venta");
                        r.ventas.add(fv);
                        r.totalVentas   = r.totalVentas.add(fv.total); // Acumular total de ventas
                    }
                }
            }
        }

        // ── Fase 3: obtener gastos del período (solo Admin y SuperAdmin) ───────────────────
        // El emprendimiento se resuelve con COALESCE(g.id_emprendimiento, u.id_emprendimiento).
        if (esAdminOSuperAdmin) {
            // Construir SQL para gastos con filtro opcional de emprendimiento
            boolean filtrarEmp = idEmprendimiento > 0;
            // SQL para obtener gastos del período con JOINs:
            // - DATE_FORMAT: formatea fecha para presentación en UI
            // - JOINs: conecta gastos con método de pago y usuarios
            // - WHERE DATE(...): filtra por rango de fechas del período
            // - COALESCE: resuelve emprendimiento (explícito o del usuario)
            // - ORDER BY: ordena gastos más recientes primero
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
                ps.setString(1, r.fechaInicio); // Fecha de inicio del período
                ps.setString(2, r.fechaFin);    // Fecha de fin del período
                if (filtrarEmp) ps.setInt(3, idEmprendimiento); // Filtro por emprendimiento
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // Mapear cada gasto a DTO y acumular totales
                        FilaGasto fg   = new FilaGasto();
                        fg.id          = rs.getInt("id");
                        fg.fecha       = rs.getString("fecha");
                        fg.descripcion = rs.getString("descripcion");
                        fg.metodoPago  = rs.getString("metodo_pago");
                        fg.total       = rs.getBigDecimal("total");
                        r.gastos.add(fg);
                        r.totalGastos  = r.totalGastos.add(fg.total); // Acumular total de gastos
                    }
                }
            }
        }

        // ── Fase 4: obtener compras de insumos del período (solo Admin y SuperAdmin) ───────────────
        // El emprendimiento se resuelve con COALESCE(ci.id_emprendimiento, u.id_emprendimiento).
        if (esAdminOSuperAdmin) {
            // Construir SQL para compras de insumos con filtro opcional de emprendimiento
            boolean filtrarEmp = idEmprendimiento > 0;
            // SQL para obtener compras de insumos del período con JOINs:
            // - DATE_FORMAT: formatea fecha para presentación en UI
            // - JOINs: conecta compras_insumos con método de pago y usuarios
            // - WHERE DATE(...): filtra por rango de fechas del período
            // - COALESCE: resuelve emprendimiento (explícito o del usuario)
            // - ORDER BY: ordena compras más recientes primero
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
                ps.setString(1, r.fechaInicio); // Fecha de inicio del período
                ps.setString(2, r.fechaFin);    // Fecha de fin del período
                if (filtrarEmp) ps.setInt(3, idEmprendimiento); // Filtro por emprendimiento
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // Mapear cada compra a DTO y acumular totales
                        FilaCompra fc  = new FilaCompra();
                        fc.id          = rs.getInt("id");
                        fc.fecha       = rs.getString("fecha");
                        fc.descripcion = rs.getString("descripcion");
                        fc.metodoPago  = rs.getString("metodo_pago");
                        fc.total       = rs.getBigDecimal("total");
                        r.compras.add(fc);
                        r.totalCompras = r.totalCompras.add(fc.total); // Acumular total de compras
                    }
                }
            }
        }

        // ── Fase 5: calcular ganancia neta ─────────────────────────────────────────────────────
        // Ganancia = Ventas - Gastos - Compras de insumos
        r.ganancia = r.totalVentas.subtract(r.totalGastos).subtract(r.totalCompras);
        return r;
    }

    // =========================================================
    // RETROCOMPATIBILIDAD - Método para código antiguo
    // =========================================================
    
    /**
     * Método de retrocompatibilidad para código anterior.
     *
     * Llama al método principal sin filtro de emprendimiento (idEmprendimiento = 0)
     * para mantener compatibilidad con código existente.
     *
     * @param idUsuario ID del usuario en sesión
     * @param esAdmin   true si es Administrador o SuperAdministrador
     * @param periodo   período a consultar
     * @return resumen del período sin filtro de emprendimiento
     * @throws SQLException si hay error en las consultas
     */
    public ResumenPeriodo obtenerResumen(int idUsuario, boolean esAdmin, String periodo)
            throws SQLException {
        return obtenerResumen(idUsuario, esAdmin, periodo, 0);
    }

    // =========================================================
    // LISTA DE MESES DISPONIBLES - Para selector de períodos
    // =========================================================

    /**
     * Retorna los últimos 12 meses para el selector de período del formulario.
     *
     * Genera una lista de pares [valor, etiqueta] donde:
     * - valor: formato YYYY-MM (ej: "2025-03")
     * - etiqueta: formato legible (ej: "Marzo 2025")
     *
     * @return lista de pares [valor, etiqueta] para el <select> del filtro de período
     * @throws SQLException si falla la consulta
     */
    public List<String[]> listarMesesDisponibles() throws SQLException {
        // SQL que genera secuencia de 0-11 para obtener últimos 12 meses:
        // - DATE_SUB(CURDATE(), INTERVAL seq MONTH): resta meses a la fecha actual según secuencia
        // - DATE_FORMAT(..., '%Y-%m'): genera valor para selector (ej: "2025-03")
        // - DATE_FORMAT(..., '%M %Y'): genera etiqueta legible (ej: "Marzo 2025")
        // - FROM (SELECT 0 UNION SELECT 1 ...): crea tabla temporal con secuencia 0-11
        // - ORDER BY seq: ordena del mes más antiguo al más reciente
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
                // Crear par [valor, etiqueta] para cada mes
                lista.add(new String[]{rs.getString("valor"), rs.getString("etiqueta")});
        }
        return lista;
    }
}
