package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el módulo de Gastos.
 *
 * <p>Tabla principal: {@code gastos}. El emprendimiento se resuelve con
 * {@code COALESCE(e2.nombre, e1.nombre)}: si el SuperAdministrador asignó
 * un emprendimiento explícito al registrar, ese tiene precedencia; de lo
 * contrario se usa el del usuario registrador.</p>
 *
 * <p>Tablas escritas: {@code gastos}.<br>
 * Tablas leídas: {@code metodo_pago}, {@code usuarios}, {@code perfil_usuario},
 * {@code emprendimientos}, {@code roles}.</p>
 */
public class GastosDAO {

    // =========================================================
    // MODELO INTERNO
    // =========================================================

    /**
     * Proyección plana de un gasto para la vista del historial.
     */
    public static class FilaGasto {
        public int        id;
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

    private static final String SELECT_BASE =
        "SELECT g.id, " +
        "DATE_FORMAT(g.fecha,'%d/%m/%Y %H:%i') AS fecha, " +
        "DATE_FORMAT(g.fecha,'%Y-%m-%d') AS fecha_raw, " +
        "g.descripcion, g.id_metodo_pago, mp.nombre AS metodo_pago, " +
        "pu.nombre_completo AS registrado_por, g.total, " +
        "COALESCE(e2.nombre, e1.nombre) AS nombre_emprendimiento, " +
        "r.nombre AS rol_registrador " +
        "FROM gastos g " +
        "JOIN metodo_pago mp    ON mp.id  = g.id_metodo_pago " +
        "JOIN usuarios u        ON u.id   = g.id_usuario " +
        "JOIN perfil_usuario pu ON pu.id_usuario = u.id " +
        "JOIN roles r           ON r.id   = u.id_rol " +
        "LEFT JOIN emprendimientos e1 ON e1.id = u.id_emprendimiento " +
        "LEFT JOIN emprendimientos e2 ON e2.id = g.id_emprendimiento ";

    // =========================================================
    // LISTAR
    // =========================================================

    /**
     * Retorna el historial de gastos ordenado del más reciente al más antiguo.
     *
     * @param idEmprendimiento filtro de emprendimiento; {@code 0} retorna todos.
     * @return lista de gastos según el filtro aplicado.
     * @throws SQLException si falla la consulta.
     */
    public List<FilaGasto> listar(int idEmprendimiento) throws SQLException {
        boolean filtrar = idEmprendimiento > 0;
        String sql = SELECT_BASE +
            (filtrar ? "WHERE COALESCE(g.id_emprendimiento, u.id_emprendimiento) = ? " : "") +
            "ORDER BY g.fecha DESC";
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
     * @param id ID del registro en {@code gastos}.
     * @return objeto {@link FilaGasto} con todos sus campos.
     * @throws SQLException si el gasto no existe o hay error de BD.
     */
    public FilaGasto obtenerPorId(int id) throws SQLException {
        String sql = SELECT_BASE + "WHERE g.id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Gasto no encontrado: id=" + id);
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
    // REGISTRAR
    // =========================================================

    /**
     * Registra un nuevo gasto en {@code gastos}.
     *
     * @param idUsuario        ID del usuario que registra el gasto.
     * @param descripcion      descripción del gasto.
     * @param total            monto del gasto.
     * @param idMetodoPago     FK a {@code metodo_pago}.
     * @param fecha            datetime en formato {@code yyyy-MM-dd HH:mm:ss}.
     * @param idEmprendimiento emprendimiento destino; {@code 0} si no aplica.
     * @throws SQLException si falla la inserción.
     */
    public void registrar(int idUsuario, String descripcion,
                          BigDecimal total, int idMetodoPago,
                          String fecha, int idEmprendimiento) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO gastos (id_usuario, descripcion, total, " +
                     "id_metodo_pago, fecha, id_emprendimiento) VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, idUsuario);
            ps.setString(2, descripcion);
            ps.setBigDecimal(3, total);
            ps.setInt(4, idMetodoPago);
            ps.setString(5, fecha);
            if (idEmprendimiento > 0) ps.setInt(6, idEmprendimiento);
            else                      ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    // =========================================================
    // EDITAR
    // =========================================================

    /**
     * Actualiza un gasto existente en {@code gastos}.
     *
     * <p>Si {@code idEmprendimiento} es mayor que cero, también actualiza el
     * emprendimiento explícito (solo permitido cuando el gasto fue registrado
     * originalmente por el SuperAdministrador).</p>
     *
     * @param id               ID del registro en {@code gastos}.
     * @param descripcion      nueva descripción.
     * @param total            nuevo monto.
     * @param idMetodoPago     nuevo método de pago.
     * @param fecha            nueva fecha en formato {@code yyyy-MM-dd HH:mm:ss}.
     * @param idEmprendimiento nuevo emprendimiento explícito; {@code 0} para limpiar.
     * @throws SQLException si falla la actualización.
     */
    public void editar(int id, String descripcion, BigDecimal total,
                       int idMetodoPago, String fecha,
                       int idEmprendimiento) throws SQLException {
        String sql = idEmprendimiento > 0
            ? "UPDATE gastos SET descripcion=?, total=?, id_metodo_pago=?, fecha=?, id_emprendimiento=? WHERE id=?"
            : "UPDATE gastos SET descripcion=?, total=?, id_metodo_pago=?, fecha=?, id_emprendimiento=NULL WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, descripcion);
            ps.setBigDecimal(2, total);
            ps.setInt(3, idMetodoPago);
            ps.setString(4, fecha);
            if (idEmprendimiento > 0) { ps.setInt(5, idEmprendimiento); ps.setInt(6, id); }
            else                      { ps.setInt(5, id); }
            ps.executeUpdate();
        }
    }

    // =========================================================
    // HELPER
    // =========================================================

    private FilaGasto mapear(ResultSet rs) throws SQLException {
        FilaGasto f = new FilaGasto();
        f.id           = rs.getInt("id");
        f.fecha        = rs.getString("fecha");
        f.fechaRaw     = rs.getString("fecha_raw");
        f.descripcion  = rs.getString("descripcion");
        f.idMetodoPago = rs.getInt("id_metodo_pago");
        f.metodoPago   = rs.getString("metodo_pago");
        f.registradoPor = rs.getString("registrado_por");
        f.total        = rs.getBigDecimal("total");
        try { f.nombreEmprendimiento    = rs.getString("nombre_emprendimiento"); } catch (Exception ignored) {}
        try { f.registradoPorSuperAdmin = "SuperAdministrador".equals(rs.getString("rol_registrador")); } catch (Exception ignored) {}
        return f;
    }
}
