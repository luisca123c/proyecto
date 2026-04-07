package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO especializado en el historial de ventas confirmadas.
 *
 * Esta clase maneja el listado de ventas con diferentes niveles de acceso
 * según el rol del usuario y filtros por emprendimiento.
 *
 * Tablas leídas: ventas, metodo_pago, carrito, usuarios, roles,
 * perfil_usuario, emprendimientos, detalle_carrito, productos
 *
 * Características importantes:
 * - Control de acceso por rol (SuperAdmin, Admin, Empleado)
 * - Filtros por emprendimiento con COALESCE
 * - Listado de items detallados por venta
 * - Identificación de ventas realizadas por SuperAdmin
 * - Formateo de fechas para presentación
 *
 * Reglas de visibilidad:
 * - SuperAdmin + idEmp=0 → todas las ventas (todos los emprendimientos)
 * - SuperAdmin + idEmp>0 → solo ventas de ese emprendimiento
 * - Admin/Empleado → solo ventas de su propio emprendimiento
 */
public class HistorialDAO {

    // =========================================================
    // MODELOS INTERNOS - DTOs para vistas de historial
    // =========================================================

    /**
     * DTO que representa una venta para mostrar en la tabla del historial.
     *
     * Contiene los datos esenciales de una venta con información
     * del vendedor, emprendimiento e indicador si fue realizada
     * por un SuperAdministrador.
     */
    public static class FilaVenta {
        public int        id;                    // ID único de la venta
        public String     fecha;                 // Fecha formateada dd/MM/yyyy HH:mm
        public String     metodoPago;            // Método de pago utilizado
        public String     realizadaPor;          // Nombre del usuario que vendió
        public boolean    realizadaPorSuperAdmin; // true si fue realizada por SuperAdmin
        public BigDecimal total;                 // Monto total de la venta
        public String     nombreEmprendimiento;  // Nombre del emprendimiento
    }

    /**
     * DTO que representa un item individual de una venta.
     *
     * Contiene los detalles de un producto vendido incluyendo
     * cantidad, precio unitario y subtotal calculado.
     */
    public static class ItemVenta {
        public String     nombreProducto;        // Nombre del producto
        public int        cantidad;              // Cantidad vendida
        public BigDecimal precioUnitario;        // Precio unitario del producto
        public BigDecimal subtotal;              // Subtotal (cantidad * precio unitario)
    }

    // =========================================================
    // LISTADO DE VENTAS - Con filtros por rol y emprendimiento
    // =========================================================
    /**
     * Lista ventas según rol e idEmprendimiento con reglas de visibilidad.
     *
     * Implementa tres escenarios diferentes según el rol y filtros:
     * 1. SuperAdmin sin filtro → todas las ventas con datos completos
     * 2. SuperAdmin con filtro → ventas de emprendimiento específico
     * 3. Admin/Empleado → solo sus ventas (sin datos de otros vendedores)
     *
     * @param idUsuario        ID del usuario en sesión
     * @param esAdminOSuper    true si es Admin o SuperAdmin
     * @param idEmprendimiento filtro de emprendimiento; 0 = sin filtro
     * @return lista de ventas según reglas de visibilidad
     * @throws SQLException si hay error en las consultas
     */
    public List<FilaVenta> listar(int idUsuario, boolean esAdminOSuper, int idEmprendimiento) throws SQLException {
        String sql;
        boolean filtrarEmp = false;

        if (esAdminOSuper && idEmprendimiento == 0) {
            // SuperAdmin viendo todas las ventas sin filtro
            // SQL para obtener todas las ventas con datos completos del vendedor y emprendimiento
            sql = "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
                  "mp.nombre AS metodo_pago, " +
                  "COALESCE(pu.nombre_completo, 'Desconocido') AS realizada_por, " +
                  "r.nombre AS rol_realizador, " +
                  "v.total_venta, e.nombre AS nombre_emprendimiento " +
                  "FROM ventas v " +
                  "JOIN metodo_pago mp   ON mp.id = v.id_metodo_pago " +
                  "JOIN carrito c        ON c.id  = v.id_carrito " +
                  "JOIN usuarios u       ON u.id  = c.id_usuario " +
                  "LEFT JOIN emprendimientos e ON e.id = COALESCE(v.id_emprendimiento, u.id_emprendimiento) " +
                  "JOIN roles r ON r.id = u.id_rol " +
                  "LEFT JOIN perfil_usuario pu ON pu.id_usuario = c.id_usuario " +
                  "ORDER BY v.fecha_venta DESC";
        } else if (esAdminOSuper) {
            // SuperAdmin filtrando por emprendimiento específico
            // SQL similar al anterior pero con WHERE para filtrar por emprendimiento
            sql = "SELECT v.id, DATE_FORMAT(v.fecha_venta,'%d/%m/%Y %H:%i') AS fecha, " +
                  "mp.nombre AS metodo_pago, " +
                  "COALESCE(pu.nombre_completo, 'Desconocido') AS realizada_por, " +
                  "r.nombre AS rol_realizador, " +
                  "v.total_venta, e.nombre AS nombre_emprendimiento " +
                  "FROM ventas v " +
                  "JOIN metodo_pago mp   ON mp.id = v.id_metodo_pago " +
                  "JOIN carrito c        ON c.id  = v.id_carrito " +
                  "JOIN usuarios u       ON u.id  = c.id_usuario " +
                  "LEFT JOIN emprendimientos e ON e.id = COALESCE(v.id_emprendimiento, u.id_emprendimiento) " +
                  "JOIN roles r ON r.id = u.id_rol " +
                  "LEFT JOIN perfil_usuario pu ON pu.id_usuario = c.id_usuario " +
                  "WHERE COALESCE(v.id_emprendimiento, u.id_emprendimiento) = ? " +
                  "ORDER BY v.fecha_venta DESC";
            filtrarEmp = true;
        } else {
            // Admin / Empleado: solo sus propias ventas (por id_usuario del carrito)
            // SQL simplificado que solo muestra datos esenciales sin información de otros vendedores
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

            // Asignar parámetros según el tipo de filtro
            if (filtrarEmp) {
                ps.setInt(1, esAdminOSuper ? idEmprendimiento : idUsuario); // Emprendimiento o usuario según rol
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Mapear cada venta a DTO con todos sus campos
                    FilaVenta fv = new FilaVenta();
                    fv.id                = rs.getInt("id");
                    fv.fecha             = rs.getString("fecha");
                    fv.metodoPago        = rs.getString("metodo_pago");
                    fv.realizadaPor      = rs.getString("realizada_por");
                    // Determinar si fue realizada por SuperAdmin (manejo seguro de nulos)
                    try { fv.realizadaPorSuperAdmin = "SuperAdministrador".equals(rs.getString("rol_realizador")); } catch (Exception ignored) {}
                    fv.total             = rs.getBigDecimal("total_venta");
                    fv.nombreEmprendimiento = rs.getString("nombre_emprendimiento");
                    lista.add(fv);
                }
            }
        }
        return lista;
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
     * @param idUsuario     ID del usuario en sesión
     * @param esAdminOSuper true si es Administrador o SuperAdministrador
     * @return lista de ventas sin filtro de emprendimiento
     * @throws SQLException si hay error en las consultas
     */
    public List<FilaVenta> listar(int idUsuario, boolean esAdminOSuper) throws SQLException {
        return listar(idUsuario, esAdminOSuper, 0);
    }

    // =========================================================
    // ITEMS DE VENTA - Detalles de productos por venta
    // =========================================================
    /**
     * Lista los items detallados de una venta específica.
     *
     * Para Admin/Empleado, verifica que la venta pertenezca al usuario
     * antes de mostrar los detalles. SuperAdmin puede ver cualquier venta.
     *
     * @param idVenta        ID de la venta a consultar
     * @param idUsuario      ID del usuario en sesión (para validación)
     * @param esAdminOSuper  true si es Admin o SuperAdmin
     * @return lista de items de la venta con detalles de productos
     * @throws SQLException si hay error en las consultas
     */
    public List<ItemVenta> listarItems(int idVenta, int idUsuario,
                                       boolean esAdminOSuper) throws SQLException {
        // Para Admin/Empleado: validar que la venta pertenezca al usuario
        if (!esAdminOSuper) {
            String check = "SELECT v.id FROM ventas v " +
                           "JOIN carrito c ON c.id = v.id_carrito " +
                           "WHERE v.id = ? AND c.id_usuario = ?";
            try (Connection con = DB.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(check)) {
                ps.setInt(1, idVenta);
                ps.setInt(2, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return List.of(); // Retorna lista vacía si no pertenece al usuario
                }
            }
        }

        // SQL para obtener items de la venta con cálculo de subtotal
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
                    // Mapear cada item a DTO con cálculos de subtotal
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
