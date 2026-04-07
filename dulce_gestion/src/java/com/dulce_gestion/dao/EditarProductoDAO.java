package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO especializado en la edición de productos existentes.
 *
 * Esta clase maneja la actualización completa de productos con todas
 * sus propiedades incluyendo claves foráneas y permisos especiales.
 *
 * Tabla modificada: productos
 * Usado por: EditarProductoServlet
 *
 * Características importantes:
 * - Actualización dinámica según rol de usuario
 * - Uso de BigDecimal para precisión financiera
 * - Manejo de claves foráneas (categorías, unidades)
 * - Retrocompatibilidad con métodos sobrecargados
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
     * Lógica de emprendimiento:
     * - Si esSuperAdmin e idEmprendimiento > 0: reasigna el emprendimiento
     * - Si no: mantiene el emprendimiento actual del producto
     *
     * @param id               ID del producto a actualizar
     * @param nombre           nuevo nombre (se aplica trim())
     * @param descripcion      nueva descripción (null se guarda como cadena vacía)
     * @param stock            nuevo stock (validado >= 0 en el Servlet)
     * @param precio           nuevo precio usando BigDecimal para precisión decimal
     * @param estado           nuevo estado ("Disponible", "Agotado", "Inactivo")
     * @param fechaVencimiento nueva fecha en formato yyyy-MM-dd (puede ser null)
     * @param idCategoria      nueva categoría (FK a tabla categorias)
     * @param idUnidad         nueva unidad (FK a tabla unidad_medida)
     * @param esSuperAdmin      true si puede cambiar emprendimiento
     * @param idEmprendimiento ID del emprendimiento (solo para SuperAdmin)
     * @throws SQLException    si hay error al actualizar o si el ID no existe
     */
    //* Actualizar creado para el superadmin que puede cambiar el emprendimiento */
    public void actualizar(int id, String nombre, String descripcion, int stock,
                           BigDecimal precio, String estado, String fechaVencimiento,
                           int idCategoria, int idUnidad,
                           boolean esSuperAdmin, int idEmprendimiento) throws SQLException {

        // Construir SQL dinámico según el rol y emprendimiento
        String sql = esSuperAdmin && idEmprendimiento > 0
            ? """
                UPDATE productos
                SET nombre = ?, descripcion = ?, stock_actual = ?,
                    precio_unitario = ?, estado = ?, fecha_vencimiento = ?,
                    id_categoria = ?, id_unidad = ?, id_emprendimiento = ?
                WHERE id = ?
                """
            : """
                UPDATE productos
                SET nombre = ?, descripcion = ?, stock_actual = ?,
                    precio_unitario = ?, estado = ?, fecha_vencimiento = ?,
                    id_categoria = ?, id_unidad = ?
                WHERE id = ?
                """;

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Asignar parámetros con validación y normalización
            ps.setString(1, nombre.trim()); // Eliminar espacios en blanco
            ps.setString(2, descripcion != null ? descripcion.trim() : ""); // null → cadena vacía
            ps.setInt(3, stock); // Stock del producto
            ps.setBigDecimal(4, precio); // Precio con precisión decimal
            ps.setString(5, estado); // Estado del producto
            ps.setString(6, fechaVencimiento); // Fecha de vencimiento (puede ser null)
            ps.setInt(7, idCategoria); // FK a tabla categorias
            ps.setInt(8, idUnidad); // FK a tabla unidad_medida
            
            // Asignar parámetros adicionales según el rol
            if (esSuperAdmin && idEmprendimiento > 0) {
                ps.setInt(9, idEmprendimiento); // Nuevo emprendimiento
                ps.setInt(10, id); // ID del producto a actualizar
            } else {
                ps.setInt(9, id); // ID del producto a actualizar (sin cambiar emprendimiento)
            }
            ps.executeUpdate();
        }
    }

    /**
     * Método de retrocompatibilidad para actualizar sin cambiar el emprendimiento.
     *
     * Este método permite mantener la compatibilidad con código anterior
     * llamando al método principal con esSuperAdmin=false e idEmprendimiento=0.
     *
     * @param id               ID del producto a actualizar
     * @param nombre           nuevo nombre del producto
     * @param descripcion      nueva descripción del producto
     * @param stock            nuevo stock del producto
     * @param precio           nuevo precio del producto
     * @param estado           nuevo estado del producto
     * @param fechaVencimiento nueva fecha de vencimiento
     * @param idCategoria      nueva categoría del producto
     * @param idUnidad         nueva unidad de medida
     * @throws SQLException    si hay error al actualizar
     */
    //* actualizar utilizado por empleado y administrador */
    public void actualizar(int id, String nombre, String descripcion, int stock,
                           BigDecimal precio, String estado, String fechaVencimiento,
                           int idCategoria, int idUnidad) throws SQLException {
        // Llamar al método principal sin cambiar emprendimiento
        actualizar(id, nombre, descripcion, stock, precio, estado,
               fechaVencimiento, idCategoria, idUnidad, false, 0);
    }
}
