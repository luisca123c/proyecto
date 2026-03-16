package com.dulce_gestion.dao;

import com.dulce_gestion.models.Producto;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * DAO: ProductoDAO
 * Tabla principal:    productos
 * Tablas del JOIN:    categorias, unidad_medida, imagenes_producto
 * Usado por:          ProductosServlet, EditarProductoServlet,
 *                     NuevoProductoServlet, CarritoDAO
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Consulta productos de la BD. Es el DAO de solo lectura para productos:
 * listar todos, buscar uno por ID, y cargar las opciones de los <select>
 * de categorías y unidades de medida.
 *
 * Las operaciones de escritura (crear, editar, eliminar) están en sus
 * propios DAOs especializados (CrearProductoDAO, EditarProductoDAO, etc.)
 * para mantener cada clase con una única responsabilidad.
 *
 * ¿POR QUÉ LEFT JOIN CON imagenes_producto?
 * ------------------------------------------
 * Un producto puede existir sin imagen (la imagen es opcional).
 * Con JOIN normal (INNER JOIN), los productos sin imagen no aparecerían
 * en la lista. Con LEFT JOIN, aparecen todos los productos y las columnas
 * de imagen quedan null para los que no tienen.
 * El JSP verifica if (prod.getPathImagen() != null) antes de mostrar la imagen.
 */
public class ProductoDAO {

    /**
     * Retorna la lista completa de productos con todos sus datos asociados.
     *
     * El JOIN triple trae en una sola consulta:
     *   - Nombre de categoría (no solo el id_categoria)
     *   - Nombre de unidad de medida (no solo el id_unidad)
     *   - Path e imagen alt (puede ser null si el producto no tiene imagen)
     *
     * @return  lista de todos los productos ordenada alfabéticamente por nombre
     * @throws SQLException si hay error al consultar la BD
     */
    public List<Producto> listarTodos() throws SQLException {
        String sql = """
                SELECT p.id, p.nombre, p.descripcion, p.stock_actual,
                       p.precio_unitario, p.estado, p.fecha_vencimiento,
                       p.id_categoria, c.nombre AS nombre_categoria,
                       p.id_unidad,    u.nombre AS nombre_unidad,
                       i.path_imagen,  i.alt_imagen
                FROM productos p
                JOIN categorias    c ON c.id = p.id_categoria
                JOIN unidad_medida u ON u.id = p.id_unidad
                LEFT JOIN imagenes_producto i ON i.id_producto = p.id
                ORDER BY p.nombre
                """;

        List<Producto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs)); // Cada fila → un objeto Producto
        }
        return lista;
    }

    /**
     * Busca un producto por su ID con todos sus datos asociados.
     * Se usa en EditarProductoServlet para prellenar el formulario de edición.
     *
     * @param id  ID del producto a buscar
     * @return    objeto Producto con todos sus datos, o null si no existe
     * @throws SQLException si hay error al consultar la BD
     */
    /**
     * Lista solo productos con estado distinto de 'Inactivo'.
     * Usado en el carrito de ventas para no mostrar productos eliminados.
     */
    public List<Producto> listarActivos() throws SQLException {
        String sql = """
                SELECT p.id, p.nombre, p.descripcion, p.stock_actual,
                       p.precio_unitario, p.estado, p.fecha_vencimiento,
                       p.id_categoria, c.nombre AS nombre_categoria,
                       p.id_unidad,    u.nombre AS nombre_unidad,
                       i.path_imagen,  i.alt_imagen
                FROM productos p
                JOIN categorias    c ON c.id = p.id_categoria
                JOIN unidad_medida u ON u.id = p.id_unidad
                LEFT JOIN imagenes_producto i ON i.id_producto = p.id
                WHERE p.estado != 'Inactivo'
                ORDER BY p.nombre
                """;

        List<Producto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Producto buscarPorId(int id) throws SQLException {
        String sql = """
                SELECT p.id, p.nombre, p.descripcion, p.stock_actual,
                       p.precio_unitario, p.estado, p.fecha_vencimiento,
                       p.id_categoria, c.nombre AS nombre_categoria,
                       p.id_unidad,    u.nombre AS nombre_unidad,
                       i.path_imagen,  i.alt_imagen
                FROM productos p
                JOIN categorias    c ON c.id = p.id_categoria
                JOIN unidad_medida u ON u.id = p.id_unidad
                LEFT JOIN imagenes_producto i ON i.id_producto = p.id
                WHERE p.id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null; // El producto no existe
    }

    /**
     * Retorna la lista de categorías para el <select> del formulario.
     *
     * Devuelve [id, nombre] por cada categoría. El JSP usa el id como
     * value del option y el nombre como texto visible:
     *   <option value="2">Lácteos</option>
     *
     * @return  lista de [id, nombre] por cada categoría, ordenada alfabéticamente
     * @throws SQLException si hay error al consultar la BD
     */
    public List<String[]> listarCategorias() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM categorias ORDER BY nombre";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    /**
     * Retorna la lista de unidades de medida para el <select> del formulario.
     *
     * @return  lista de [id, nombre] por cada unidad, ordenada alfabéticamente
     * @throws SQLException si hay error al consultar la BD
     */
    public List<String[]> listarUnidades() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM unidad_medida ORDER BY nombre";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    /**
     * Convierte una fila del ResultSet en un objeto Producto.
     *
     * ¿POR QUÉ ESTE MÉTODO PRIVADO?
     * --------------------------------
     * listarTodos() y buscarPorId() hacen el mismo SELECT y necesitan
     * mapear las mismas columnas. Centralizar evita duplicar las ~14 líneas
     * de setters en cada método. Si se agrega una columna nueva al modelo
     * Producto, solo se cambia aquí.
     *
     * @param rs  ResultSet posicionado en la fila a mapear
     * @return    objeto Producto con todos los campos del ResultSet
     * @throws SQLException si algún nombre de columna no existe
     */
    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setPrecioUnitario(rs.getBigDecimal("precio_unitario")); // BigDecimal para precisión monetaria
        p.setEstado(rs.getString("estado"));                     // "Disponible", "Agotado", "Inactivo"
        p.setFechaVencimiento(rs.getString("fecha_vencimiento"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setNombreCategoria(rs.getString("nombre_categoria"));  // Del JOIN con categorias
        p.setIdUnidad(rs.getInt("id_unidad"));
        p.setNombreUnidad(rs.getString("nombre_unidad"));        // Del JOIN con unidad_medida
        p.setPathImagen(rs.getString("path_imagen"));            // Puede ser null (LEFT JOIN)
        p.setAltImagen(rs.getString("alt_imagen"));              // Puede ser null (LEFT JOIN)
        return p;
    }
}
