package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * DAO: GastosDAO
 * Tablas escritas:    compras, detalle_compra, gastos
 * Tablas leídas:      metodo_pago, usuarios, perfil_usuario
 * Usado por:          GastosServlet
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Gestiona el CRUD completo de gastos. Lista, busca uno por ID,
 * registra nuevo y edita existente, todo usando la estructura de 3 tablas
 * que modela un gasto en esta BD.
 *
 * ¿POR QUÉ UN GASTO SE ALMACENA EN 3 TABLAS?
 * --------------------------------------------
 * La BD normaliza los gastos para soportar futuras extensiones
 * (ej: un gasto con múltiples ítems en detalle_compra):
 *
 *   compras         → encabezado: fecha y total de la compra
 *   detalle_compra  → detalle: descripción, quién lo registró (id_usuario)
 *   gastos          → registro financiero: método de pago, fecha, total
 *
 * Para operaciones simples (un gasto = un registro), las 3 tablas
 * siempre se crean o editan juntas en transacción.
 *
 * ¿POR QUÉ FilaGasto TIENE fechaRaw ADEMÁS DE fecha?
 * ----------------------------------------------------
 * El JSP de gastos muestra la fecha en formato legible ("15/03/2025 14:30")
 * y también la necesita en formato yyyy-MM-dd para el input type="date"
 * del modal de edición.
 * El formateo en SQL con DATE_FORMAT evita hacerlo en Java:
 *   fecha    → DATE_FORMAT(g.fecha_gasto, '%d/%m/%Y %H:%i') → mostrar
 *   fechaRaw → DATE_FORMAT(g.fecha_gasto, '%Y-%m-%d')       → input date
 */
public class GastosDAO {

    // =========================================================
    // MODELO INTERNO
    // =========================================================

    /**
     * Representa una fila del historial de gastos.
     * Incluye los IDs de las 3 tablas subyacentes para poder editarlas.
     */
    public static class FilaGasto {
        public int        id;               // gastos.id (clave principal para editar/eliminar)
        public int        idDetalleCompra;  // detalle_compra.id (para editar descripción)
        public int        idCompra;         // compras.id (para editar fecha y total de compras)
        public String     fecha;            // Formateada para mostrar: "15/03/2025 14:30"
        public String     fechaRaw;         // En formato yyyy-MM-dd para el input type="date"
        public String     descripcion;      // Descripción del gasto (qué se compró)
        public int        idMetodoPago;     // FK para seleccionar la opción en el <select>
        public String     metodoPago;       // Nombre del método para mostrar en la tabla
        public String     registradoPor;    // Nombre completo del usuario que lo registró
        public BigDecimal total;            // Monto del gasto
    }

    // =========================================================
    // LISTAR TODOS
    // =========================================================

    /**
     * Retorna el historial completo de gastos ordenado del más reciente al más antiguo.
     *
     * El JOIN de 5 tablas trae en una sola consulta todos los datos
     * necesarios para la tabla del historial, incluyendo el nombre
     * del método de pago y el nombre de quien registró el gasto.
     *
     * @return  lista de todos los gastos, más recientes primero
     * @throws SQLException si hay error al consultar la BD
     */
    public List<FilaGasto> listar() throws SQLException {
        String sql =
            "SELECT g.id, g.id_detalle_compra, dc.id_compra, " +
            // Dos formatos de fecha en una sola consulta: display y edición
            "DATE_FORMAT(g.fecha_gasto,'%d/%m/%Y %H:%i') AS fecha, " +
            "DATE_FORMAT(g.fecha_gasto,'%Y-%m-%d') AS fecha_raw, " +
            "dc.descripcion, g.id_metodo_pago, mp.nombre AS metodo_pago, " +
            "pu.nombre_completo AS registrado_por, g.total_gasto " +
            "FROM gastos g " +
            "JOIN detalle_compra dc ON dc.id=g.id_detalle_compra " +
            "JOIN metodo_pago mp ON mp.id=g.id_metodo_pago " +
            "JOIN usuarios u ON u.id=dc.id_usuario " +
            "JOIN perfil_usuario pu ON pu.id_usuario=u.id " +
            "ORDER BY g.fecha_gasto DESC"; // Más recientes primero

        List<FilaGasto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // =========================================================
    // OBTENER UNO POR ID
    // =========================================================

    /**
     * Busca un gasto por su ID en la tabla gastos.
     * Se usa en GastosServlet para prellenar el modal de edición.
     *
     * @param idGasto  ID del gasto a buscar
     * @return         objeto FilaGasto con todos los datos
     * @throws SQLException si el gasto no existe o hay error de BD
     */
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
                if (!rs.next()) throw new SQLException("Gasto no encontrado: id=" + idGasto);
                return mapear(rs);
            }
        }
    }

    // =========================================================
    // MÉTODOS DE PAGO
    // =========================================================

    /**
     * Retorna los métodos de pago disponibles para el <select> del formulario.
     *
     * @return  lista de [id, nombre] por método de pago
     * @throws SQLException si hay error al consultar la BD
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
    // REGISTRAR NUEVO GASTO (TRANSACCIÓN 3 TABLAS)
    // =========================================================

    /**
     * Registra un nuevo gasto insertando en 3 tablas dentro de una transacción.
     *
     * ORDEN DE INSERCIÓN (respetando las FK):
     *
     * Paso 1 — INSERT compras:
     *   Encabezado del gasto. Retorna idCompra.
     *   compras.fecha_compra = mismo valor que gastos.fecha_gasto
     *   compras.total_compra = mismo total (redundante pero exigido por el esquema)
     *
     * Paso 2 — INSERT detalle_compra:
     *   Quién registró el gasto y qué se compró. Usa idCompra del paso 1.
     *   Retorna idDetalle.
     *
     * Paso 3 — INSERT gastos:
     *   El registro financiero con el método de pago. Usa idDetalle del paso 2.
     *
     * Si cualquier paso falla → ROLLBACK → ninguna tabla queda modificada.
     *
     * @param idUsuario    ID del usuario que registra el gasto
     * @param descripcion  descripción de qué se compró
     * @param total        monto del gasto (BigDecimal para exactitud)
     * @param idMetodoPago FK a metodo_pago
     * @param fechaGasto   datetime completo en formato "yyyy-MM-dd HH:mm:ss"
     * @throws SQLException si hay error de BD
     */
    public void registrar(int idUsuario, String descripcion,
                          BigDecimal total, int idMetodoPago,
                          String fechaGasto) throws SQLException {
        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false); // Transacción: los 3 INSERTs son atómicos
            try {

                // ── Paso 1: insertar en compras ───────────────────────────
                int idCompra;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO compras (fecha_compra, total_compra) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, fechaGasto);
                    ps.setBigDecimal(2, total);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); idCompra = rs.getInt(1); }
                }

                // ── Paso 2: insertar en detalle_compra ────────────────────
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

                // ── Paso 3: insertar en gastos ────────────────────────────
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO gastos (id_detalle_compra, id_metodo_pago, fecha_gasto, total_gasto) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, idDetalle);
                    ps.setInt(2, idMetodoPago);
                    ps.setString(3, fechaGasto);
                    ps.setBigDecimal(4, total);
                    ps.executeUpdate();
                }

                con.commit(); // Todo bien → confirmar los 3 INSERTs

            } catch (SQLException e) {
                con.rollback(); // Algo falló → deshacer todo
                throw e;
            }
        }
    }

    // =========================================================
    // EDITAR GASTO (TRANSACCIÓN 3 TABLAS)
    // =========================================================

    /**
     * Actualiza un gasto existente modificando las 3 tablas dentro de una transacción.
     *
     * ORDEN DE ACTUALIZACIÓN:
     *
     * Paso 1 — UPDATE compras:
     *   Actualiza fecha y total de la tabla de encabezado.
     *
     * Paso 2 — UPDATE detalle_compra:
     *   Actualiza la descripción del detalle.
     *
     * Paso 3 — UPDATE gastos:
     *   Actualiza el método de pago, la fecha y el total del registro financiero.
     *
     * ¿POR QUÉ SE PASAN idDetalleCompra e idCompra COMO PARÁMETROS?
     * ---------------------------------------------------------------
     * GastosServlet los recibe del formulario (campos ocultos que el JSP
     * incluye al cargar el modal de edición). Se pasan explícitamente para
     * que este método no tenga que hacer una consulta extra para obtenerlos.
     *
     * @param idGasto         gastos.id  → identifica el registro financiero a editar
     * @param idDetalleCompra detalle_compra.id  → para actualizar la descripción
     * @param idCompra        compras.id  → para actualizar fecha y total de compras
     * @param descripcion     nueva descripción
     * @param total           nuevo monto
     * @param idMetodoPago    nuevo método de pago (FK)
     * @param fechaGasto      nueva fecha en formato "yyyy-MM-dd HH:mm:ss"
     * @throws SQLException   si hay error de BD
     */
    public void editar(int idGasto, int idDetalleCompra, int idCompra,
                       String descripcion, BigDecimal total,
                       int idMetodoPago, String fechaGasto) throws SQLException {
        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false); // Transacción: los 3 UPDATEs son atómicos
            try {

                // ── Paso 1: actualizar compras ────────────────────────────
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE compras SET fecha_compra=?, total_compra=? WHERE id=?")) {
                    ps.setString(1, fechaGasto);
                    ps.setBigDecimal(2, total);
                    ps.setInt(3, idCompra);
                    ps.executeUpdate();
                }

                // ── Paso 2: actualizar detalle_compra ─────────────────────
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE detalle_compra SET descripcion=? WHERE id=?")) {
                    ps.setString(1, descripcion);
                    ps.setInt(2, idDetalleCompra);
                    ps.executeUpdate();
                }

                // ── Paso 3: actualizar gastos ─────────────────────────────
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE gastos SET id_metodo_pago=?, fecha_gasto=?, total_gasto=? WHERE id=?")) {
                    ps.setInt(1, idMetodoPago);
                    ps.setString(2, fechaGasto);
                    ps.setBigDecimal(3, total);
                    ps.setInt(4, idGasto);
                    ps.executeUpdate();
                }

                con.commit(); // Todo bien → confirmar los 3 UPDATEs

            } catch (SQLException e) {
                con.rollback(); // Algo falló → deshacer todo
                throw e;
            }
        }
    }

    // =========================================================
    // HELPER PRIVADO
    // =========================================================

    /**
     * Convierte una fila del ResultSet en un objeto FilaGasto.
     * Reutilizado por listar() y obtenerPorId() para evitar duplicar el mapeo.
     */
    private FilaGasto mapear(ResultSet rs) throws SQLException {
        FilaGasto f = new FilaGasto();
        f.id              = rs.getInt("id");
        f.idDetalleCompra = rs.getInt("id_detalle_compra");
        f.idCompra        = rs.getInt("id_compra");
        f.fecha           = rs.getString("fecha");     // "15/03/2025 14:30" → mostrar en tabla
        f.fechaRaw        = rs.getString("fecha_raw"); // "2025-03-15" → para el input type="date"
        f.descripcion     = rs.getString("descripcion");
        f.idMetodoPago    = rs.getInt("id_metodo_pago");
        f.metodoPago      = rs.getString("metodo_pago");
        f.registradoPor   = rs.getString("registrado_por");
        f.total           = rs.getBigDecimal("total_gasto");
        return f;
    }
}
