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
 * DAO especializado en la gestión de productos del sistema.
 *
 * Esta clase maneja las operaciones de consulta para productos
 * con filtros por emprendimiento y estado, incluyendo datos
 * asociados como categorías y unidades de medida.
 *
 * Tabla principal: productos
 * Tablas del JOIN: categorias, unidad_medida, imagenes_producto, emprendimientos
 *
 * Características importantes:
 * - Consultas complejas con múltiples JOINs para obtener datos completos
 * - Filtros por emprendimiento según rol de usuario
 * - Manejo de estados (activo/inactivo) para eliminación lógica
 * - Integración con imágenes de productos
 * - Listados optimizados para diferentes contextos (todos, activos, filtrados)
 *
 * Usado por: ProductosServlet, EditarProductoServlet, NuevoProductoServlet, CarritoDAO
 */
public class ProductoDAO {

    // =========================================================
    // QUERY BASE - Consulta con JOINs múltiples para datos completos
    // =========================================================
    // Query base con JOINs para obtener todos los datos asociados en una sola consulta:
    // - categorias: obtiene nombre de categoría (no solo el id_categoria)
    // - unidad_medida: obtiene nombre de unidad (no solo el id_unidad)
    // - imagenes_producto: obtiene path y alt (LEFT JOIN porque puede ser null)
    // - emprendimientos: obtiene nombre del emprendimiento para filtrado y presentación
    private static final String SELECT_BASE =
            "SELECT p.id, p.nombre, p.descripcion, p.stock_actual, " +
            "       p.precio_unitario, p.estado, p.fecha_vencimiento, " +
            "       p.id_categoria, cat.nombre AS nombre_categoria, " +
            "       p.id_unidad,    u.nombre   AS nombre_unidad, " +
            "       i.path_imagen,  i.alt_imagen, " +
            "       p.id_emprendimiento, e.nombre AS nombre_emprendimiento " +
            "FROM productos p " +
            "JOIN categorias       cat ON cat.id = p.id_categoria " +
            "JOIN unidad_medida    u   ON u.id   = p.id_unidad " +
            "JOIN emprendimientos  e   ON e.id   = p.id_emprendimiento " +
            "LEFT JOIN imagenes_producto i ON i.id_producto = p.id ";

    // =========================================================
    // LISTADOS - Obtener productos según diferentes criterios
    // =========================================================
    /**
     * Retorna la lista completa de productos con todos sus datos asociados.
     *
     * El JOIN múltiple trae en una sola consulta:
     * - Nombre de categoría (no solo el id_categoria)
     * - Nombre de unidad de medida (no solo el id_unidad)
     * - Path y alt de imagen (puede ser null si el producto no tiene imagen)
     * - Nombre del emprendimiento
     *
     * @return lista de todos los productos ordenada por emprendimiento y nombre
     * @throws SQLException si hay error al consultar la BD
     */
    public List<Producto> listarTodos() throws SQLException {
        return ejecutarLista(SELECT_BASE + "ORDER BY e.nombre, p.nombre");
    }

    /**
     * Lista productos filtrados por emprendimiento según rol del usuario.
     *
     * Implementa reglas de visibilidad:
     * - SuperAdmin con idEmp=0: ve todos los productos de todos los emprendimientos
     * - SuperAdmin con idEmp>0: filtra por ese emprendimiento específico
     * - Admin: siempre pasa su propio idEmprendimiento para ver solo sus productos
     *
     * @param rol              rol del usuario ("SuperAdministrador" o "Administrador")
     * @param idEmprendimiento ID del emprendimiento para filtrar (0 = sin filtro)
     * @return lista de productos según reglas de visibilidad
     * @throws SQLException si hay error al consultar la BD
     */
    public List<Producto> listarFiltrado(String rol, int idEmprendimiento) throws SQLException {
        if ("SuperAdministrador".equals(rol) && idEmprendimiento == 0) {
            // SuperAdmin viendo todos los productos sin filtro
            return ejecutarLista(SELECT_BASE + "ORDER BY e.nombre, p.nombre");
        }
        // Filtro por emprendimiento específico (Admin o SuperAdmin con filtro)
        String sql = SELECT_BASE + "WHERE p.id_emprendimiento = ? ORDER BY p.nombre";
        List<Producto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEmprendimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Busca un producto por su ID con todos sus datos asociados.
     *
     * Se usa en EditarProductoServlet para prellenar el formulario de edición.
     * Usa el query base para obtener todos los datos necesarios en una sola consulta.
     *
     * @param id ID del producto a buscar
     * @return objeto Producto con todos sus datos, o null si no existe
     * @throws SQLException si hay error al consultar la BD
     */
    public Producto buscarPorId(int id) throws SQLException {
        String sql = SELECT_BASE + "WHERE p.id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }
    /**
     * Lista solo productos con estado distinto de 'Inactivo'.
     *
     * Usado en el carrito de ventas para no mostrar productos eliminados
     * lógicamente. Filtra productos con estado 'Activo' o 'Pendiente'.
     *
     * @return lista de productos activos ordenada alfabéticamente por nombre
     * @throws SQLException si hay error al consultar la BD
     */
    public List<Producto> listarActivos() throws SQLException {
        return ejecutarLista(SELECT_BASE + "WHERE p.estado != 'Inactivo' ORDER BY p.nombre");
    }

    /**
     * Lista productos activos filtrados por emprendimiento.
     *
     * Usado en el carrito de ventas para obtener solo los productos
     * disponibles de un emprendimiento específico, excluyendo
     * los productos eliminados lógicamente.
     *
     * @param idEmprendimiento ID del emprendimiento para filtrar
     * @return lista de productos activos del emprendimiento ordenada por nombre
     * @throws SQLException si hay error al consultar la BD
     */
    public List<Producto> listarActivosPorEmprendimiento(int idEmprendimiento) throws SQLException {
        String sql = SELECT_BASE +
                     "WHERE p.estado != 'Inactivo' AND p.id_emprendimiento = ? ORDER BY p.nombre";
        List<Producto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEmprendimiento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // =========================================================
    // LISTAS PARA FORMULARIOS - Categorías y unidades de medida
    // =========================================================
    /**
     * Retorna la lista de categorías para el selector del formulario.
     *
     * Devuelve pares [id, nombre] por cada categoría para que el JSP
     * pueda generar las opciones del select dinámicamente.
     *
     * @return lista de [id, nombre] por cada categoría ordenada alfabéticamente
     * @throws SQLException si hay error al consultar la BD
     */
    public List<String[]> listarCategorias() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM categorias ORDER BY nombre";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                // Crear par [id, nombre] para cada categoría
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    /**
     * Retorna la lista de unidades de medida para el selector del formulario.
     *
     * Devuelve pares [id, nombre] por cada unidad de medida para que
     * el JSP pueda generar las opciones del select dinámicamente.
     *
     * @return lista de [id, nombre] por cada unidad ordenada alfabéticamente
     * @throws SQLException si hay error al consultar la BD
     */
    public List<String[]> listarUnidades() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre FROM unidad_medida ORDER BY nombre";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                // Crear par [id, nombre] para cada unidad de medida
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    // =========================================================
    // HELPER - Métodos privados para ejecución de consultas
    // =========================================================
    /**
     * Ejecuta una consulta SQL y retorna la lista de productos mapeados.
     *
     * Método helper para evitar duplicación de código en los métodos
     * de listado que usan el mismo patrón de ejecución.
     *
     * @param sql consulta SQL a ejecutar
     * @return lista de productos mapeados desde el ResultSet
     * @throws SQLException si hay error en la consulta
     */
    private List<Producto> ejecutarLista(String sql) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /**
     * Convierte una fila del ResultSet en un objeto Producto.
     *
     * Mapea todos los campos del query base incluyendo datos
     * asociados como categorías, unidades, imágenes y emprendimiento.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto Producto con todos los datos
     * @throws SQLException si hay error al acceder a los campos
     */
    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        p.setEstado(rs.getString("estado"));
        p.setFechaVencimiento(rs.getString("fecha_vencimiento"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setNombreCategoria(rs.getString("nombre_categoria"));
        p.setIdUnidad(rs.getInt("id_unidad"));
        p.setNombreUnidad(rs.getString("nombre_unidad"));
        p.setPathImagen(rs.getString("path_imagen"));
        p.setAltImagen(rs.getString("alt_imagen"));
        p.setIdEmprendimiento(rs.getInt("id_emprendimiento"));
        p.setNombreEmprendimiento(rs.getString("nombre_emprendimiento"));
        return p;
    }
}
