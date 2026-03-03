package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * DAO para la tabla imagenes_producto.
 * Cada producto tiene máximo una imagen (se guarda/reemplaza en la misma fila).
 * El archivo físico se guarda en assets/images/productos/ y aquí
 * solo se almacena el nombre del archivo y el texto alternativo.
 */
public class ImagenProductoDAO {

    /*
     * Retorna el path_imagen de un producto, o null si no tiene.
     */
    public String obtenerPath(int idProducto) throws SQLException {
        String sql = "SELECT path_imagen FROM imagenes_producto WHERE id_producto = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("path_imagen") : null;
            }
        }
    }

    /*
     * Guarda o reemplaza la imagen de un producto.
     * Si ya existe un registro para ese producto hace UPDATE, si no hace INSERT.
     */
    public void guardarOActualizar(int idProducto, String pathImagen, String altImagen)
            throws SQLException {

        // Verificar si ya existe registro para este producto
        String sqlCheck = "SELECT id FROM imagenes_producto WHERE id_producto = ?";
        boolean existe = false;
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sqlCheck)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                existe = rs.next();
            }
        }

        String sql = existe
            ? "UPDATE imagenes_producto SET path_imagen = ?, alt_imagen = ? WHERE id_producto = ?"
            : "INSERT INTO imagenes_producto (path_imagen, alt_imagen, id_producto) VALUES (?, ?, ?)";

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pathImagen);
            ps.setString(2, altImagen);
            ps.setInt(3, idProducto);
            ps.executeUpdate();
        }
    }

    /*
     * Elimina el registro de imagen de un producto.
     * (El archivo físico lo borra el Servlet antes de llamar esto.)
     */
    public void eliminar(int idProducto) throws SQLException {
        String sql = "DELETE FROM imagenes_producto WHERE id_producto = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.executeUpdate();
        }
    }
}
