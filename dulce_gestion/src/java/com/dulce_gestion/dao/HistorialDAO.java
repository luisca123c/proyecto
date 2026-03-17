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
        public String     nombreEmprendimiento;
    }

    public static class ItemVenta {
        public String     nombreProducto;
        public int        cantidad;
        public BigDecimal precioUnitario;
        public BigDecimal subtotal;
    }

    /**
     * Lista ventas según rol e idEmprendimiento.
     *   SuperAdmin + idEmp=0 → todas las ventas (todos los emprendimientos)
     *   SuperAdmin + idEmp>0 → solo ventas de ese emprendimiento
     *   Admin/Empleado       → solo ventas de su propio emprendimiento
     */
    public List<FilaVenta> listar(int idUsuario, boolean esAdminOSuper,
                                  int idEmprendimiento) throws SQLException {
        String sql;
        boolean filtrarEmp = false;

        if (esAdminOSuper && idEmprendimiento == 0) {
            // SuperAdmin viendo todo
            sql = "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
                  "mp.nombre AS metodo_pago, " +
                  "COALESCE(pu.nombre_completo, 'Desconocido') AS realizada_por, " +
                  "v.total_venta, e.nombre AS nombre_emprendimiento " +
                  "FROM ventas v " +
                  "JOIN metodo_pago mp   ON mp.id = v.id_metodo_pago " +
                  "JOIN carrito c        ON c.id  = v.id_carrito " +
                  "JOIN usuarios u       ON u.id  = c.id_usuario " +
                  "JOIN emprendimientos e ON e.id = u.id_emprendimiento " +
                  "LEFT JOIN perfil_usuario pu ON pu.id_usuario = c.id_usuario " +
                  "ORDER BY v.fecha_venta DESC";
        } else if (esAdminOSuper) {
            // SuperAdmin filtrando por emprendimiento
            sql = "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
                  "mp.nombre AS metodo_pago, " +
                  "COALESCE(pu.nombre_completo, 'Desconocido') AS realizada_por, " +
                  "v.total_venta, e.nombre AS nombre_emprendimiento " +
                  "FROM ventas v " +
                  "JOIN metodo_pago mp   ON mp.id = v.id_metodo_pago " +
                  "JOIN carrito c        ON c.id  = v.id_carrito " +
                  "JOIN usuarios u       ON u.id  = c.id_usuario " +
                  "JOIN emprendimientos e ON e.id = u.id_emprendimiento " +
                  "LEFT JOIN perfil_usuario pu ON pu.id_usuario = c.id_usuario " +
                  "WHERE u.id_emprendimiento = ? " +
                  "ORDER BY v.fecha_venta DESC";
            filtrarEmp = true;
        } else {
            // Admin / Empleado: solo sus propias ventas (por id_usuario del carrito)
            sql = "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
                  "mp.nombre AS metodo_pago, " +
                  "NULL AS realizada_por, " +
                  "v.total_venta, NULL AS nombre_emprendimiento " +
                  "FROM ventas v " +
                  "JOIN metodo_pago mp ON mp.id = v.id_metodo_pago " +
                  "JOIN carrito c      ON c.id  = v.id_carrito " +
                  "WHERE c.id_usuario = ? " +
                  "ORDER BY v.fecha_venta DESC";
            filtrarEmp = true;
        }

        List<FilaVenta> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filtrarEmp) {
                ps.setInt(1, esAdminOSuper ? idEmprendimiento : idUsuario);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilaVenta fv = new FilaVenta();
                    fv.id                = rs.getInt("id");
                    fv.fecha             = rs.getString("fecha");
                    fv.metodoPago        = rs.getString("metodo_pago");
                    fv.realizadaPor      = rs.getString("realizada_por");
                    fv.total             = rs.getBigDecimal("total_venta");
                    fv.nombreEmprendimiento = rs.getString("nombre_emprendimiento");
                    lista.add(fv);
                }
            }
        }
        return lista;
    }

    /** Compatibilidad retroactiva: muestra todas las ventas sin filtro de emprendimiento. */
    public List<FilaVenta> listar(int idUsuario, boolean esAdminOSuper) throws SQLException {
        return listar(idUsuario, esAdminOSuper, 0);
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
