package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * ============================================================
 * DAO: EditarProductoDAO
 * Tabla escrita:  productos
 * Usado por:      EditarProductoServlet
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Actualiza los datos de un producto existente en la tabla productos.
 * Solo actualiza los campos de datos (columnas de la tabla productos).
 * La imagen se maneja por separado en EditarProductoServlet +
 * ImagenProductoDAO para mantener la separación de responsabilidades.
 *
 * ¿POR QUÉ LA IMAGEN NO SE ACTUALIZA AQUÍ?
 * ------------------------------------------
 * La imagen involucra dos operaciones distintas:
 *   1. Manejo de archivos físicos en disco (guardar/borrar)
 *   2. Actualizar la ruta en imagenes_producto (otra tabla)
 *
 * Si se pusiera todo en este DAO, mezclaría lógica de BD con lógica
 * de sistema de archivos. EditarProductoServlet orquesta ambas cosas
 * coordinando este DAO con ImagenProductoDAO y la clase Uploads.
 */
public class EditarProductoDAO {

    /**
     * Actualiza todos los campos de datos de un producto.
     *
     * El UPDATE incluye 8 campos. Si alguno no cambió, igual se actualiza
     * con el mismo valor (no se hace UPDATE selectivo por campo modificado),
     * lo cual simplifica el código sin impacto significativo en rendimiento
     * para tablas de este tamaño.
     *
     * @param id               ID del producto a actualizar
     * @param nombre           nuevo nombre
     * @param descripcion      nueva descripción (null se guarda como "")
     * @param stock            nuevo stock
     * @param precio           nuevo precio (BigDecimal para exactitud monetaria)
     * @param estado           nuevo estado ("Disponible", "Agotado", "Inactivo")
     * @param fechaVencimiento nueva fecha en formato yyyy-MM-dd
     * @param idCategoria      nueva categoría (FK)
     * @param idUnidad         nueva unidad de medida (FK)
     * @throws SQLException    si hay error al actualizar o si el ID no existe
     */
    public void actualizar(int id, String nombre, String descripcion, int stock,
                           BigDecimal precio, String estado, String fechaVencimiento,
                           int idCategoria, int idUnidad) throws SQLException {

        String sql = """
                UPDATE productos
                SET nombre = ?, descripcion = ?, stock_actual = ?,
                    precio_unitario = ?, estado = ?, fecha_vencimiento = ?,
                    id_categoria = ?, id_unidad = ?
                WHERE id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nombre.trim());
            ps.setString(2, descripcion != null ? descripcion.trim() : ""); // null → string vacío
            ps.setInt(3, stock);
            ps.setBigDecimal(4, precio);
            ps.setString(5, estado);
            ps.setString(6, fechaVencimiento);
            ps.setInt(7, idCategoria);
            ps.setInt(8, idUnidad);
            ps.setInt(9, id); // Condición WHERE: solo actualizar el producto con este ID

            ps.executeUpdate();
        }
    }
}
