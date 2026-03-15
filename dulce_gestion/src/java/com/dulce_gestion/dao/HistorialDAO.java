package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el historial de ventas confirmadas.
 * Empleados ven solo sus propias ventas.
 * Admin y SuperAdmin ven todas con nombre del vendedor.
 */
public class HistorialDAO {

    public static class FilaVenta {
        public int        id;
        public String     fecha;
        public String     metodoPago;
        public String     realizadaPor;
        public BigDecimal total;
    }

    public static class ItemVenta {
        public String     nombreProducto;
        public int        cantidad;
        public BigDecimal precioUnitario;
        public BigDecimal subtotal;
    }

    public List<FilaVenta> listar(int idUsuario, boolean esAdminOSuper) throws SQLException {
        String sql = esAdminOSuper
            ? "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
              "mp.nombre AS metodo_pago, " +
              "COALESCE(p.nombre_completo, 'Desconocido') AS realizada_por, " +
              "v.total_venta " +
              "FROM ventas v " +
              "JOIN metodo_pago mp ON mp.id = v.id_metodo_pago " +
              "JOIN carrito c ON c.id = v.id_carrito " +
              "LEFT JOIN perfil_usuario p ON p.id_usuario = c.id_usuario " +
              "ORDER BY v.fecha_venta DESC"
            : "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
              "mp.nombre AS metodo_pago, " +
              "NULL AS realizada_por, " +
              "v.total_venta " +
              "FROM ventas v " +
              "JOIN metodo_pago mp ON mp.id = v.id_metodo_pago " +
              "JOIN carrito c ON c.id = v.id_carrito " +
              "WHERE c.id_usuario = ? " +
              "ORDER BY v.fecha_venta DESC";

        List<FilaVenta> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (!esAdminOSuper) ps.setInt(1, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaVenta fv = new FilaVenta();
                    fv.id           = rs.getInt("id");
                    fv.fecha        = rs.getString("fecha");
                    fv.metodoPago   = rs.getString("metodo_pago");
                    fv.realizadaPor = rs.getString("realizada_por");
                    fv.total        = rs.getBigDecimal("total_venta");
                    lista.add(fv);
                }
            }
        }
        return lista;
    }

    public List<ItemVenta> listarItems(int idVenta, int idUsuario,
                                       boolean esAdminOSuper) throws SQLException {
        if (!esAdminOSuper) {
            String check = "SELECT v.id FROM ventas v " +
                           "JOIN carrito c ON c.id = v.id_carrito " +
                           "WHERE v.id = ? AND c.id_usuario = ?";
            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(check)) {
                ps.setInt(1, idVenta);
                ps.setInt(2, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return List.of();
                }
            }
        }

        String sql = "SELECT pr.nombre AS producto, " +
                     "dc.cantidad, " +
                     "pr.precio_unitario, " +
                     "(dc.cantidad * pr.precio_unitario) AS subtotal " +
                     "FROM ventas v " +
                     "JOIN carrito c ON c.id = v.id_carrito " +
                     "JOIN detalle_carrito dc ON dc.id_carrito = c.id " +
                     "JOIN productos pr ON pr.id = dc.id_producto " +
                     "WHERE v.id = ? " +
                     "ORDER BY pr.nombre";

        List<ItemVenta> items = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemVenta iv = new ItemVenta();
                    iv.nombreProducto = rs.getString("producto");
                    iv.cantidad       = rs.getInt("cantidad");
                    iv.precioUnitario = rs.getBigDecimal("precio_unitario");
                    iv.subtotal       = rs.getBigDecimal("subtotal");
                    items.add(iv);
                }
            }
        }
        return items;
    }
}
