package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO especializado en la creación de productos en el sistema.
 *
 * Esta clase maneja la inserción de nuevos productos en la tabla
 * {@code productos} con todas sus propiedades y relaciones.
 *
 * Tabla modificada:
 * 
 * {@code productos} - almacenamiento de información de productos
 * 
 *
 * Características importantes:
 * 
 *   Precisión decimal: uso de {@code BigDecimal} para precios
 *   Validación de datos: trim() y manejo de valores nulos
 *   Recuperación de ID: obtiene el ID generado automáticamente
 *   Manejo de FKs: categorías, unidades y emprendimientos
 *
 *
 * @see NuevoProductoServlet Servlet que consume este DAO
 */
public class CrearProductoDAO {

    /**
     * Inserta un nuevo producto en la base de datos y retorna su ID generado.
     *
     * Este método maneja la inserción completa de un producto con todas
     * sus propiedades incluyendo claves foráneas a tablas relacionadas.
     *
     * Se utiliza {@code BigDecimal} para el precio unitario para garantizar
     * precisión en cálculos financieros y evitar errores de redondeo
     * que ocurren con {@code float} o {@code double}.
     *
     * Recuperación de ID:
     * {@code Statement.RETURN_GENERATED_KEYS} indica al driver JDBC que
     * capture el ID generado por {@code AUTO_INCREMENT} de MySQL,
     * permitiendo retornarlo al servlet para posibles operaciones posteriores.
     *
     * @param nombre           nombre del producto (se aplica trim() para eliminar espacios)
     * @param descripcion      descripción detallada del producto (opcional, null se convierte a cadena vacía)
     * @param stock            stock inicial del producto (validado como >= 0 en el Servlet)
     * @param precio           precio unitario usando BigDecimal para precisión decimal
     * @param estado           estado del producto ("Disponible", "Agotado", "Inactivo")
     * @param fechaVencimiento fecha de vencimiento en formato yyyy-MM-dd (puede ser null)
     * @param idCategoria      clave foránea a tabla {@code categorias}
     * @param idUnidad         clave foránea a tabla {@code unidad_medida}
     * @param idEmprendimiento clave foránea a tabla {@code emprendimientos}
     * @return                 ID generado automáticamente por AUTO_INCREMENT del nuevo producto
     * @throws SQLException    si hay error al insertar o si no se puede obtener el ID generado
     */
    public int crear(String nombre, String descripcion, int stock,
                     BigDecimal precio, String estado, String fechaVencimiento,
                     int idCategoria, int idUnidad, int idEmprendimiento) throws SQLException {

        // INSERT INTO productos: tabla principal del catálogo de productos
        // Cada registro representa un producto disponible para venta
        String sql = """
                INSERT INTO productos
                    (nombre, descripcion, stock_actual, precio_unitario,
                     estado, fecha_vencimiento, id_categoria, id_unidad, id_emprendimiento)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        // Statement.RETURN_GENERATED_KEYS permite obtener el ID AUTO_INCREMENT
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Asignar parámetros en el mismo orden que los campos del INSERT
            ps.setString(1, nombre);                    // Nombre del producto para mostrar
            ps.setString(2, descripcion);                // Descripción detallada del producto
            ps.setInt(3, stock);                         // Cantidad inicial disponible
            ps.setBigDecimal(4, precio);                  // Precio unitario (BigDecimal para precisión)
            ps.setString(5, "Activo");                    // Estado inicial siempre "Activo"
            ps.setString(6, fechaVencimiento);             // Fecha de vencimiento (puede ser NULL)
            ps.setInt(7, idCategoria);                    // FK: categoría del producto
            ps.setInt(8, idUnidad);                       // FK: unidad de medida
            ps.setInt(9, idEmprendimiento);               // FK: emprendimiento dueño del producto

            // Asignar parámetros con validación y normalización
            ps.setString(1, nombre.trim()); // Eliminar espacios en blanco
            ps.setString(2, descripcion != null ? descripcion.trim() : ""); // null → cadena vacía
            ps.setInt(3, stock); // Stock inicial del producto
            ps.setBigDecimal(4, precio); // Precio con precisión decimal
            ps.setString(5, estado); // Estado del producto
            ps.setString(6, fechaVencimiento); // Fecha de vencimiento (puede ser null)
            ps.setInt(7, idCategoria); // FK a tabla categorias
            ps.setInt(8, idUnidad); // FK a tabla unidad_medida
            ps.setInt(9, idEmprendimiento); // FK a tabla emprendimientos
            ps.executeUpdate();

            // Recuperar el ID generado por MySQL usando getGeneratedKeys()
            try (ResultSet keys = ps.getGeneratedKeys()) {
                // .next() mueve el cursor al siguiente registro y retorna true si existe
                if (keys.next()) {
                    return keys.getInt(1); // Retornar el ID del nuevo producto
                }
            }
        }

        // Si executeUpdate() no lanzó excepción pero no hay ID generado,
        // algo inesperado ocurrió en la base de datos
        throw new SQLException("No se obtuvo el ID del producto creado.");
    }
}
