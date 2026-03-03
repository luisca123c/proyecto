package com.dulce_gestion.dao;

import com.dulce_gestion.models.Producto;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * DAO para consultar productos.
 * Hace LEFT JOIN con imagenes_producto para traer el path si existe.
 */
public class ProductoDAO {

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
        return null;
    }

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
        return p;
    }
}
