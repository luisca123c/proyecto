package com.dulce_gestion.dao;

import com.dulce_gestion.models.CarritoItem;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el módulo de Carrito de Compras.
 *
 * Esta clase maneja todas las operaciones relacionadas con el carrito de compras
 * del sistema. Funciona como una capa de abstracción entre la lógica de negocio
 * y la base de datos para el proceso completo de compras: desde agregar productos
 * hasta confirmar la venta.
 *
 * Tabla principal: {@code carrito} con sus detalles en {@code detalle_carrito}.
 * Implementa validaciones importantes como stock disponible y consistencia
 * de emprendimientos en el mismo carrito.
 *
 * Tablas escritas: {@code carrito}, {@code detalle_carrito}, {@code productos}, {@code ventas}.<br>
 * Tablas leídas: {@code metodo_pago}, {@code imagenes_producto}, {@code emprendimientos}.
 *
 */
public class CarritoDAO {

    // =========================================================
    // GESTIÓN DE CARRITO - Obtener y crear carritos activos
    // =========================================================

    /**
     * Retorna el ID del carrito Activo del usuario, o crea uno nuevo si no existe.
     *
     * Este método implementa el patrón "get-or-create": primero busca un carrito
     * en estado 'Activo' para el usuario, y si no lo encuentra, crea uno nuevo.
     * Esto garantiza que cada usuario siempre tenga exactamente un carrito activo.
     *
     * @param idUsuario ID del usuario en sesión
     * @return ID del carrito activo (existente o recién creado)
     * @throws SQLException si hay error en la consulta o inserción
     */
    public int obtenerOCrearCarrito(int idUsuario) throws SQLException {

        // Paso 1: Buscar carrito activo existente para este usuario
        String sqlBuscar = """
                SELECT id FROM carrito
                WHERE id_usuario = ?
                  AND estado = 'Activo'
                LIMIT 1
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setInt(1, idUsuario); // Parámetro para filtrar por usuario
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id"); // Ya existe → retornar el ID
            }
        }

        // Paso 2: No existe carrito activo → crear uno nuevo
        String sqlCrear = """
                INSERT INTO carrito (id_usuario, estado)
                VALUES (?, 'Activo')
                """;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlCrear, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUsuario); // Asociar carrito al usuario
            ps.executeUpdate();
            
            // Obtener el ID generado automáticamente por la BD
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1); // ID del carrito recién creado
            }
        }
    }

    // =========================================================
    // CONSULTA DE ÍTEMS - Listar productos y calcular totales
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
        // Consulta con JOINs para obtener todos los datos necesarios en una sola llamada
        /** LEFT JOIN con imágenes: el producto puede no tener imagen */

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
            ps.setInt(1, idCarrito); // Filtrar por carrito específico
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CarritoItem item = new CarritoItem();
                    // Mapear campos del ResultSet al objeto CarritoItem
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
        // Stream API con reduce: empieza con ZERO y acumula sumando cada subtotal
        // BigDecimal.ZERO es el valor neutro para sumas (equivalente a 0)
        // CarritoItem::getSubtotal es referencia de método (más legible que lambda)
        // stream: recorre la lista de ítems y aplica getSubtotal a cada uno
        return items.stream()
                    .map(CarritoItem::getSubtotal) // Extraer subtotal de cada ítem
                    .reduce(BigDecimal.ZERO, BigDecimal::add); // Sumar todos los subtotales
    }

    // =========================================================
    // AGREGAR PRODUCTO AL CARRITO
    // =========================================================

    /**
     * Agrega un producto al carrito o incrementa su cantidad si ya está.
     *
     * Este método implementa múltiples validaciones críticas:
     * 1. No mezclar productos de diferentes emprendimientos en el mismo carrito
     * 2. Verificar stock disponible antes de agregar
     * 3. Actualizar cantidad si el producto ya existe, o insertar nuevo
     *
     * @param idCarrito   ID del carrito activo del usuario
     * @param idProducto  ID del producto a agregar
     * @param cantidad    Cantidad a agregar (se suma si ya existe)
     * @throws SQLException si hay violación de reglas de negocio
     */
    public void agregarProducto(int idCarrito, int idProducto, int cantidad) throws SQLException {

        // Validación 1: Verificar que no se mezclen emprendimientos en el carrito
        String sqlEmpProducto = "SELECT id_emprendimiento FROM productos WHERE id = ?";
        int empProductoNuevo;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlEmpProducto)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Producto no encontrado.");
                empProductoNuevo = rs.getInt("id_emprendimiento"); // Emprendimiento del nuevo producto
            }
        }
        
        // Obtener emprendimiento actual del carrito (0 si está vacío)
        int empCarrito = obtenerEmprendimientoDelCarrito(idCarrito);
        if (empCarrito > 0 && empCarrito != empProductoNuevo) {
            throw new SQLException("No puedes mezclar productos de diferentes emprendimientos en el mismo carrito. Vacía el carrito primero.");
        }

        // Validación 2: Verificar stock disponible del producto
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

        // Validación 3: Ver si el producto ya está en el carrito para incrementar la cantidad
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
                    
                    // Validar que la nueva cantidad no supere el stock
                    if (nuevaCantidad > stock)
                        throw new SQLException("Stock insuficiente. Disponible: " + stock);
                        
                    // Actualizar cantidad existente
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
        // INSERT INTO detalle_carrito: tabla intermedia entre carrito y productos
        // Cada registro representa un producto específico en el carrito de un usuario
        if (cantidad > stock) throw new SQLException("Stock insuficiente. Disponible: " + stock);
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO detalle_carrito (id_carrito, id_producto, cantidad) VALUES (?, ?, ?)")) {
            ps.setInt(1, idCarrito);   // FK: carrito al que pertenece este ítem
            ps.setInt(2, idProducto);  // FK: producto que se está agregando
            ps.setInt(3, cantidad);    // Cantidad inicial del producto
            ps.executeUpdate();
        }
    }

    // =========================================================
    // MODIFICACIÓN DE ÍTEMS - Actualizar cantidades y eliminar
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
            // Si la cantidad llega a 0 o menos, eliminar el ítem directamente
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

        // Actualizar la cantidad del ítem en la base de datos
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
        // Eliminar el ítem específico del carrito
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
        // Eliminar todos los ítems del carrito pero mantener la fila del carrito
        // Esto preserva el historial y permite reutilizar el mismo carrito
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM detalle_carrito WHERE id_carrito = ?")) {
            ps.setInt(1, idCarrito);
            ps.executeUpdate();
        }
    }

    // =========================================================
    // PROCESO DE VENTA - Transacción completa y confirmación
    // =========================================================

    /**
     * Confirma la venta: registra en ventas, descuenta stock y vacía el carrito.
     * Todo dentro de una transacción para garantizar consistencia total.
     *
     * <strong>Transacción ACID:</strong>
     * - Atomicity: Todo o nada se ejecuta
     * - Consistency: La BD siempre queda en estado válido
     * - Isolation: Otros usuarios no ven cambios parciales
     * - Durability: Los cambios persisten aunque haya fallos
     *
     * @param idCarrito         ID del carrito a procesar
     * @param idUsuario         ID del usuario que realiza la compra
     * @param idMetodoPago      ID del método de pago seleccionado
     * @param idEmprendimiento  ID del emprendimiento (0 o negativo = NULL)
     * @return                  ID de la venta registrada
     * @throws SQLException     Si hay error en cualquier paso de la transacción
     */
    public int confirmarVenta(int idCarrito, int idUsuario, int idMetodoPago,
                              int idEmprendimiento) throws SQLException {

        // Obtener ítems del carrito y validar que no esté vacío
        List<CarritoItem> items = listarItems(idCarrito);
        if (items.isEmpty()) throw new SQLException("El carrito está vacío.");

        // Calcular total exacto usando BigDecimal (evita errores de punto flotante)
        BigDecimal total = calcularTotal(items);

        try (Connection con = DB.obtenerConexion()) {
            // auto-commit realiza commit automático tras cada sentencia
            con.setAutoCommit(false); // desactivar auto-commit para transacción manual

            try {
                // ── Paso 1: Verificar stock de TODOS los ítems antes de modificar ──
                // Esta verificación previa evita descontar stock parcialmente si hay error
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

                // ── Paso 2: Descontar stock de cada producto (actualización atómica) ──
                // Se usa stock_actual = stock_actual - ? para evitar condiciones de carrera
                for (CarritoItem item : items) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ?")) {
                        ps.setInt(1, item.getCantidad());
                        ps.setInt(2, item.getIdProducto());
                        ps.executeUpdate();
                    }
                }

                // ── Paso 3: Registrar la venta en la tabla ventas ──
                int idVenta;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO ventas (fecha_venta, id_carrito, id_metodo_pago, total_venta, id_emprendimiento) " +
                        "VALUES (NOW(), ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idCarrito);
                    ps.setInt(2, idMetodoPago);
                    ps.setBigDecimal(3, total);
                    // Manejar emprendimiento opcional (puede ser NULL)
                    if (idEmprendimiento > 0) ps.setInt(4, idEmprendimiento);
                    else                      ps.setNull(4, java.sql.Types.INTEGER);
                    ps.executeUpdate();
                    
                    // Obtener el ID generado automáticamente para la venta
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idVenta = rs.getInt(1); // ID de la nueva venta
                    }
                }

                // ── Paso 4: Marcar el carrito como Inactivo (preserva historial) ──
                // No se elimina el carrito, solo se cambia estado para mantener historial
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE carrito SET estado = 'Inactivo' WHERE id = ?")) {
                    ps.setInt(1, idCarrito);
                    ps.executeUpdate();
                }

                // ── Paso 5: Crear un carrito Activo nuevo para la próxima compra ──
                // Esto asegura que el usuario siempre tenga un carrito disponible
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO carrito (id_usuario, estado) VALUES (?, 'Activo')")) {
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
    // DATOS DE REFERENCIA - Catálogos y métodos de pago
    // =========================================================

    /**
     * Retorna los productos disponibles para agregar al carrito.
     * Solo incluye productos con stock_actual > 0.
     *
     */
    /**
     * Retorna los productos disponibles para el carrito del usuario,
     * filtrando siempre por el emprendimiento del usuario.
     *
     * @param idEmprendimiento  ID del emprendimiento del usuario en sesión
     */
    /**
     * Lista productos activos con stock disponible para el carrito.
     * 
     * Este método filtra productos según el emprendimiento del usuario:
     * - Si idEmprendimiento=0 (SuperAdmin sin filtro) → todos los emprendimientos
     * - Si idEmprendimiento>0 → solo ese emprendimiento específico
     * 
     * Incluye LEFT JOIN con imágenes para mostrar thumbnails en el carrito.
     * Solo muestra productos con stock > 0 y estado != 'Inactivo'.
     *
     * @param idEmprendimiento ID del emprendimiento para filtrar (0 = sin filtro)
     * @return lista de productos disponibles para agregar al carrito
     * @throws SQLException si hay error en la consulta
     */
    public List<com.dulce_gestion.models.Producto> listarProductosActivos(int idEmprendimiento)
            throws SQLException {
        // Construir SQL dinámico según el filtro de emprendimiento
        String sql;
        boolean filtrar = idEmprendimiento > 0;
        if (filtrar) {
            // SQL con filtro específico de emprendimiento
            /** LEFT JOIN: el producto puede no tener imagen */
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
            // SQL sin filtro (todos los emprendimientos)
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
        // Ejecutar consulta y mapear resultados
        List<com.dulce_gestion.models.Producto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filtrar) ps.setInt(1, idEmprendimiento); // Asignar parámetro solo si hay filtro
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Mapear ResultSet a objeto Producto
                    com.dulce_gestion.models.Producto p = new com.dulce_gestion.models.Producto();
                    p.setId(rs.getInt("id"));
                    p.setNombre(rs.getString("nombre"));
                    p.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    p.setStockActual(rs.getInt("stock_actual"));
                    p.setPathImagen(rs.getString("path_imagen")); // Puede ser null
                    p.setIdEmprendimiento(rs.getInt("id_emprendimiento"));
                    p.setNombreEmprendimiento(rs.getString("nombre_emprendimiento"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    /**
     * Obtiene el emprendimiento de los productos ya en el carrito.
     * 
     * Este método es crucial para la validación que impide mezclar productos
     * de diferentes emprendimientos en el mismo carrito. Retorna 0 si el carrito está vacío.
     *
     * @param idCarrito ID del carrito a consultar
     * @return ID del emprendimiento (0 si el carrito está vacío)
     * @throws SQLException si hay error en la consulta
     */
    public int obtenerEmprendimientoDelCarrito(int idCarrito) throws SQLException {
        // Consulta para obtener el emprendimiento del primer producto en el carrito
        // LIMIT 1 porque todos los productos deben ser del mismo emprendimiento
        String sql = "SELECT p.id_emprendimiento FROM detalle_carrito dc " +
                      "JOIN productos p ON p.id = dc.id_producto " +
                      "WHERE dc.id_carrito = ? LIMIT 1";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCarrito);
            try (ResultSet rs = ps.executeQuery()) {
                // Retornar el emprendimiento si hay productos, sino 0 (carrito vacío)
                return rs.next() ? rs.getInt("id_emprendimiento") : 0;
            }
        }
    }

    /**
     * Retorna los métodos de pago disponibles para el modal de confirmación.
     *
     * Este método carga el catálogo de métodos de pago del sistema.
     * El resultado se usa para poblar el campo desplegable en el formulario
     * de confirmación de compra.
     *
     * @return lista de pares [id, nombre] para el select HTML
     * @throws SQLException si hay error al consultar la BD
     */
    public List<String[]> listarMetodosPago() throws SQLException {
        // Consulta simple para obtener todos los métodos de pago ordenados por nombre
        String sql = "SELECT id, nombre FROM metodo_pago ORDER BY nombre";
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Convertir cada fila a un arreglo [id, nombre] para el select HTML
            while (rs.next())
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }
}
