package com.dulce_gestion.dao;

import com.dulce_gestion.models.CarritoItem;
import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * DAO para todas las operaciones del carrito:
 *  - obtener o crear el carrito activo del usuario
 *  - listar items
 *  - agregar / actualizar / eliminar item
 *  - vaciar carrito
 *  - confirmar venta (descuenta stock, registra en ventas, vacía carrito)
 */
public class CarritoDAO {

    /* ── Obtener o crear carrito activo ───────────────────── */

    public int obtenerOCrearCarrito(int idUsuario) throws SQLException {
        // Buscar carrito activo existente
        String sqlBuscar = """
                SELECT id FROM carrito
                WHERE id_usuario = ?
                  AND id_estado_carro = (SELECT id FROM estado_carrito WHERE nombre = 'Activo')
                LIMIT 1
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }

        // No existe — crear uno nuevo
        String sqlCrear = """
                INSERT INTO carrito (id_usuario, id_estado_carro)
                VALUES (?, (SELECT id FROM estado_carrito WHERE nombre = 'Activo'))
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlCrear, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /* ── Listar items del carrito ─────────────────────────── */

    public List<CarritoItem> listarItems(int idCarrito) throws SQLException {
        String sql = """
                SELECT dc.id            AS id_detalle,
                       dc.id_producto,
                       dc.cantidad,
                       p.nombre         AS nombre_producto,
                       p.precio_unitario,
                       p.stock_actual,
                       i.path_imagen
                FROM detalle_carrito dc
                JOIN productos p ON p.id = dc.id_producto
                LEFT JOIN imagenes_producto i ON i.id_producto = p.id
                WHERE dc.id_carrito = ?
                ORDER BY dc.id
                """;
        List<CarritoItem> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCarrito);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CarritoItem item = new CarritoItem();
                    item.setIdDetalle(rs.getInt("id_detalle"));
                    item.setIdProducto(rs.getInt("id_producto"));
                    item.setCantidad(rs.getInt("cantidad"));
                    item.setNombreProducto(rs.getString("nombre_producto"));
                    item.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    item.setStockDisponible(rs.getInt("stock_actual"));
                    item.setPathImagen(rs.getString("path_imagen"));
                    lista.add(item);
                }
            }
        }
        return lista;
    }

    /* ── Total del carrito ────────────────────────────────── */

    public BigDecimal calcularTotal(List<CarritoItem> items) {
        return items.stream()
                    .map(CarritoItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /* ── Agregar o incrementar producto ──────────────────── */

    public void agregarProducto(int idCarrito, int idProducto, int cantidad) throws SQLException {
        // Verificar stock disponible
        String sqlStock = "SELECT stock_actual FROM productos WHERE id = ?";
        int stock;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlStock)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Producto no encontrado.");
                stock = rs.getInt("stock_actual");
            }
        }

        // Ver si ya está en el carrito
        String sqlExiste = "SELECT id, cantidad FROM detalle_carrito WHERE id_carrito = ? AND id_producto = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlExiste)) {
            ps.setInt(1, idCarrito);
            ps.setInt(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idDetalle    = rs.getInt("id");
                    int cantidadActual = rs.getInt("cantidad");
                    int nuevaCantidad  = cantidadActual + cantidad;
                    if (nuevaCantidad > stock)
                        throw new SQLException("Stock insuficiente. Disponible: " + stock);
                    try (Connection con2 = DB.obtenerConexion();
                         PreparedStatement ps2 = con2.prepareStatement(
                             "UPDATE detalle_carrito SET cantidad = ? WHERE id = ?")) {
                        ps2.setInt(1, nuevaCantidad);
                        ps2.setInt(2, idDetalle);
                        ps2.executeUpdate();
                    }
                    return;
                }
            }
        }

        // No existe — insertar
        if (cantidad > stock) throw new SQLException("Stock insuficiente. Disponible: " + stock);
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO detalle_carrito (id_carrito, id_producto, cantidad) VALUES (?, ?, ?)")) {
            ps.setInt(1, idCarrito);
            ps.setInt(2, idProducto);
            ps.setInt(3, cantidad);
            ps.executeUpdate();
        }
    }

    /* ── Actualizar cantidad de un item ──────────────────── */

    public void actualizarCantidad(int idDetalle, int nuevaCantidad) throws SQLException {
        if (nuevaCantidad <= 0) {
            eliminarItem(idDetalle);
            return;
        }
        // Verificar stock
        String sqlStock = """
                SELECT p.stock_actual FROM productos p
                JOIN detalle_carrito dc ON dc.id_producto = p.id
                WHERE dc.id = ?
                """;
        int stock;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlStock)) {
            ps.setInt(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Item no encontrado.");
                stock = rs.getInt("stock_actual");
            }
        }
        if (nuevaCantidad > stock) throw new SQLException("Stock insuficiente. Disponible: " + stock);

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE detalle_carrito SET cantidad = ? WHERE id = ?")) {
            ps.setInt(1, nuevaCantidad);
            ps.setInt(2, idDetalle);
            ps.executeUpdate();
        }
    }

    /* ── Eliminar un item ────────────────────────────────── */

    public void eliminarItem(int idDetalle) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM detalle_carrito WHERE id = ?")) {
            ps.setInt(1, idDetalle);
            ps.executeUpdate();
        }
    }

    /* ── Vaciar carrito ──────────────────────────────────── */

    public void vaciarCarrito(int idCarrito) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM detalle_carrito WHERE id_carrito = ?")) {
            ps.setInt(1, idCarrito);
            ps.executeUpdate();
        }
    }

    /* ── Confirmar venta ─────────────────────────────────── */

    /*
     * Dentro de una transacción:
     * 1. Verifica stock de cada item
     * 2. Descuenta stock
     * 3. Inserta en ventas
     * 4. Vacía el carrito
     * Retorna el ID de la venta creada.
     */
    public int confirmarVenta(int idCarrito, int idUsuario, int idMetodoPago) throws SQLException {
        List<CarritoItem> items = listarItems(idCarrito);
        if (items.isEmpty()) throw new SQLException("El carrito está vacío.");

        BigDecimal total = calcularTotal(items);

        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false);
            try {
                // 1. Verificar stock de todos
                for (CarritoItem item : items) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT stock_actual FROM productos WHERE id = ?")) {
                        ps.setInt(1, item.getIdProducto());
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            int stock = rs.getInt("stock_actual");
                            if (item.getCantidad() > stock)
                                throw new SQLException("Stock insuficiente para: " + item.getNombreProducto());
                        }
                    }
                }

                // 2. Descontar stock
                for (CarritoItem item : items) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ?")) {
                        ps.setInt(1, item.getCantidad());
                        ps.setInt(2, item.getIdProducto());
                        ps.executeUpdate();
                    }
                }

                // 3. Insertar venta
                int idVenta;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO ventas (fecha_venta, id_carrito, id_metodo_pago, total_venta) " +
                        "VALUES (NOW(), ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idCarrito);
                    ps.setInt(2, idMetodoPago);
                    ps.setBigDecimal(3, total);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idVenta = rs.getInt(1);
                    }
                }

                // 4. Vaciar carrito
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM detalle_carrito WHERE id_carrito = ?")) {
                    ps.setInt(1, idCarrito);
                    ps.executeUpdate();
                }

                con.commit();
                return idVenta;

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /* ── Listar productos disponibles para agregar ────────── */

    public List<com.dulce_gestion.models.Producto> listarProductosActivos() throws SQLException {
        String sql = """
                SELECT p.id, p.nombre, p.precio_unitario, p.stock_actual, i.path_imagen
                FROM productos p
                LEFT JOIN imagenes_producto i ON i.id_producto = p.id
                WHERE p.stock_actual > 0
                ORDER BY p.nombre
                """;
        List<com.dulce_gestion.models.Producto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.dulce_gestion.models.Producto p = new com.dulce_gestion.models.Producto();
                p.setId(rs.getInt("id"));
                p.setNombre(rs.getString("nombre"));
                p.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                p.setStockActual(rs.getInt("stock_actual"));
                p.setPathImagen(rs.getString("path_imagen"));
                lista.add(p);
            }
        }
        return lista;
    }

    /* ── Listar métodos de pago ───────────────────────────── */

    public List<String[]> listarMetodosPago() throws SQLException {
        String sql = "SELECT id, nombre FROM metodo_pago ORDER BY nombre";
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }
}
