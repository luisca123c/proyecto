package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el módulo de Compras de Insumos.
 *
 * <p>Tabla principal: {@code compras_insumos}. El emprendimiento se resuelve con
 * {@code COALESCE(e2.nombre, e1.nombre)}: si el SuperAdministrador asignó un
 * emprendimiento explícito al registrar, ese tiene precedencia; de lo contrario
 * se usa el del usuario registrador.</p>
 *
 * <p>Tablas escritas: {@code compras_insumos}.<br>
 * Tablas leídas: {@code metodo_pago}, {@code usuarios}, {@code perfil_usuario},
 * {@code emprendimientos}, {@code roles}.</p>
 */
public class ComprasDAO {

    // =========================================================
    // MODELO INTERNO
    // =========================================================

    /**
     * Proyección plana de una compra de insumos para la vista del historial.
     */
    public static class FilaCompra {
        public int        id;
        /** Fecha formateada {@code dd/MM/yyyy HH:mm} para mostrar en tabla. */
        public String     fecha;
        /** Fecha en formato {@code yyyy-MM-dd} para usar en {@code <input type="date">}. */
        public String     fechaRaw;
        public String     descripcion;
        public String     metodoPago;
        public int        idMetodoPago;
        public String     registradoPor;
        public BigDecimal total;
        public String     nombreEmprendimiento;
        /** {@code true} cuando quien registró la compra tiene rol SuperAdministrador. */
        public boolean    registradoPorSuperAdmin;
    }

    private static final String SELECT_BASE =
        "SELECT ci.id, " +
        "DATE_FORMAT(ci.fecha_compra,'%d/%m/%Y %H:%i') AS fecha, " +
        "DATE_FORMAT(ci.fecha_compra,'%Y-%m-%d') AS fecha_raw, " +
        "ci.descripcion, mp.nombre AS metodo_pago, ci.id_metodo_pago, " +
        "pu.nombre_completo AS registrado_por, ci.total, " +
        "COALESCE(e2.nombre, e1.nombre) AS nombre_emprendimiento, " +
        "r.nombre AS rol_registrador " +
        "FROM compras_insumos ci " +
        "JOIN metodo_pago mp    ON mp.id  = ci.id_metodo_pago " +
        "JOIN usuarios u        ON u.id   = ci.id_usuario " +
        "JOIN perfil_usuario pu ON pu.id_usuario = u.id " +
        "JOIN roles r           ON r.id   = u.id_rol " +
        "LEFT JOIN emprendimientos e1 ON e1.id = u.id_emprendimiento " +
        "LEFT JOIN emprendimientos e2 ON e2.id = ci.id_emprendimiento ";

    // =========================================================
    // LISTAR
    // =========================================================

    /**
     * Retorna el historial de compras ordenado del más reciente al más antiguo.
     *
     * @param idEmprendimiento filtro de emprendimiento; {@code 0} retorna todos.
     * @return lista de compras según el filtro aplicado.
     * @throws SQLException si falla la consulta.
     */
    public List<FilaCompra> listar(int idEmprendimiento) throws SQLException {
        boolean filtrar = idEmprendimiento > 0;
        String sql = SELECT_BASE +
            (filtrar ? "WHERE COALESCE(ci.id_emprendimiento, u.id_emprendimiento) = ? " : "") +
            "ORDER BY ci.fecha_compra DESC";
        List<FilaCompra> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filtrar) ps.setInt(1, idEmprendimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Lista todas las compras sin filtro de emprendimiento. */
    public List<FilaCompra> listar() throws SQLException { return listar(0); }

    // =========================================================
    // OBTENER UNO POR ID
    // =========================================================

    /**
     * Busca una compra por su ID para prellenar el modal de edición.
     *
     * @param id ID del registro en {@code compras_insumos}.
     * @return objeto {@link FilaCompra} con todos sus campos, o {@code null} si no existe.
     * @throws SQLException si falla la consulta.
     */
    public FilaCompra obtenerPorId(int id) throws SQLException {
        String sql = SELECT_BASE + "WHERE ci.id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
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
                     "SELECT id, nombre FROM metodo_pago ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    // =========================================================
    // REGISTRAR
    // =========================================================

    /**
     * Registra una nueva compra de insumos en {@code compras_insumos}.
     *
     * @param idUsuario        ID del usuario que registra la compra.
     * @param descripcion      descripción de qué se compró.
     * @param total            monto de la compra.
     * @param idMetodoPago     FK a {@code metodo_pago}.
     * @param fechaDatetime    datetime en formato {@code yyyy-MM-dd HH:mm:ss}.
     * @param idEmprendimiento emprendimiento destino; {@code 0} si no aplica.
     * @throws SQLException si falla la inserción.
     */
    public void registrar(int idUsuario, String descripcion,
                          BigDecimal total, int idMetodoPago,
                          String fechaDatetime, int idEmprendimiento) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO compras_insumos " +
                     "(id_usuario, descripcion, total, id_metodo_pago, fecha_compra, id_emprendimiento) " +
                     "VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, idUsuario);
            ps.setString(2, descripcion);
            ps.setBigDecimal(3, total);
            ps.setInt(4, idMetodoPago);
            ps.setString(5, fechaDatetime);
            if (idEmprendimiento > 0) ps.setInt(6, idEmprendimiento);
            else                      ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    // =========================================================
    // EDITAR
    // =========================================================

    /**
     * Actualiza una compra de insumos existente en {@code compras_insumos}.
     *
     * <p>Si {@code idEmprendimiento} es mayor que cero, también actualiza el
     * emprendimiento explícito (solo permitido cuando la compra fue registrada
     * originalmente por el SuperAdministrador).</p>
     *
     * @param id               ID del registro en {@code compras_insumos}.
     * @param descripcion      nueva descripción.
     * @param total            nuevo monto.
     * @param idMetodoPago     nuevo método de pago.
     * @param fechaDatetime    nueva fecha en formato {@code yyyy-MM-dd HH:mm:ss}.
     * @param idEmprendimiento nuevo emprendimiento explícito; {@code 0} para limpiar.
     * @throws SQLException si falla la actualización.
     */
    public void editar(int id, String descripcion,
                       BigDecimal total, int idMetodoPago,
                       String fechaDatetime, int idEmprendimiento) throws SQLException {
        String sql = idEmprendimiento > 0
            ? "UPDATE compras_insumos SET descripcion=?, total=?, id_metodo_pago=?, fecha_compra=?, id_emprendimiento=? WHERE id=?"
            : "UPDATE compras_insumos SET descripcion=?, total=?, id_metodo_pago=?, fecha_compra=?, id_emprendimiento=NULL WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, descripcion);
            ps.setBigDecimal(2, total);
            ps.setInt(3, idMetodoPago);
            ps.setString(4, fechaDatetime);
            if (idEmprendimiento > 0) { ps.setInt(5, idEmprendimiento); ps.setInt(6, id); }
            else                      { ps.setInt(5, id); }
            ps.executeUpdate();
        }
    }

    // =========================================================
    // HELPER
    // =========================================================

    private FilaCompra mapear(ResultSet rs) throws SQLException {
        FilaCompra fc = new FilaCompra();
        fc.id           = rs.getInt("id");
        fc.fecha        = rs.getString("fecha");
        fc.fechaRaw     = rs.getString("fecha_raw");
        fc.descripcion  = rs.getString("descripcion");
        fc.metodoPago   = rs.getString("metodo_pago");
        fc.idMetodoPago = rs.getInt("id_metodo_pago");
        fc.registradoPor = rs.getString("registrado_por");
        fc.total        = rs.getBigDecimal("total");
        try { fc.nombreEmprendimiento    = rs.getString("nombre_emprendimiento"); } catch (Exception ignored) {}
        try { fc.registradoPorSuperAdmin = "SuperAdministrador".equals(rs.getString("rol_registrador")); } catch (Exception ignored) {}
        return fc;
    }
}
