package com.dulce_gestion.dao;

import com.dulce_gestion.models.CarritoItem;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * DAO: CarritoDAO
 * Tablas escritas:    carrito, detalle_carrito, productos, ventas
 * Tablas leídas:      estado_carrito, metodo_pago, imagenes_producto
 * Usado por:          VentasServlet
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Maneja todo el ciclo de vida del carrito de ventas:
 *   - Obtener o crear el carrito activo del usuario
 *   - Listar ítems, calcular el total
 *   - Agregar, actualizar cantidad y eliminar ítems
 *   - Vaciar el carrito
 *   - Confirmar la venta (la operación más importante: transacción de 4 pasos)
 *   - Listar el catálogo de productos disponibles y los métodos de pago
 *
 * ¿POR QUÉ UN SOLO DAO PARA TANTAS OPERACIONES?
 * -----------------------------------------------
 * Todas estas operaciones trabajan sobre el mismo conjunto de tablas
 * (carrito + detalle_carrito + productos + ventas) y comparten contexto:
 * siempre se opera sobre "el carrito activo del usuario actual".
 * Dividirlo en varios DAOs aumentaría la complejidad sin beneficio real.
 *
 * ¿QUÉ ES EL ESTADO DEL CARRITO?
 * --------------------------------
 * La tabla estado_carrito define los estados posibles:
 *   "Activo"    → el carrito está en uso, el usuario puede agregar/quitar ítems
 *   "Inactivo"  → el carrito fue usado en una venta confirmada
 *   "Cancelado" → el carrito fue cancelado sin completar venta
 *
 * Cada usuario tiene siempre un carrito Activo. Al confirmar la venta,
 * el carrito pasa a Inactivo (o se vacía) y la próxima acción del usuario
 * creará un carrito Activo nuevo.
 */
public class CarritoDAO {

    // =========================================================
    // OBTENER O CREAR CARRITO ACTIVO
    // =========================================================

    /**
     * Retorna el ID del carrito Activo del usuario, o crea uno nuevo si no existe.
     *
     * ¿POR QUÉ "OBTENER O CREAR"?
     * ----------------------------
     * En la primera visita de un usuario a /ventas, no tiene carrito.
     * Este método garantiza que siempre hay un carrito disponible.
     *
     * FLUJO:
     * 1. SELECT: buscar carrito con estado 'Activo' para este usuario.
     * 2. Si existe → retornar su ID.
     * 3. Si no existe → INSERT nuevo carrito con estado 'Activo' → retornar ID generado.
     *
     * ¿POR QUÉ SE BUSCA EL ESTADO POR NOMBRE Y NO POR ID?
     * -----------------------------------------------------
     * (SELECT id FROM estado_carrito WHERE nombre = 'Activo')
     * El ID del estado puede variar entre bases de datos (dev, prod).
     * Buscarlo por nombre hace el código portable sin depender de IDs hardcodeados.
     *
     * @param idUsuario  ID del usuario dueño del carrito
     * @return           ID del carrito activo (existente o recién creado)
     * @throws SQLException si hay error al consultar o insertar en la BD
     */
    public int obtenerOCrearCarrito(int idUsuario) throws SQLException {

        // Buscar carrito activo existente para este usuario
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
                if (rs.next()) return rs.getInt("id"); // Ya existe → retornar el ID
            }
        }

        // No existe carrito activo → crear uno nuevo
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
                return rs.getInt(1); // ID del carrito recién creado
            }
        }
    }

    // =========================================================
    // LISTAR ÍTEMS Y CALCULAR TOTAL
    // =========================================================

    /**
     * Retorna la lista de ítems en el carrito con todos sus datos.
     *
     * El JOIN triple trae en una sola consulta:
     *   - Nombre y precio del producto (para mostrar en la tabla)
     *   - Stock actual (para validar que la cantidad pedida no lo supera)
     *   - Path de imagen (para mostrar thumbnail en el carrito)
     *
     * @param idCarrito  ID del carrito cuyos ítems se quieren listar
     * @return           lista de CarritoItem ordenada por el ID del detalle
     * @throws SQLException si hay error al consultar la BD
     */
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
                    item.setStockDisponible(rs.getInt("stock_actual")); // Para validar max en el input
                    item.setPathImagen(rs.getString("path_imagen"));    // Puede ser null (LEFT JOIN)
                    lista.add(item);
                }
            }
        }
        return lista;
    }

    /**
     * Calcula el total del carrito sumando los subtotales de todos los ítems.
     *
     * Se hace en Java (no en SQL) porque los ítems ya están cargados en memoria.
     * CarritoItem.getSubtotal() calcula: precio_unitario × cantidad.
     * BigDecimal.add() suma sin errores de punto flotante.
     *
     * @param items  lista de ítems del carrito
     * @return       suma de todos los subtotales (BigDecimal.ZERO si la lista está vacía)
     */
    public BigDecimal calcularTotal(List<CarritoItem> items) {
        // Stream reduce: empieza con ZERO y acumula sumando cada subtotal
        return items.stream()
                    .map(CarritoItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =========================================================
    // AGREGAR PRODUCTO AL CARRITO
    // =========================================================

    /**
     * Agrega un producto al carrito o incrementa su cantidad si ya está.
     *
     * FLUJO:
     * 1. Verificar que el producto existe y tiene stock suficiente.
     * 2. Buscar si el producto ya está en el carrito.
     *    → Si sí: verificar que (cantidad actual + cantidad nueva) ≤ stock
     *             y hacer UPDATE de la cantidad.
     *    → Si no: verificar que la cantidad nueva ≤ stock
     *             y hacer INSERT nuevo ítem.
     *
     * ¿POR QUÉ SE VERIFICA EL STOCK ANTES DE AGREGAR?
     * -------------------------------------------------
     * Sin verificación, un usuario podría agregar 100 unidades de un
     * producto con stock 5. La validación en el DAO es la última
     * línea de defensa (el JSP también tiene validación en el cliente,
     * pero esta puede eludirse con herramientas de desarrollador).
     *
     * @param idCarrito  ID del carrito activo
     * @param idProducto ID del producto a agregar
     * @param cantidad   cantidad a agregar (ya validada como >= 1 en el Servlet)
     * @throws SQLException si no hay stock suficiente o el producto no existe
     */
    public void agregarProducto(int idCarrito, int idProducto, int cantidad) throws SQLException {

        // Verificar que no se mezclen emprendimientos en el carrito
        String sqlEmpProducto = "SELECT id_emprendimiento FROM productos WHERE id = ?";
        int empProductoNuevo;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlEmpProducto)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Producto no encontrado.");
                empProductoNuevo = rs.getInt("id_emprendimiento");
            }
        }
        int empCarrito = obtenerEmprendimientoDelCarrito(idCarrito);
        if (empCarrito > 0 && empCarrito != empProductoNuevo) {
            throw new SQLException("No puedes mezclar productos de diferentes emprendimientos en el mismo carrito. Vacía el carrito primero.");
        }

        // Verificar stock disponible del producto
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

        // Ver si el producto ya está en el carrito para incrementar la cantidad
        String sqlExiste = "SELECT id, cantidad FROM detalle_carrito WHERE id_carrito = ? AND id_producto = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlExiste)) {
            ps.setInt(1, idCarrito);
            ps.setInt(2, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // El producto ya está en el carrito → incrementar cantidad
                    int idDetalle      = rs.getInt("id");
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
                    return; // Listo → no continuar al INSERT
                }
            }
        }

        // El producto no estaba en el carrito → insertar nuevo ítem
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

    // =========================================================
    // ACTUALIZAR CANTIDAD, ELIMINAR ÍTEM, VACIAR
    // =========================================================

    /**
     * Actualiza la cantidad de un ítem del carrito.
     * Si la nueva cantidad es 0 o negativa, elimina el ítem.
     *
     * @param idDetalle    ID de la fila en detalle_carrito
     * @param nuevaCantidad nueva cantidad (si <= 0 → elimina el ítem)
     * @throws SQLException si no hay stock suficiente o el ítem no existe
     */
    public void actualizarCantidad(int idDetalle, int nuevaCantidad) throws SQLException {
        if (nuevaCantidad <= 0) {
            // Si la cantidad llega a 0, quitar el ítem del carrito directamente
            eliminarItem(idDetalle);
            return;
        }

        // Verificar que la nueva cantidad no supere el stock disponible
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

        // Actualizar la cantidad del ítem
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE detalle_carrito SET cantidad = ? WHERE id = ?")) {
            ps.setInt(1, nuevaCantidad);
            ps.setInt(2, idDetalle);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina un ítem específico del carrito por su ID en detalle_carrito.
     *
     * @param idDetalle  ID de la fila a eliminar en detalle_carrito
     * @throws SQLException si hay error al eliminar
     */
    public void eliminarItem(int idDetalle) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM detalle_carrito WHERE id = ?")) {
            ps.setInt(1, idDetalle);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina todos los ítems del carrito (vaciar).
     * El carrito en sí (fila en la tabla carrito) se mantiene.
     *
     * @param idCarrito  ID del carrito a vaciar
     * @throws SQLException si hay error al eliminar
     */
    public void vaciarCarrito(int idCarrito) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM detalle_carrito WHERE id_carrito = ?")) {
            ps.setInt(1, idCarrito);
            ps.executeUpdate();
        }
    }

    // =========================================================
    // CONFIRMAR VENTA (TRANSACCIÓN PRINCIPAL)
    // =========================================================

    /**
     * Confirma la venta: registra en ventas, descuenta stock y vacía el carrito.
     * Todo dentro de una transacción para garantizar consistencia.
     *
     * FLUJO EN TRANSACCIÓN:
     *
     * Pre-condición: el carrito no puede estar vacío.
     *
     * Paso 1 — Verificar stock de TODOS los ítems:
     *   Se verifica todo antes de modificar nada. Si cualquier producto
     *   no tiene stock suficiente, se lanza excepción y se hace ROLLBACK.
     *   Sin este paso, podría descontarse stock de algunos productos y
     *   fallar en otro, dejando la BD en estado inconsistente.
     *
     * Paso 2 — Descontar stock de cada producto:
     *   stock_actual = stock_actual - cantidad_pedida
     *   Se usa aritmética en SQL para evitar condiciones de carrera:
     *   si dos vendedores venden el último ítem simultáneamente, la BD
     *   aplica el descuento atómicamente.
     *
     * Paso 3 — Insertar registro en ventas:
     *   fecha_venta = NOW() → timestamp exacto del servidor BD
     *   id_carrito → referencia al carrito (para trazabilidad histórica)
     *   total_venta → suma calculada en Java con BigDecimal (exactitud)
     *
     * Paso 4 — Vaciar el carrito:
     *   DELETE en detalle_carrito. El carrito en sí se mantiene (id en carrito)
     *   para la próxima venta del mismo usuario.
     *
     * ¿POR QUÉ EL CARRITO NO PASA A ESTADO "INACTIVO"?
     * --------------------------------------------------
     * Al vaciar el carrito y mantenerlo Activo, el usuario puede
     * empezar a agregar ítems inmediatamente para la siguiente venta.
     * No hay necesidad de crear un carrito nuevo.
     *
     * @param idCarrito    ID del carrito que se va a confirmar
     * @param idUsuario    ID del usuario que realiza la venta (para trazabilidad)
     * @param idMetodoPago FK a metodo_pago ("Efectivo" o "Nequi")
     * @return             ID de la venta creada (para mostrar en el mensaje de éxito)
     * @throws SQLException si el carrito está vacío, hay stock insuficiente o error de BD
     */
    public int confirmarVenta(int idCarrito, int idUsuario, int idMetodoPago) throws SQLException {

        List<CarritoItem> items = listarItems(idCarrito);
        if (items.isEmpty()) throw new SQLException("El carrito está vacío.");

        BigDecimal total = calcularTotal(items); // Total exacto con BigDecimal

        try (Connection con = DB.obtenerConexion()) {
            con.setAutoCommit(false); // Iniciar transacción

            try {
                // ── Paso 1: verificar stock de TODOS los ítems antes de modificar ──
                for (CarritoItem item : items) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT stock_actual FROM productos WHERE id = ?")) {
                        ps.setInt(1, item.getIdProducto());
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            int stock = rs.getInt("stock_actual");
                            if (item.getCantidad() > stock)
                                throw new SQLException(
                                    "Stock insuficiente para: " + item.getNombreProducto() +
                                    " (disponible: " + stock + ")");
                        }
                    }
                }

                // ── Paso 2: descontar stock de cada producto ──────────────
                for (CarritoItem item : items) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ?")) {
                        ps.setInt(1, item.getCantidad());
                        ps.setInt(2, item.getIdProducto());
                        ps.executeUpdate();
                    }
                }

                // ── Paso 3: registrar la venta ────────────────────────────
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
                        idVenta = rs.getInt(1); // ID de la nueva venta
                    }
                }

                // ── Paso 4: marcar el carrito como Inactivo (preserva los ítems para historial) ──
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE carrito SET id_estado_carro = " +
                        "(SELECT id FROM estado_carrito WHERE nombre = 'Inactivo') " +
                        "WHERE id = ?")) {
                    ps.setInt(1, idCarrito);
                    ps.executeUpdate();
                }

                // ── Paso 5: crear un carrito Activo nuevo para la próxima venta ──
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO carrito (id_usuario, id_estado_carro) " +
                        "VALUES (?, (SELECT id FROM estado_carrito WHERE nombre = 'Activo'))")) {
                    ps.setInt(1, idUsuario);
                    ps.executeUpdate();
                }

                con.commit(); // Todos los pasos exitosos → confirmar la transacción
                return idVenta;

            } catch (SQLException e) {
                con.rollback(); // Algo falló → deshacer TODO (stock, venta, carrito)
                throw e;        // Re-lanzar para que el Servlet muestre el error
            }
        }
    }

    // =========================================================
    // CATÁLOGO Y MÉTODOS DE PAGO
    // =========================================================

    /**
     * Retorna los productos disponibles para agregar al carrito.
     * Solo incluye productos con stock_actual > 0.
     *
     * ¿POR QUÉ FILTRAR POR stock > 0?
     * ---------------------------------
     * No tiene sentido mostrar en el catálogo de ventas productos
     * que no se pueden vender. Filtrar en SQL evita traer filas
     * innecesarias y que el JSP tenga que ocultar productos.
     *
     * @return  lista de productos disponibles ordenada por nombre
     * @throws SQLException si hay error al consultar la BD
     */
    /**
     * Retorna los productos disponibles para el carrito del usuario,
     * filtrando siempre por el emprendimiento del usuario.
     *
     * @param idEmprendimiento  ID del emprendimiento del usuario en sesión
     */
    /**
     * Lista productos activos con stock disponible para el carrito.
     * Si idEmprendimiento=0 (SuperAdmin sin filtro) → todos los emprendimientos.
     * Si idEmprendimiento>0 → solo ese emprendimiento.
     * Incluye id_emprendimiento para validar mezcla de emprendimientos en el carrito.
     */
    public List<com.dulce_gestion.models.Producto> listarProductosActivos(int idEmprendimiento)
            throws SQLException {
        String sql;
        boolean filtrar = idEmprendimiento > 0;
        if (filtrar) {
            sql = """
                    SELECT p.id, p.nombre, p.precio_unitario, p.stock_actual,
                           p.id_emprendimiento, e.nombre AS nombre_emprendimiento,
                           i.path_imagen
                    FROM productos p
                    JOIN emprendimientos e ON e.id = p.id_emprendimiento
                    LEFT JOIN imagenes_producto i ON i.id_producto = p.id
                    WHERE p.stock_actual > 0
                      AND p.estado != 'Inactivo'
                      AND p.id_emprendimiento = ?
                    ORDER BY p.nombre
                    """;
        } else {
            sql = """
                    SELECT p.id, p.nombre, p.precio_unitario, p.stock_actual,
                           p.id_emprendimiento, e.nombre AS nombre_emprendimiento,
                           i.path_imagen
                    FROM productos p
                    JOIN emprendimientos e ON e.id = p.id_emprendimiento
                    LEFT JOIN imagenes_producto i ON i.id_producto = p.id
                    WHERE p.stock_actual > 0
                      AND p.estado != 'Inactivo'
                    ORDER BY e.nombre, p.nombre
                    """;
        }
        List<com.dulce_gestion.models.Producto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filtrar) ps.setInt(1, idEmprendimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.dulce_gestion.models.Producto p = new com.dulce_gestion.models.Producto();
                    p.setId(rs.getInt("id"));
                    p.setNombre(rs.getString("nombre"));
                    p.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    p.setStockActual(rs.getInt("stock_actual"));
                    p.setPathImagen(rs.getString("path_imagen"));
                    p.setIdEmprendimiento(rs.getInt("id_emprendimiento"));
                    p.setNombreEmprendimiento(rs.getString("nombre_emprendimiento"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    /**
     * Obtiene el id_emprendimiento de los productos ya en el carrito.
     * Retorna 0 si el carrito está vacío.
     */
    public int obtenerEmprendimientoDelCarrito(int idCarrito) throws SQLException {
        String sql = "SELECT p.id_emprendimiento FROM detalle_carrito dc " +
                      "JOIN productos p ON p.id = dc.id_producto " +
                      "WHERE dc.id_carrito = ? LIMIT 1";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCarrito);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_emprendimiento") : 0;
            }
        }
    }

    /**
     * Retorna los métodos de pago disponibles para el modal de confirmación.
     *
     * @return  lista de [id, nombre] por cada método de pago (ej: ["1", "Efectivo"])
     * @throws SQLException si hay error al consultar la BD
     */
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
