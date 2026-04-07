package com.dulce_gestion.dao;

import com.dulce_gestion.utils.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para el módulo de Compras de Insumos.
 *
 * Esta clase maneja todas las operaciones CRUD (Crear, Leer, Actualizar, Eliminar)
 * relacionadas con las compras de insumos en el sistema. Funciona como una capa
 * de abstracción entre la lógica de negocio y la base de datos.
 *
 * Tabla principal: {@code compras_insumos}. El emprendimiento se resuelve con
 * {@code COALESCE(e2.nombre, e1.nombre)}: si el SuperAdministrador asignó un
 * emprendimiento explícito al registrar, ese tiene precedencia; de lo contrario
 * se usa el del usuario registrador.
 *
 * Tablas escritas: {@code compras_insumos}.<br>
 * Tablas leídas: {@code metodo_pago}, {@code usuarios}, {@code perfil_usuario},
 * {@code emprendimientos}, {@code roles}.
 */
public class ComprasDAO {

    // =========================================================
    // MODELO INTERNO - Clases que representan los datos
    // =========================================================

    /**
     * Proyección plana de una compra de insumos para la vista del historial.
     * constructor: FilaCompra(int id, String fecha, String fechaRaw, String descripcion, 
     * String metodoPago, int idMetodoPago, String registradoPor, BigDecimal total, String 
     * nombreEmprendimiento, boolean registradoPorSuperAdmin)
     */
    public static class FilaCompra {
        public int        id;
        /** Fecha formateada {@code dd/MM/yyyy HH:mm} para mostrar en tabla. */
        public String     fecha;
        /** Fecha en formato {@code yyyy-MM-dd} para usar en {@code <input type="date">}. */
        public String     fechaRaw;
        public String     descripcion;
        public String     metodoPago;
        public int        idMetodoPago;
        public String     registradoPor;
        /** BigDecimal es para manejar valores decimales con precisión */
        public BigDecimal total;
        public String     nombreEmprendimiento;
        /** {@code true} cuando quien registró la compra tiene rol SuperAdministrador. */
        public boolean    registradoPorSuperAdmin;
    }

    /**
     * Consulta SQL base para obtener datos completos de compras.
     * 
     * Esta consulta incluye todos los JOIN necesarios para obtener:
     * - Datos básicos de la compra (id, fecha, descripción, total)
     * - Método de pago asociado
     * - Usuario que registró la compra
     * - Emprendimiento (con lógica de prioridad)
     * - Rol del usuario (para identificar SuperAdministradores)
     */
    private static final String SELECT_BASE =
        "SELECT ci.id, " +
        "DATE_FORMAT(ci.fecha_compra,'%d/%m/%Y %H:%i') AS fecha, " +
        "DATE_FORMAT(ci.fecha_compra,'%Y-%m-%d') AS fecha_raw, " +
        "ci.descripcion, mp.nombre AS metodo_pago, ci.id_metodo_pago, " +
        "pu.nombre_completo AS registrado_por, ci.total, " +
        /** COALESCE: Función SQL que devuelve el primer valor NO NULL de la lista.
         * En este caso: si e2.nombre (emprendimiento explícito) no es NULL, lo usa;
         * si es NULL, usa e1.nombre (emprendimiento del usuario).
         * Esto permite que el SuperAdministrador pueda asignar un emprendimiento
         * diferente al del usuario que registra la compra. */
        "COALESCE(e2.nombre, e1.nombre) AS nombre_emprendimiento, " +
        "r.nombre AS rol_registrador " +
        "FROM compras_insumos ci " +
        "JOIN metodo_pago mp    ON mp.id  = ci.id_metodo_pago " +
        "JOIN usuarios u        ON u.id   = ci.id_usuario " +
        "JOIN perfil_usuario pu ON pu.id_usuario = u.id " +
        "JOIN roles r           ON r.id   = u.id_rol " +
        /** LEFT JOIN: A diferencia del INNER JOIN que requiere coincidencia en AMBAS tablas,
         * el LEFT JOIN retorna todos los registros de la tabla izquierda (compras_insumos)
         * aunque no tengan correspondencia en la tabla derecha (emprendimientos).
         * Esto es crucial porque una compra puede no tener emprendimiento asignado. */
        "LEFT JOIN emprendimientos e1 ON e1.id = u.id_emprendimiento " +
        /** e1 = emprendimiento del usuario que registra la compra.
         * Se usa como respaldo cuando no hay emprendimiento explícito asignado. */
        "LEFT JOIN emprendimientos e2 ON e2.id = ci.id_emprendimiento ";

    // =========================================================
    // OPERACIONES DE LECTURA - Métodos para consultar datos
    // =========================================================

    /**
     * Retorna el historial de compras ordenado del más reciente al más antiguo.
     *
     * Este método permite filtrar compras por emprendimiento específico
     * u obtener todas las compras si se pasa 0 como filtro. Utiliza la
     * consulta base SELECT_BASE para obtener todos los datos necesarios
     * en una sola consulta optimizada.
     *
     * @param idEmprendimiento filtro de emprendimiento; {@code 0} retorna todos.
     * @return lista de compras según el filtro aplicado.
     * @throws SQLException si falla la consulta.
     */
    public List<FilaCompra> listar(int idEmprendimiento) throws SQLException {
        // Determinar si se aplica filtro por emprendimiento
        boolean filtrar = idEmprendimiento > 0;
        
        // Construir SQL dinámico: agrega WHERE solo si hay filtro
        String sql = SELECT_BASE +
            /** COALESCE en WHERE: Similar al del SELECT, pero aquí filtra.
             * Busca compras donde el emprendimiento explícito (ci.id_emprendimiento)
             * o el emprendimiento del usuario (u.id_emprendimiento) coincidan
             * con el ID proporcionado. */
            (filtrar ? "WHERE COALESCE(ci.id_emprendimiento, u.id_emprendimiento) = ? " : "") +
            "ORDER BY ci.fecha_compra DESC"; // Orden más reciente primero
            
        List<FilaCompra> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Asignar parámetro solo si hay filtro
            if (filtrar) ps.setInt(1, idEmprendimiento);
            
            try (ResultSet rs = ps.executeQuery()) {
                // Convertir cada fila del ResultSet a objeto FilaCompra
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Lista todas las compras sin filtro de emprendimiento. */
    public List<FilaCompra> listar() throws SQLException { return listar(0); }

    // =========================================================
    // OBTENER UNO POR ID
    // =========================================================

    /**
     * Busca una compra por su ID para prellenar el modal de edición.
     *
     * @param id ID del registro en {@code compras_insumos}.
     * @return objeto {@link FilaCompra} con todos sus campos, o {@code null} si no existe.
     * @throws SQLException si falla la consulta.
     */
    public FilaCompra obtenerPorId(int id) throws SQLException {
        // Usar la consulta base y agregar filtro por ID específico
        String sql = SELECT_BASE + "WHERE ci.id = ?";
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, id); // Parámetro ID para buscar la compra específica
            
            try (ResultSet rs = ps.executeQuery()) {
                // Retornar objeto mapeado si encuentra registro, sino null
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    // =========================================================
    // MÉTODOS DE PAGO
    // =========================================================

    /**
     * Retorna los métodos de pago para el {@code <select>} del formulario.
     *
     * Este método carga el catálogo de métodos de pago disponibles
     * en el sistema. El resultado se usa para poblar el campo desplegable
     * en el formulario de registro/edición de compras.
     *
     * @return lista de pares {@code [id, nombre]} para el select HTML.
     * @throws SQLException si falla la consulta.
     */
    public List<String[]> listarMetodosPago() throws SQLException {
        List<String[]> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, nombre FROM metodo_pago ORDER BY id"); // Orden por ID
             ResultSet rs = ps.executeQuery()) {
            
            // Convertir cada fila a un arreglo [id, nombre] para el select HTML
            while (rs.next())
                lista.add(new String[]{rs.getString("id"), rs.getString("nombre")});
        }
        return lista;
    }

    // =========================================================
    // OPERACIONES DE ESCRITURA - Métodos para crear y modificar
    // =========================================================

    /**
     * Registra una nueva compra de insumos en {@code compras_insumos}.
     *
     * Este método crea un nuevo registro de compra con todos los datos
     * proporcionados. El emprendimiento es opcional (puede ser null si
     * no aplica). La fecha debe venir en formato datetime completo.
     *
     * Nota importante: El ID de usuario se registra
     * para auditoría y para determinar el emprendimiento por defecto
     * si no se especifica uno explícitamente.
     *
     * @param idUsuario        ID del usuario que registra la compra (para auditoría).
     * @param descripcion      descripción detallada de qué se compró.
     * @param total            monto total de la compra (usar BigDecimal para precisión).
     * @param idMetodoPago     FK a {@code metodo_pago} (debe existir en la tabla).
     * @param fechaDatetime    fecha y hora en formato {@code yyyy-MM-dd HH:mm:ss}.
     * @param idEmprendimiento emprendimiento destino; {@code 0} si no aplica.
     * @throws SQLException si falla la inserción o viola constraints.
     */
    public void registrar(int idUsuario, String descripcion,
                          BigDecimal total, int idMetodoPago,
                          String fechaDatetime, int idEmprendimiento) throws SQLException {
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO compras_insumos " +
                     "(id_usuario, descripcion, total, id_metodo_pago, fecha_compra, id_emprendimiento) " +
                     "VALUES (?,?,?,?,?,?)")) {
            
            // INSERT INTO compras_insumos: tabla principal para registro de compras de insumos
            // Cada registro representa una compra de materiales para el negocio
            ps.setInt(1, idUsuario);        // FK: usuario que realiza la compra (auditoría)
            ps.setString(2, descripcion);    // Descripción detallada de qué se compró
            ps.setBigDecimal(3, total);      // Monto total usando BigDecimal para precisión decimal
            ps.setInt(4, idMetodoPago);     // FK: método de pago utilizado
            ps.setString(5, fechaDatetime);  // Fecha y hora completas en formato datetime
            
            // Manejar emprendimiento opcional: puede ser NULL si no aplica
            if (idEmprendimiento > 0) ps.setInt(6, idEmprendimiento);
            // type INTEGER para NULL
            else                      ps.setNull(6, Types.INTEGER);
            
            ps.executeUpdate(); // Ejecutar inserción
        }
    }

    // =========================================================
    // ACTUALIZACIÓN - Modificar registros existentes
    // =========================================================

    /**
     * Actualiza una compra de insumos existente en {@code compras_insumos}.
     *
     * <p>Si {@code idEmprendimiento} es mayor que cero, también actualiza el
     * emprendimiento explícito (solo permitido cuando la compra fue registrada
     * originalmente por el SuperAdministrador).</p>
     *
     * @param id               ID del registro en {@code compras_insumos}.
     * @param descripcion      nueva descripción.
     * @param total            nuevo monto.
     * @param idMetodoPago     nuevo método de pago.
     * @param fechaDatetime    nueva fecha en formato {@code yyyy-MM-dd HH:mm:ss}.
     * @param idEmprendimiento nuevo emprendimiento explícito; {@code 0} para limpiar.
     * @throws SQLException si falla la actualización.
     */
    public void editar(int id, String descripcion,
                       BigDecimal total, int idMetodoPago,
                       String fechaDatetime, int idEmprendimiento) throws SQLException {
        // Construir SQL dinámico según si se actualiza emprendimiento
        String sql = idEmprendimiento > 0
            ? "UPDATE compras_insumos SET descripcion=?, total=?, id_metodo_pago=?, fecha_compra=?, id_emprendimiento=? WHERE id=?"
            : "UPDATE compras_insumos SET descripcion=?, total=?, id_metodo_pago=?, fecha_compra=?, id_emprendimiento=NULL WHERE id=?";
            
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            // Asignar parámetros comunes para la actualización
            ps.setString(1, descripcion);    // Nueva descripción
            ps.setBigDecimal(2, total);      // Nuevo monto
            ps.setInt(3, idMetodoPago);     // Nuevo método de pago
            ps.setString(4, fechaDatetime);  // Nueva fecha
            
            // Manejar parámetros diferentes según el SQL generado
            if (idEmprendimiento > 0) { 
                ps.setInt(5, idEmprendimiento); // Nuevo emprendimiento
                ps.setInt(6, id);              // ID del registro a actualizar
            } else { 
                ps.setInt(5, id); // Solo ID cuando se limpia emprendimiento
            }
            
            ps.executeUpdate(); // Ejecutar actualización
        }
    }

    // =========================================================
    // UTILIDADES INTERNAS - Métodos helper privados
    // =========================================================

    /**
     * Convierte un ResultSet a un objeto FilaCompra.
     * 
     * Este método privado extrae los datos de la fila actual del ResultSet
     * y los mapea a los campos correspondientes del objeto FilaCompra.
     * Incluye manejo seguro de valores nulos para evitar excepciones.
     * 
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto FilaCompra con los datos de la fila
     * @throws SQLException si hay error al acceder a los datos del ResultSet
     */
    private FilaCompra mapear(ResultSet rs) throws SQLException {
        FilaCompra fc = new FilaCompra();
        
        // Mapear campos obligatorios (siempre existen)
        fc.id           = rs.getInt("id");
        fc.fecha        = rs.getString("fecha");
        fc.fechaRaw     = rs.getString("fecha_raw");
        fc.descripcion  = rs.getString("descripcion");
        fc.metodoPago   = rs.getString("metodo_pago");
        fc.idMetodoPago = rs.getInt("id_metodo_pago");
        fc.registradoPor = rs.getString("registrado_por");
        fc.total        = rs.getBigDecimal("total");
        
        // Mapear campos opcionales con manejo seguro de NULL
        try { 
            fc.nombreEmprendimiento = rs.getString("nombre_emprendimiento"); 
        } catch (Exception ignored) { 
            /** Si el ResultSet devuelve NULL para nombre_emprendimiento,
             * getString() lanza SQLException. El catch silencioso evita
             * que el programa falle y deja el campo como null. */
            fc.nombreEmprendimiento = null; // Valor por defecto si es NULL
        }
        
        try { 
            fc.registradoPorSuperAdmin = "SuperAdministrador".equals(rs.getString("rol_registrador")); 
        } catch (Exception ignored) { 
            fc.registradoPorSuperAdmin = false; // Valor por defecto si es NULL
        }
        
        return fc;
    }
}
