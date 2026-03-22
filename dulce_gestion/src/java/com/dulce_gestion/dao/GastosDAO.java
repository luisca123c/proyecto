package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el módulo de gastos.
 *
 * <p>Cada gasto se distribuye en tres tablas relacionadas:</p>
 * <ul>
 *   <li>{@code compras} — encabezado: fecha y total de la compra.</li>
 *   <li>{@code detalle_compra} — quién registró el gasto, descripción e
 *       {@code id_emprendimiento} (permite asociar gastos del SuperAdmin
 *       a un emprendimiento específico).</li>
 *   <li>{@code gastos} — registro financiero: método de pago, fecha y monto.</li>
 * </ul>
 *
 * <p>Las operaciones de escritura ({@link #registrar} y {@link #editar})
 * ejecutan las tres modificaciones dentro de una única transacción.</p>
 *
 * <p>Tablas escritas: {@code compras}, {@code detalle_compra}, {@code gastos}.<br>
 * Tablas leídas: {@code metodo_pago}, {@code usuarios}, {@code perfil_usuario},
 * {@code emprendimientos}, {@code roles}.</p>
 */
public class GastosDAO {

    // =========================================================
    // MODELO INTERNO
    // =========================================================

    /**
     * Proyección plana de un gasto para la vista del historial.
     * Incluye los IDs de las tres tablas subyacentes para permitir ediciones.
     */
    public static class FilaGasto {
        public int        id;
        public int        idDetalleCompra;
        public int        idCompra;
        /** Fecha formateada {@code dd/MM/yyyy HH:mm} para mostrar en tabla. */
        public String     fecha;
        /** Fecha en formato {@code yyyy-MM-dd} para usar en {@code <input type="date">}. */
        public String     fechaRaw;
        public String     descripcion;
        public int        idMetodoPago;
        public String     metodoPago;
        public String     registradoPor;
        public BigDecimal total;
        public String     nombreEmprendimiento;
        /** {@code true} cuando quien registró el gasto tiene rol SuperAdministrador. */
        public boolean    registradoPorSuperAdmin;
    }

    // =========================================================
    // LISTAR
    // =========================================================

    /**
     * Retorna el historial de gastos ordenado del más reciente al más antiguo.
     *
     * <p>El emprendimiento se resuelve con {@code COALESCE(dc.id_emprendimiento,
     * u.id_emprendimiento)}: si el gasto fue registrado por el SuperAdmin con un
     * emprendimiento explícito usa ese; si no, cae al emprendimiento del usuario.</p>
     *
     * @param idEmprendimiento filtro de emprendimiento; {@code 0} retorna todos.
     * @return lista de gastos según el filtro aplicado.
     * @throws SQLException si falla la consulta.
     */
    public List<FilaGasto> listar(int idEmprendimiento) throws SQLException {
        boolean filtrar = idEmprendimiento > 0;
        String sql =
            "SELECT g.id, g.id_detalle_compra, dc.id_compra, " +
            "DATE_FORMAT(g.fecha_gasto,'%d/%m/%Y %H:%i') AS fecha, " +
            "DATE_FORMAT(g.fecha_gasto,'%Y-%m-%d') AS fecha_raw, " +
            "dc.descripcion, g.id_metodo_pago, mp.nombre AS metodo_pago, " +
            "pu.nombre_completo AS registrado_por, g.total_gasto, " +
            "COALESCE(e2.nombre, e1.nombre) AS nombre_emprendimiento, " +
            "r.nombre AS rol_registrador " +
            "FROM gastos g " +
            "JOIN detalle_compra dc ON dc.id = g.id_detalle_compra " +
            "JOIN metodo_pago mp ON mp.id = g.id_metodo_pago " +
            "JOIN usuarios u ON u.id = dc.id_usuario " +
            "JOIN perfil_usuario pu ON pu.id_usuario = u.id " +
            "JOIN roles r ON r.id = u.id_rol " +
            "LEFT JOIN emprendimientos e1 ON e1.id = u.id_emprendimiento " +
            "LEFT JOIN emprendimientos e2 ON e2.id = dc.id_emprendimiento " +
            (filtrar
                ? "WHERE COALESCE(dc.id_emprendimiento, u.id_emprendimiento) = ? "
                : "") +
            "ORDER BY g.fecha_gasto DESC";

        List<FilaGasto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filtrar) ps.setInt(1, idEmprendimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Lista todos los gastos sin filtro de emprendimiento. */
    public List<FilaGasto> listar() throws SQLException { return listar(0); }

    // =========================================================
    // OBTENER UNO POR ID
    // =========================================================

    /**
     * Busca un gasto por su ID para prellenar el modal de edición.
     *
     * @param idGasto ID del registro en {@code gastos}.
     * @return objeto {@link FilaGasto} con todos sus campos.
     * @throws SQLException si el gasto no existe o hay error de BD.
     */
    public FilaGasto obtenerPorId(int idGasto) throws SQLException {
        String sql =
            "SELECT g.id, g.id_detalle_compra, dc.id_compra, " +
            "DATE_FORMAT(g.fecha_gasto,'%d/%m/%Y %H:%i') AS fecha, " +
            "DATE_FORMAT(g.fecha_gasto,'%Y-%m-%d') AS fecha_raw, " +
            "dc.descripcion, g.id_metodo_pago, mp.nombre AS metodo_pago, " +
            "pu.nombre_completo AS registrado_por, g.total_gasto, " +
            "COALESCE(e2.nombre, e1.nombre) AS nombre_emprendimiento, " +
            "r.nombre AS rol_registrador " +
            "FROM gastos g " +
            "JOIN detalle_compra dc ON dc.id = g.id_detalle_compra " +
            "JOIN metodo_pago mp ON mp.id = g.id_metodo_pago " +
            "JOIN usuarios u ON u.id = dc.id_usuario " +
            "JOIN perfil_usuario pu ON pu.id_usuario = u.id " +
            "JOIN roles r ON r.id = u.id_rol " +
            "LEFT JOIN emprendimientos e1 ON e1.id = u.id_emprendimiento " +
            "LEFT JOIN emprendimientos e2 ON e2.id = dc.id_emprendimiento " +
            "WHERE g.id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGasto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Gasto no encontrado: id=" + idGasto);
                return mapear(rs);
            }
        }
    }

    // =========================================================
    // MÉTODOS DE PAGO
    // =========================================================

    /**
     * Retorna los métodos de pago para el {@code <select>} del formulario.
     *
     * @return lista de pares {@code [id, nombre]}.
     * @throws SQLException si falla la consulta.
     */
    public List<String[]> listarMetodosPago() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, nombre FROM metodo_pago ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    // =========================================================
    // REGISTRAR (TRANSACCIÓN 3 TABLAS)
    // =========================================================

    /**
     * Registra un nuevo gasto en {@code compras}, {@code detalle_compra} y {@code gastos}
     * dentro de una única transacción. Si cualquier inserción falla se hace rollback completo.
     *
     * <p>{@code idEmprendimiento} es necesario cuando {@code idUsuario} pertenece al
     * SuperAdministrador (cuyo {@code usuarios.id_emprendimiento} es {@code NULL}).
     * Se persiste en {@code detalle_compra.id_emprendimiento} para que el historial
     * pueda filtrar y mostrar el emprendimiento correcto.</p>
     *
     * @param idUsuario        ID del usuario que registra el gasto.
     * @param descripcion      descripción de la compra.
     * @param total            monto del gasto.
     * @param idMetodoPago     FK a {@code metodo_pago}.
     * @param fechaGasto       datetime en formato {@code yyyy-MM-dd HH:mm:ss}.
     * @param idEmprendimiento emprendimiento destino; {@code 0} si no aplica.
     * @throws SQLException si falla alguna inserción o la transacción no puede completarse.
     */
    public void registrar(int idUsuario, String descripcion,
                          BigDecimal total, int idMetodoPago,
                          String fechaGasto, int idEmprendimiento) throws SQLException {
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
                        "INSERT INTO detalle_compra (id_usuario, descripcion, id_compra, id_emprendimiento) " +
                        "VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idUsuario);
                    ps.setString(2, descripcion);
                    ps.setInt(3, idCompra);
                    if (idEmprendimiento > 0) ps.setInt(4, idEmprendimiento);
                    else                      ps.setNull(4, Types.INTEGER);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); idDetalle = rs.getInt(1); }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO gastos (id_detalle_compra, id_metodo_pago, fecha_gasto, total_gasto) " +
                        "VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, idDetalle);
                    ps.setInt(2, idMetodoPago);
                    ps.setString(3, fechaGasto);
                    ps.setBigDecimal(4, total);
                    ps.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // =========================================================
    // EDITAR (TRANSACCIÓN 3 TABLAS)
    // =========================================================

    /**
     * Actualiza un gasto existente en las tres tablas dentro de una transacción.
     *
     * <p>Si {@code idNuevoUsuario} es mayor que cero, también reasigna el
     * {@code id_usuario} en {@code detalle_compra} (solo permitido cuando el
     * gasto fue registrado originalmente por el SuperAdministrador).</p>
     *
     * @param idGasto          ID del registro en {@code gastos}.
     * @param idDetalleCompra  ID del registro en {@code detalle_compra}.
     * @param idCompra         ID del registro en {@code compras}.
     * @param descripcion      nueva descripción.
     * @param total            nuevo monto.
     * @param idMetodoPago     nuevo método de pago.
     * @param fechaGasto       nueva fecha en formato {@code yyyy-MM-dd HH:mm:ss}.
     * @param idEmprendimiento emprendimiento al que pertenece el gasto; {@code 0} para limpiar.
     * @throws SQLException si falla alguna actualización o la transacción no puede completarse.
     */
    public void editar(int idGasto, int idDetalleCompra, int idCompra,
                       String descripcion, BigDecimal total,
                       int idMetodoPago, String fechaGasto,
                       int idEmprendimiento) throws SQLException {
        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE compras SET fecha_compra=?, total_compra=? WHERE id=?")) {
                    ps.setString(1, fechaGasto);
                    ps.setBigDecimal(2, total);
                    ps.setInt(3, idCompra);
                    ps.executeUpdate();
                }

                String sqlDC = idEmprendimiento > 0
                    ? "UPDATE detalle_compra SET descripcion=?, id_emprendimiento=? WHERE id=?"
                    : "UPDATE detalle_compra SET descripcion=?, id_emprendimiento=NULL WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(sqlDC)) {
                    ps.setString(1, descripcion);
                    if (idEmprendimiento > 0) { ps.setInt(2, idEmprendimiento); ps.setInt(3, idDetalleCompra); }
                    else                      { ps.setInt(2, idDetalleCompra); }
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE gastos SET id_metodo_pago=?, fecha_gasto=?, total_gasto=? WHERE id=?")) {
                    ps.setInt(1, idMetodoPago);
                    ps.setString(2, fechaGasto);
                    ps.setBigDecimal(3, total);
                    ps.setInt(4, idGasto);
                    ps.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Edita un gasto sin cambiar el emprendimiento asignado. */
    public void editar(int idGasto, int idDetalleCompra, int idCompra,
                       String descripcion, BigDecimal total,
                       int idMetodoPago, String fechaGasto) throws SQLException {
        editar(idGasto, idDetalleCompra, idCompra, descripcion, total, idMetodoPago, fechaGasto, 0);
    }

    // =========================================================
    // HELPER
    // =========================================================

    /**
     * Convierte una fila del {@link ResultSet} en un objeto {@link FilaGasto}.
     * Reutilizado por {@link #listar} y {@link #obtenerPorId}.
     */
    private FilaGasto mapear(ResultSet rs) throws SQLException {
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
        try { f.nombreEmprendimiento    = rs.getString("nombre_emprendimiento"); } catch (Exception ignored) {}
        try { f.registradoPorSuperAdmin = "SuperAdministrador".equals(rs.getString("rol_registrador")); } catch (Exception ignored) {}
        return f;
    }
}
