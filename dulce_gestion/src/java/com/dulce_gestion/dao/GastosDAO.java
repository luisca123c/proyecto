package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO especializado en la gestión de gastos del sistema.
 *
 * Esta clase maneja todas las operaciones CRUD para gastos con
 * filtros por emprendimiento y control de acceso por roles.
 *
 * Tabla principal: gastos
 * Tablas leídas: metodo_pago, usuarios, perfil_usuario, emprendimientos, roles
 *
 * Características importantes:
 * - Filtros por emprendimiento con COALESCE
 * - Control de acceso según rol de usuario
 * - Formateo de fechas para presentación y edición
 * - Identificación de registros de SuperAdmin
 * - Uso de BigDecimal para precisión financiera
 *
 * El emprendimiento se resuelve con COALESCE(e2.nombre, e1.nombre):
 * si el SuperAdministrador asignó un emprendimiento explícito al registrar,
 * ese tiene precedencia; de lo contrario se usa el del usuario registrador.
 */
public class GastosDAO {

    // =========================================================
    // MODELO INTERNO - DTO para vista de gastos
    // =========================================================

    /**
     * DTO que representa un gasto para mostrar en la vista del historial.
     *
     * Contiene todos los datos necesarios para la tabla del JSP,
     * incluyendo fechas formateadas para presentación y edición,
     * y la identificación del rol del usuario que registró el gasto.
     */
    public static class FilaGasto {
        public int        id;                    // ID único del gasto
        public String     fecha;                 // Fecha formateada dd/MM/yyyy HH:mm para mostrar
        public String     fechaRaw;              // Fecha en formato yyyy-MM-dd para input type="date"
        public String     descripcion;           // Descripción del gasto
        public int        idMetodoPago;          // ID del método de pago
        public String     metodoPago;            // Nombre del método de pago
        public String     registradoPor;        // Nombre del usuario que registró
        public BigDecimal total;                 // Monto total del gasto
        public String     nombreEmprendimiento;  // Nombre del emprendimiento
        public boolean    registradoPorSuperAdmin; // true si fue registrado por SuperAdmin
    }

    // Query base con JOINs para obtener todos los datos necesarios en una sola consulta
    // Incluye emprendimiento con COALESCE para manejar asignación explícita vs emprendimiento del usuario
    private static final String SELECT_BASE =
        "SELECT g.id, " +
        "DATE_FORMAT(g.fecha,'%d/%m/%Y %H:%i') AS fecha, " +                    // Fecha formateada para mostrar
        "DATE_FORMAT(g.fecha,'%Y-%m-%d') AS fecha_raw, " +                      // Fecha para input type="date"
        "g.descripcion, g.id_metodo_pago, mp.nombre AS metodo_pago, " +
        "pu.nombre_completo AS registrado_por, g.total, " +
        "COALESCE(e2.nombre, e1.nombre) AS nombre_emprendimiento, " +              // Emprendimiento: explícito o del usuario
        "r.nombre AS rol_registrador " +                                         // Rol del usuario que registró
        "FROM gastos g " +
        "JOIN metodo_pago mp    ON mp.id  = g.id_metodo_pago " +                 // Método de pago del gasto
        "JOIN usuarios u        ON u.id   = g.id_usuario " +                      // Usuario que registró
        "JOIN perfil_usuario pu ON pu.id_usuario = u.id " +                      // Perfil del usuario
        "JOIN roles r           ON r.id   = u.id_rol " +                         // Rol del usuario
        "LEFT JOIN emprendimientos e1 ON e1.id = u.id_emprendimiento " +         // Emprendimiento del usuario
        "LEFT JOIN emprendimientos e2 ON e2.id = g.id_emprendimiento ";           // Emprendimiento explícito del gasto

    // =========================================================
    // LISTADO - Obtener historial de gastos
    // =========================================================

    /**
     * Retorna el historial de gastos ordenado del más reciente al más antiguo.
     *
     * Aplica filtro por emprendimiento usando COALESCE para incluir
     * correctamente los registros del SuperAdmin con emprendimiento asignado.
     *
     * @param idEmprendimiento filtro de emprendimiento; 0 retorna todos
     * @return lista de gastos según el filtro aplicado
     * @throws SQLException si falla la consulta
     */
    public List<FilaGasto> listar(int idEmprendimiento) throws SQLException {
        boolean filtrar = idEmprendimiento > 0;
        String sql = SELECT_BASE +
            (filtrar ? "WHERE COALESCE(g.id_emprendimiento, u.id_emprendimiento) = ? " : "") +
            "ORDER BY g.fecha DESC";
        List<FilaGasto> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filtrar) ps.setInt(1, idEmprendimiento); // Asignar filtro de emprendimiento
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs)); // Mapear cada fila a DTO
            }
        }
        return lista;
    }

    /**
     * Lista todos los gastos sin filtro de emprendimiento.
     *
     * @return lista completa de gastos de todos los emprendimientos
     * @throws SQLException si falla la consulta
     */
    public List<FilaGasto> listar() throws SQLException { return listar(0); }

    // =========================================================
    // BÚSQUEDA - Consulta por ID
    // =========================================================

    /**
     * Busca un gasto por su ID para prellenar el modal de edición.
     *
     * Usa el query base para obtener todos los datos necesarios
     * incluyendo emprendimiento y rol del registrador.
     *
     * @param id ID del registro en la tabla gastos
     * @return objeto FilaGasto con todos sus campos
     * @throws SQLException si el gasto no existe o hay error de BD
     */
    public FilaGasto obtenerPorId(int id) throws SQLException {
        String sql = SELECT_BASE + "WHERE g.id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Gasto no encontrado: id=" + id);
                return mapear(rs);
            }
        }
    }

    // =========================================================
    // MÉTODOS DE PAGO - Para selectores del formulario
    // =========================================================

    /**
     * Retorna los métodos de pago para el selector del formulario.
     *
     * @return lista de pares [id, nombre] para el <select> del formulario
     * @throws SQLException si falla la consulta
     */
    public List<String[]> listarMetodosPago() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, nombre FROM metodo_pago ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                // Crear par [id, nombre] para cada método de pago
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    // =========================================================
    // REGISTRAR - Crear nuevo gasto
    // =========================================================

    /**
     * Registra un nuevo gasto en la tabla gastos.
     *
     * Permite asignar un emprendimiento explícito (para SuperAdmin)
     * o dejarlo nulo para usar el emprendimiento del usuario registrador.
     *
     * @param idUsuario        ID del usuario que registra el gasto
     * @param descripcion      descripción del gasto
     * @param total            monto del gasto (usando BigDecimal para precisión)
     * @param idMetodoPago     FK a metodo_pago
     * @param fecha            datetime en formato yyyy-MM-dd HH:mm:ss
     * @param idEmprendimiento emprendimiento destino; 0 si no aplica
     * @throws SQLException si falla la inserción
     */
    public void registrar(int idUsuario, String descripcion,
                          BigDecimal total, int idMetodoPago,
                          String fecha, int idEmprendimiento) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO gastos (id_usuario, descripcion, total, " +
                     "id_metodo_pago, fecha, id_emprendimiento) VALUES (?,?,?,?,?,?)")) {
            
            // INSERT INTO gastos: tabla principal para registro de egresos del negocio
            // Cada registro representa un gasto operativo o inversión del emprendimiento
            ps.setInt(1, idUsuario);                    // FK: usuario que registra el gasto (auditoría)
            ps.setString(2, descripcion);               // Descripción detallada del gasto
            ps.setBigDecimal(3, total);                 // Monto usando BigDecimal para precisión decimal
            ps.setInt(4, idMetodoPago);                  // FK: método de pago utilizado
            ps.setString(5, fecha);                       // Fecha y hora completas del gasto
            
            // Manejar emprendimiento opcional: puede ser NULL si es gasto general
            if (idEmprendimiento > 0) ps.setInt(6, idEmprendimiento); // Emprendimiento específico
            else                      ps.setNull(6, Types.INTEGER);       // Sin emprendimiento (general)
            
            ps.executeUpdate(); // Ejecutar inserción del gasto
        }
    }

    // =========================================================
    // EDITAR - Actualizar gasto existente
    // =========================================================

    /**
     * Actualiza un gasto existente en la tabla gastos.
     *
     * Si idEmprendimiento es mayor que cero, también actualiza el
     * emprendimiento explícito (solo permitido cuando el gasto fue
     * registrado originalmente por el SuperAdministrador).
     *
     * @param id               ID del registro en gastos
     * @param descripcion      nueva descripción
     * @param total            nuevo monto (BigDecimal para precisión)
     * @param idMetodoPago     nuevo método de pago
     * @param fecha            nueva fecha en formato yyyy-MM-dd HH:mm:ss
     * @param idEmprendimiento nuevo emprendimiento explícito; 0 para limpiar
     * @throws SQLException si falla la actualización
     */
    public void editar(int id, String descripcion, BigDecimal total,
                       int idMetodoPago, String fecha,
                       int idEmprendimiento) throws SQLException {
        // Construir SQL dinámico según si se actualiza el emprendimiento
        String sql = idEmprendimiento > 0
            ? "UPDATE gastos SET descripcion=?, total=?, id_metodo_pago=?, fecha=?, id_emprendimiento=? WHERE id=?"
            : "UPDATE gastos SET descripcion=?, total=?, id_metodo_pago=?, fecha=?, id_emprendimiento=NULL WHERE id=?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, descripcion);               // Nueva descripción
            ps.setBigDecimal(2, total);                 // Nuevo monto
            ps.setInt(3, idMetodoPago);                  // Nuevo método de pago
            ps.setString(4, fecha);                       // Nueva fecha
            if (idEmprendimiento > 0) { ps.setInt(5, idEmprendimiento); ps.setInt(6, id); }
            else                      { ps.setInt(5, id); }
            ps.executeUpdate();
        }
    }

    // =========================================================
    // HELPER - Mapeo de ResultSet a DTO
    // =========================================================

    /**
     * Mapea un ResultSet a un objeto FilaGasto.
     *
     * Convierte una fila del ResultSet en el DTO completo,
     * manejando valores nulos y determinando si fue registrado
     * por un SuperAdministrador.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto FilaGasto con todos los datos
     * @throws SQLException si hay error al acceder a los campos
     */
    private FilaGasto mapear(ResultSet rs) throws SQLException {
        FilaGasto f = new FilaGasto();
        f.id           = rs.getInt("id");
        f.fecha        = rs.getString("fecha");
        f.fechaRaw     = rs.getString("fecha_raw");
        f.descripcion  = rs.getString("descripcion");
        f.idMetodoPago = rs.getInt("id_metodo_pago");
        f.metodoPago   = rs.getString("metodo_pago");
        f.registradoPor = rs.getString("registrado_por");
        f.total        = rs.getBigDecimal("total");
        // Manejar valores nulos con try-catch para evitar errores
        try { f.nombreEmprendimiento    = rs.getString("nombre_emprendimiento"); } catch (Exception ignored) {}
        try { f.registradoPorSuperAdmin = "SuperAdministrador".equals(rs.getString("rol_registrador")); } catch (Exception ignored) {}
        return f;
    }
}
