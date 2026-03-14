package com.dulce_gestion.dao;

import com.dulce_gestion.models.CompraInsumo;
import com.dulce_gestion.models.DetalleCompraInsumo;
import com.dulce_gestion.models.Insumo;
import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el módulo de Compras de Insumos.
 * Maneja: catálogo de insumos, registro de compras y sus detalles.
 */
public class InsumosDAO {

    // ═══════════════════════════════════════════════════
    //  CATÁLOGO DE INSUMOS
    // ═══════════════════════════════════════════════════

    /** Lista todos los insumos activos con su unidad de medida */
    public List<Insumo> listarInsumos() throws SQLException {
        String sql = """
            SELECT i.id, i.nombre, i.id_unidad, u.nombre AS unidad,
                   i.descripcion, i.estado
            FROM insumos i
            JOIN unidad_medida u ON u.id = i.id_unidad
            ORDER BY i.nombre
            """;
        List<Insumo> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Insumo ins = new Insumo();
                ins.setId(rs.getInt("id"));
                ins.setNombre(rs.getString("nombre"));
                ins.setIdUnidad(rs.getInt("id_unidad"));
                ins.setUnidad(rs.getString("unidad"));
                ins.setDescripcion(rs.getString("descripcion"));
                ins.setEstado(rs.getString("estado"));
                lista.add(ins);
            }
        }
        return lista;
    }

    /** Lista solo insumos activos (para el selector de nueva compra) */
    public List<Insumo> listarInsumosActivos() throws SQLException {
        String sql = """
            SELECT i.id, i.nombre, i.id_unidad, u.nombre AS unidad,
                   i.descripcion, i.estado
            FROM insumos i
            JOIN unidad_medida u ON u.id = i.id_unidad
            WHERE i.estado = 'Activo'
            ORDER BY i.nombre
            """;
        List<Insumo> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Insumo ins = new Insumo();
                ins.setId(rs.getInt("id"));
                ins.setNombre(rs.getString("nombre"));
                ins.setIdUnidad(rs.getInt("id_unidad"));
                ins.setUnidad(rs.getString("unidad"));
                ins.setDescripcion(rs.getString("descripcion"));
                ins.setEstado(rs.getString("estado"));
                lista.add(ins);
            }
        }
        return lista;
    }

    /** Busca un insumo por ID */
    public Insumo buscarInsumo(int id) throws SQLException {
        String sql = """
            SELECT i.id, i.nombre, i.id_unidad, u.nombre AS unidad,
                   i.descripcion, i.estado
            FROM insumos i
            JOIN unidad_medida u ON u.id = i.id_unidad
            WHERE i.id = ?
            """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Insumo ins = new Insumo();
                    ins.setId(rs.getInt("id"));
                    ins.setNombre(rs.getString("nombre"));
                    ins.setIdUnidad(rs.getInt("id_unidad"));
                    ins.setUnidad(rs.getString("unidad"));
                    ins.setDescripcion(rs.getString("descripcion"));
                    ins.setEstado(rs.getString("estado"));
                    return ins;
                }
            }
        }
        return null;
    }

    /** Crea un nuevo insumo */
    public void crearInsumo(String nombre, int idUnidad, String descripcion) throws SQLException {
        String sql = "INSERT INTO insumos (nombre, id_unidad, descripcion, estado) VALUES (?,?,?,'Activo')";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, idUnidad);
            ps.setString(3, descripcion != null ? descripcion.trim() : null);
            ps.executeUpdate();
        }
    }

    /** Actualiza un insumo existente */
    public void editarInsumo(int id, String nombre, int idUnidad, String descripcion, String estado) throws SQLException {
        String sql = "UPDATE insumos SET nombre=?, id_unidad=?, descripcion=?, estado=? WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, idUnidad);
            ps.setString(3, descripcion != null ? descripcion.trim() : null);
            ps.setString(4, estado);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    /** Elimina un insumo (falla si tiene compras asociadas por RESTRICT) */
    public void eliminarInsumo(int id) throws SQLException {
        String sql = "DELETE FROM insumos WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════
    //  COMPRAS DE INSUMOS
    // ═══════════════════════════════════════════════════

    /** Lista todas las compras con encabezado (sin detalles) */
    public List<CompraInsumo> listarCompras() throws SQLException {
        String sql = """
            SELECT c.id, c.id_usuario, p.nombre_completo AS nombre_usuario,
                   DATE_FORMAT(c.fecha_compra,'%d/%m/%Y %H:%i') AS fecha_compra,
                   c.total, c.id_metodo_pago, m.nombre AS metodo_pago
            FROM compra_insumos c
            JOIN perfil_usuario p ON p.id_usuario = c.id_usuario
            JOIN metodo_pago    m ON m.id = c.id_metodo_pago
            ORDER BY c.fecha_compra DESC
            """;
        List<CompraInsumo> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CompraInsumo c = new CompraInsumo();
                c.setId(rs.getInt("id"));
                c.setIdUsuario(rs.getInt("id_usuario"));
                c.setNombreUsuario(rs.getString("nombre_usuario"));
                c.setFechaCompra(rs.getString("fecha_compra"));
                c.setTotal(rs.getBigDecimal("total"));
                c.setIdMetodoPago(rs.getInt("id_metodo_pago"));
                c.setMetodoPago(rs.getString("metodo_pago"));
                lista.add(c);
            }
        }
        return lista;
    }

    /** Obtiene los ítems de una compra */
    public List<DetalleCompraInsumo> listarDetalle(int idCompra) throws SQLException {
        String sql = """
            SELECT d.id, d.id_compra, d.id_insumo, i.nombre AS nombre_insumo,
                   u.nombre AS unidad, d.cantidad, d.precio_unitario, d.subtotal
            FROM detalle_compra_insumos d
            JOIN insumos       i ON i.id = d.id_insumo
            JOIN unidad_medida u ON u.id = i.id_unidad
            WHERE d.id_compra = ?
            """;
        List<DetalleCompraInsumo> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleCompraInsumo d = new DetalleCompraInsumo();
                    d.setId(rs.getInt("id"));
                    d.setIdCompra(rs.getInt("id_compra"));
                    d.setIdInsumo(rs.getInt("id_insumo"));
                    d.setNombreInsumo(rs.getString("nombre_insumo"));
                    d.setUnidad(rs.getString("unidad"));
                    d.setCantidad(rs.getBigDecimal("cantidad"));
                    d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    d.setSubtotal(rs.getBigDecimal("subtotal"));
                    lista.add(d);
                }
            }
        }
        return lista;
    }

    /**
     * Registra una compra completa en transacción:
     *   1. INSERT compra_insumos  → obtiene el ID generado
     *   2. INSERT detalle_compra_insumos por cada ítem
     * Si algo falla → rollback total.
     *
     * @param idUsuario     quién realizó la compra
     * @param idMetodoPago  método de pago usado
     * @param fecha         datetime completo "yyyy-MM-dd HH:mm:ss"
     * @param idsInsumo     array de IDs de insumos
     * @param cantidades    array de cantidades (mismo orden)
     * @param precios       array de precios unitarios (mismo orden)
     * @return ID de la compra creada
     */
    public int registrarCompra(int idUsuario, int idMetodoPago, String fecha,
                                int[] idsInsumo, BigDecimal[] cantidades, BigDecimal[] precios)
            throws SQLException {

        // Calcular total sumando subtotales
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < idsInsumo.length; i++) {
            total = total.add(cantidades[i].multiply(precios[i]));
        }

        String sqlCompra  = "INSERT INTO compra_insumos (id_usuario, fecha_compra, total, id_metodo_pago) VALUES (?,?,?,?)";
        String sqlDetalle = "INSERT INTO detalle_compra_insumos (id_compra, id_insumo, cantidad, precio_unitario) VALUES (?,?,?,?)";

        Connection con = DB.obtenerConexion();
        try {
            con.setAutoCommit(false);

            int idCompra;
            try (PreparedStatement ps = con.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idUsuario);
                ps.setString(2, fecha);
                ps.setBigDecimal(3, total);
                ps.setInt(4, idMetodoPago);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    idCompra = rs.getInt(1);
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                for (int i = 0; i < idsInsumo.length; i++) {
                    ps.setInt(1, idCompra);
                    ps.setInt(2, idsInsumo[i]);
                    ps.setBigDecimal(3, cantidades[i]);
                    ps.setBigDecimal(4, precios[i]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            return idCompra;

        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
            con.close();
        }
    }

    /** Elimina una compra (CASCADE elimina sus detalles) */
    public void eliminarCompra(int id) throws SQLException {
        String sql = "DELETE FROM compra_insumos WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Lista los métodos de pago disponibles */
    public List<String[]> listarMetodosPago() throws SQLException {
        String sql = "SELECT id, nombre FROM metodo_pago ORDER BY id";
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new String[]{ rs.getString("id"), rs.getString("nombre") });
            }
        }
        return lista;
    }

    /** Lista las unidades de medida disponibles */
    public List<String[]> listarUnidades() throws SQLException {
        String sql = "SELECT id, nombre FROM unidad_medida ORDER BY id";
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new String[]{ rs.getString("id"), rs.getString("nombre") });
            }
        }
        return lista;
    }
}
