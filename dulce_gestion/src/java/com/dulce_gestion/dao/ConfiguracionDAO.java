package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestión de tablas de configuración del sistema.
 *
 * Esta clase maneja las operaciones CRUD para las tablas maestras del sistema
 * que son utilizadas para configurar las opciones disponibles en los formularios.
 * Solo accesible por usuarios con rol SuperAdministrador.
 *
 * Tablas gestionadas: {@code categorias}, {@code unidad_medida}, {@code metodo_pago}.
 * Todas las tablas tienen estructura similar: id, nombre, descripción (opcional), activo.
 *
 */
public class ConfiguracionDAO {

    // ── Modelo genérico (id, nombre, descripcion opcional) ──────────────
    // constructor: Fila(int id, String nombre, String descripcion, boolean activo)
    public static class Fila {
        public int     id;          
        public String  nombre;       
        /**
         * Descripción detallada (solo para categorías).
         */
        public String  descripcion;   
        /**
         * Estado del registro (true=activo, false=inactivo).
         */
        public boolean activo = true; 
    }

    // =========================================================
    // GESTIÓN DE CATEGORÍAS - CRUD completo para categorías de productos
    // =========================================================══

    /**
     * Lista todas las categorías ordenadas alfabéticamente.
     * 
     * Incluye tanto categorías activas como inactivas para que el administrador
     * pueda reactivarlas si es necesario.
     *
     * @return lista de categorías con todos sus campos
     * @throws SQLException si hay error en la consulta
     */
    public List<Fila> listarCategorias() throws SQLException {
        return listar("SELECT id, nombre, descripcion, activo FROM categorias ORDER BY nombre");
    }

    /**
     * Crea una nueva categoría en el sistema.
     * 
     * Valida y limpia los datos antes de insertarlos:
     * - Elimina espacios en blanco con trim()
     * - Convierte cadenas vacías a null para la descripción
     *
     * @param nombre      nombre de la categoría (requerido)
     * @param descripcion descripción detallada (opcional)
     * @throws SQLException si hay error en la inserción
     */
    public void crearCategoria(String nombre, String descripcion) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO categorias (nombre, descripcion) VALUES (?, ?)")) {
            ps.setString(1, nombre.trim()); // Eliminar espacios en blanco
            // Manejar descripción opcional: vacía → null
            ps.setString(2, descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null);
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza una categoría existente.
     * 
     * Permite modificar nombre, descripción y estado activo.
     * El estado activo controla si la categoría aparece en los formularios.
     *
     * @param id          ID de la categoría a actualizar
     * @param nombre      nuevo nombre de la categoría
     * @param descripcion nueva descripción (opcional)
     * @param activo      true para activar, false para desactivar
     * @throws SQLException si hay error en la actualización
     */
    public void editarCategoria(int id, String nombre, String descripcion, boolean activo) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE categorias SET nombre=?, descripcion=?, activo=? WHERE id=?")) {
            ps.setString(1, nombre.trim());
            // Manejar descripción opcional: vacía → null
            ps.setString(2, descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null);
            ps.setInt(3, activo ? 1 : 0); // Convertir boolean a int para la BD
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina lógicamente una categoría.
     * 
     * En lugar de borrar el registro (DELETE), lo marca como inactivo.
     * Esto preserva el historial y permite reactivarlo después.
     *
     * @param id ID de la categoría a desactivar
     * @throws SQLException si hay error en la actualización
     */
    public void eliminarCategoria(int id) throws SQLException {
        ejecutar("UPDATE categorias SET activo=0 WHERE id=?", id);
    }

    // =========================================================
    // GESTIÓN DE UNIDADES DE MEDIDA - CRUD para unidades de productos
    // =========================================================══

    /**
     * Lista todas las unidades de medida ordenadas alfabéticamente.
     * 
     * Incluye tanto unidades activas como inactivas para que el administrador
     * pueda reactivarlas si es necesario.
     *
     * @return lista de unidades de medida con todos sus campos
     * @throws SQLException si hay error en la consulta
     */
    /**
     * Lista todas las unidades de medida ordenadas alfabéticamente.
     * 
     * Incluye tanto unidades activas como inactivas para que el administrador
     * pueda reactivarlas si es necesario.
     *
     * @return lista de unidades de medida con todos sus campos
     * @throws SQLException si hay error en la consulta
     */
    public List<Fila> listarUnidades() throws SQLException {
        return listar("SELECT id, nombre, NULL AS descripcion, activo FROM unidad_medida ORDER BY nombre");
    }

    /**
     * Crea una nueva unidad de medida en el sistema.
     * 
     * Valida y limpia los datos antes de insertarlos:
     * - Elimina espacios en blanco con trim()
     *
     * @param nombre nombre de la unidad de medida (requerido)
     * @throws SQLException si hay error en la inserción
     */
    /**
     * Crea una nueva unidad de medida en el sistema.
     * 
     * Valida y limpia los datos antes de insertarlos:
     * - Elimina espacios en blanco con trim()
     *
     * @param nombre nombre de la unidad de medida (requerido)
     * @throws SQLException si hay error en la inserción
     */
    public void crearUnidad(String nombre) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO unidad_medida (nombre) VALUES (?)")) {
            ps.setString(1, nombre.trim()); // Eliminar espacios en blanco
            ps.executeUpdate();
        }
    }

    public void editarUnidad(int id, String nombre, boolean activo) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE unidad_medida SET nombre=?, activo=? WHERE id=?")) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, activo ? 1 : 0); // Convertir boolean a int para la BD
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina lógicamente una unidad de medida.
     * 
     * En lugar de borrar el registro (DELETE), lo marca como inactivo.
     * Esto preserva el historial y permite reactivarlo después.
     *
     * @param id ID de la unidad a desactivar
     * @throws SQLException si hay error en la actualización
     *
     * @return lista de métodos de pago con todos sus campos
     * @throws SQLException si hay error en la consulta
     */
    public List<Fila> listarMetodos() throws SQLException {
        return listar("SELECT id, nombre, NULL AS descripcion, activo FROM metodo_pago ORDER BY nombre");
    }

    /**
     * Crea un nuevo método de pago en el sistema.
     * 
     * Valida y limpia los datos antes de insertarlos:
     * - Elimina espacios en blanco con trim()
     *
     * @param nombre nombre del método de pago (requerido)
     * @throws SQLException si hay error en la inserción
     */
    public void crearMetodo(String nombre) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO metodo_pago (nombre) VALUES (?)")) {
            ps.setString(1, nombre.trim()); // Eliminar espacios en blanco
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza un método de pago existente.
     * 
     * Permite modificar nombre y estado activo.
     * El estado activo controla si el método aparece en los formularios.
     *
     * @param id     ID del método a actualizar
     * @param nombre nuevo nombre del método
     * @param activo true para activar, false para desactivar
     * @throws SQLException si hay error en la actualización
     */
    public void editarMetodo(int id, String nombre, boolean activo) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE metodo_pago SET nombre=?, activo=? WHERE id=?")) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, activo ? 1 : 0); // Convertir boolean a int para la BD
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina lógicamente un método de pago.
     * 
     * En lugar de borrar el registro (DELETE), lo marca como inactivo.
     * Esto preserva el historial y permite reactivarlo después.
     *
     * @param id ID del método a desactivar
     * @throws SQLException si hay error en la actualización
     */
    public void eliminarMetodo(int id) throws SQLException {
        ejecutar("UPDATE metodo_pago SET activo=0 WHERE id=?", id);
    }

    // =========================================================
    // UTILIDADES INTERNAS - Métodos helper reutilizables
    // =========================================================

    /**
     * Método genérico para ejecutar consultas SELECT y mapear resultados.
     * 
     * Este método helper evita duplicación de código al procesar
     * los resultados de cualquier consulta que retorne la estructura
     * estándar (id, nombre, descripcion, activo).
     *
     * @param sql consulta SQL a ejecutar
     * @return lista de objetos Fila con los resultados
     * @throws SQLException si hay error en la consulta
     */
    private List<Fila> listar(String sql) throws SQLException {
        List<Fila> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Fila f = new Fila();
                // Mapear campos del ResultSet al objeto Fila
                f.id          = rs.getInt("id");
                f.nombre      = rs.getString("nombre");
                f.descripcion = rs.getString("descripcion"); // Puede ser null
                f.activo      = rs.getInt("activo") == 1; // Convertir int a boolean
                lista.add(f);
            }
        }
        return lista;
    }

    /**
     * Método genérico para ejecutar consultas UPDATE con un solo parámetro.
     * 
     * Este método helper se usa principalmente para las operaciones
     * de eliminación lógica (UPDATE SET activo=0 WHERE id=?).
     *
     * @param sql consulta UPDATE a ejecutar
     * @param id  parámetro ID para la consulta
     * @throws SQLException si hay error en la actualización
     */
    private void ejecutar(String sql, int id) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id); // Asignar el parámetro ID
            ps.executeUpdate();
        }
    }
}
