package com.dulce_gestion.dao;

import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.utils.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO especializado en la gestión completa de emprendimientos.
 *
 * Esta clase maneja todas las operaciones CRUD para emprendimientos
 * con validaciones de unicidad y control de acceso por roles.
 *
 * Usado por: Múltiples servlets para gestión de emprendimientos
 *
 * Características importantes:
 * - Validación de unicidad (NIT, teléfono, correo)
 * - Operaciones CRUD completas
 * - Control de estado (Activo/Inactivo)
 * - Formateo de fechas y manejo de valores nulos
 * - Solo SuperAdmin puede crear/editar/inactivar
 * - Admins y Empleados solo pueden ver información básica
 */
public class EmprendimientoDAO {

    // =========================================================
    // CONSULTAS BASE - Query con formato de fecha
    // =========================================================
    
    // Query base que incluye formateo de fecha para presentación
    private static final String SELECT_BASE =
        "SELECT id, nombre, nit, direccion, ciudad, telefono, correo, estado, " +
        "DATE_FORMAT(fecha_creacion, '%d/%m/%Y') AS fecha_creacion " +
        "FROM emprendimientos ";

    // =========================================================
    // LISTADO - Obtener emprendimientos según estado
    // =========================================================

    /**
     * Lista todos los emprendimientos sin filtro de estado.
     *
     * @return lista completa de emprendimientos (activos e inactivos)
     * @throws SQLException si hay error en la consulta
     */
    public List<Emprendimiento> listarTodos() throws SQLException {
        return ejecutarLista(SELECT_BASE + "ORDER BY nombre");
    }

    /**
     * Lista solo los emprendimientos con estado 'Activo'.
     *
     * @return lista de emprendimientos activos
     * @throws SQLException si hay error en la consulta
     */
    public List<Emprendimiento> listarActivos() throws SQLException {
        return ejecutarLista(SELECT_BASE + "WHERE estado = 'Activo' ORDER BY nombre");
    }

    // =========================================================
    // BÚSQUEDA - Consulta por ID
    // =========================================================

    /**
     * Busca un emprendimiento por su ID.
     *
     * @param id ID del emprendimiento a buscar
     * @return objeto Emprendimiento con todos sus datos, o null si no existe
     * @throws SQLException si hay error en la consulta
     */
    public Emprendimiento buscarPorId(int id) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    // =========================================================
    // VALIDACIONES DE UNICIDAD - Verificar duplicados
    // =========================================================


    /**
     * Verifica si un NIT ya existe en otro emprendimiento.
     *
     * Esta validación es crucial para evitar duplicados de NIT,
     * que es un identificador único legal para cada emprendimiento.
     *
     * @param nit       NIT a verificar (se normaliza con trim)
     * @param excludeId ID a excluir (para edición); 0 en creación
     * @return true si el NIT ya lo usa otro emprendimiento
     * @throws SQLException si falla la consulta
     */
    public boolean nitExisteEnOtro(String nit, int excludeId) throws SQLException {
        String sql = excludeId > 0
            ? "SELECT id FROM emprendimientos WHERE nit = ? AND id != ?"
            : "SELECT id FROM emprendimientos WHERE nit = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nit.trim()); // Normalizar NIT
            if (excludeId > 0) ps.setInt(2, excludeId); // Excluir registro actual en edición
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Verifica si un teléfono ya existe en otro emprendimiento.
     *
     * @param telefono  teléfono a verificar (se normaliza con trim)
     * @param excludeId ID a excluir (para edición); 0 en creación
     * @return true si el teléfono ya lo usa otro emprendimiento
     * @throws SQLException si falla la consulta
     */
    public boolean telefonoExisteEnOtro(String telefono, int excludeId) throws SQLException {
        String sql = excludeId > 0
            ? "SELECT id FROM emprendimientos WHERE telefono = ? AND id != ?"
            : "SELECT id FROM emprendimientos WHERE telefono = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, telefono.trim()); // Normalizar teléfono
            if (excludeId > 0) ps.setInt(2, excludeId); // Excluir registro actual
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Verifica si un correo ya existe en otro emprendimiento.
     *
     * @param correo    correo a verificar (se normaliza a minúsculas)
     * @param excludeId ID a excluir (para edición); 0 en creación
     * @return true si el correo ya lo usa otro emprendimiento
     * @throws SQLException si falla la consulta
     */
    public boolean correoExisteEnOtro(String correo, int excludeId) throws SQLException {
        String sql = excludeId > 0
            ? "SELECT id FROM emprendimientos WHERE correo = ? AND id != ?"
            : "SELECT id FROM emprendimientos WHERE correo = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo.trim().toLowerCase()); // Normalizar correo a minúsculas
            if (excludeId > 0) ps.setInt(2, excludeId); // Excluir registro actual
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // =========================================================
    // CREACIÓN - Insertar nuevo emprendimiento
    // =========================================================
    /**
     * Crea un nuevo emprendimiento en la base de datos.
     *
     * Este método maneja valores nulos convirtiéndolos a NULL en la BD,
     * lo que permite que campos opcionales como NIT, dirección, etc.
     * puedan ser omitidos durante la creación.
     *
     * @param nombre    nombre del emprendimiento (obligatorio)
     * @param nit       NIT del emprendimiento (opcional)
     * @param direccion dirección física (opcional)
     * @param ciudad    ciudad (opcional)
     * @param telefono  teléfono de contacto (opcional)
     * @param correo    correo electrónico (opcional)
     * @throws SQLException si hay error al insertar
     */
    public void crear(String nombre, String nit, String direccion,
                      String ciudad, String telefono, String correo) throws SQLException {
        // INSERT INTO emprendimientos: tabla principal para registro de negocios/empresas
        // Cada registro representa un emprendimiento independiente con sus datos de contacto
        String sql = "INSERT INTO emprendimientos (nombre, nit, direccion, ciudad, telefono, correo) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Asignar parámetros con validación y normalización
            ps.setString(1, nombre.trim()); // Nombre obligatorio del emprendimiento
            
            // Campos opcionales: si son null o vacíos, se guardan como NULL en la BD
            // Esto permite registrar emprendimientos con información mínima inicial
            ps.setString(2, nit    != null && !nit.isBlank()       ? nit.trim()       : null);    // NIT/identificación fiscal
            ps.setString(3, direccion != null && !direccion.isBlank() ? direccion.trim() : null);    // Dirección física
            ps.setString(4, ciudad != null && !ciudad.isBlank()    ? ciudad.trim()    : null);    // Ciudad/ubicación
            ps.setString(5, telefono != null && !telefono.isBlank()? telefono.trim()  : null);    // Teléfono de contacto
            ps.setString(6, correo != null && !correo.isBlank()    ? correo.trim()    : null);    // Correo electrónico
            
            ps.executeUpdate(); // Ejecutar inserción del nuevo emprendimiento
        }
    }

    // =========================================================
    // EDICIÓN - Actualizar emprendimiento existente
    // =========================================================

    /**
     * Actualiza los datos de un emprendimiento existente.
     *
     * Similar al método crear(), maneja valores nulos convirtiéndolos
     * a NULL en la base de datos para campos opcionales.
     *
     * @param id        ID del emprendimiento a actualizar
     * @param nombre    nuevo nombre del emprendimiento
     * @param nit       nuevo NIT (opcional)
     * @param direccion nueva dirección (opcional)
     * @param ciudad    nueva ciudad (opcional)
     * @param telefono  nuevo teléfono (opcional)
     * @param correo    nuevo correo (opcional)
     * @throws SQLException si hay error al actualizar
     */
    public void editar(int id, String nombre, String nit, String direccion,
                       String ciudad, String telefono, String correo) throws SQLException {
        String sql = "UPDATE emprendimientos SET nombre=?, nit=?, direccion=?, " +
                     "ciudad=?, telefono=?, correo=? WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre.trim()); // Nombre obligatorio, siempre con trim
            // Campos opcionales: si son null o vacíos, se guardan como NULL
            ps.setString(2, nit       != null && !nit.isBlank()       ? nit.trim()       : null);
            ps.setString(3, direccion != null && !direccion.isBlank() ? direccion.trim() : null);
            ps.setString(4, ciudad    != null && !ciudad.isBlank()    ? ciudad.trim()    : null);
            ps.setString(5, telefono  != null && !telefono.isBlank()  ? telefono.trim()  : null);
            ps.setString(6, correo    != null && !correo.isBlank()    ? correo.trim()    : null);
            ps.setInt(7, id); // ID del emprendimiento a actualizar
            ps.executeUpdate();
        }
    }

    // =========================================================
    // CONTROL DE ESTADO - Activar/Inactivar emprendimientos
    // =========================================================

    /**
     * Inactiva un emprendimiento (eliminación lógica).
     *
     * Cambia el estado a 'Inactivo' sin borrar el registro,
     * preservando el historial y relaciones con usuarios.
     *
     * @param id ID del emprendimiento a inactivar
     * @throws SQLException si hay error al actualizar
     */
    public void inactivar(int id) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE emprendimientos SET estado = 'Inactivo' WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Activa un emprendimiento previamente inactivado.
     *
     * Cambia el estado a 'Activo', permitiendo que el emprendimiento
     * vuelva a estar operativo en el sistema.
     *
     * @param id ID del emprendimiento a activar
     * @throws SQLException si hay error al actualizar
     */
    public void activar(int id) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE emprendimientos SET estado = 'Activo' WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // =========================================================
    // MÉTODOS HELPER - Funciones reutilizables
    // =========================================================

    /**
     * Ejecuta una consulta y retorna la lista de emprendimientos.
     *
     * Método helper que encapsula la lógica común para ejecutar
     * consultas SELECT que retornan listas de emprendimientos.
     *
     * @param sql consulta SQL a ejecutar
     * @return lista de emprendimientos mapeados desde el ResultSet
     * @throws SQLException si hay error en la consulta
     */
    private List<Emprendimiento> ejecutarLista(String sql) throws SQLException {
        List<Emprendimiento> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /**
     * Mapea un ResultSet a un objeto Emprendimiento.
     *
     * Método helper que convierte una fila del ResultSet
     * en un objeto Emprendimiento con todos sus campos.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto Emprendimiento con los datos del ResultSet
     * @throws SQLException si hay error al acceder a los campos
     */
    private Emprendimiento mapear(ResultSet rs) throws SQLException {
        Emprendimiento e = new Emprendimiento();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        e.setNit(rs.getString("nit"));
        e.setDireccion(rs.getString("direccion"));
        e.setCiudad(rs.getString("ciudad"));
        e.setTelefono(rs.getString("telefono"));
        e.setCorreo(rs.getString("correo"));
        e.setEstado(rs.getString("estado"));
        e.setFechaCreacion(rs.getString("fecha_creacion")); // Fecha ya formateada
        return e;
    }
}
