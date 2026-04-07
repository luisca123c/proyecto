package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO especializado en la consulta y filtrado de empleados y administradores.
 *
 * Esta clase maneja el listado de usuarios con reglas de visibilidad
 * según el rol del solicitante y emprendimiento especificado.
 *
 * Tabla principal: usuarios
 * Tablas del JOIN: correos, roles, perfil_usuario, emprendimientos
 * Usado por: EmpleadosServlet
 *
 * Características importantes:
 * - Filtros por rol y emprendimiento
 * - Reglas de visibilidad según tipo de usuario
 * - Consultas complejas con JOINs múltiples
 * - Flexibilidad para diferentes tipos de acceso
 *
 * REGLAS DE VISIBILIDAD:
 *   SuperAdministrador + idEmp=0  -> todos los Admins y Empleados
 *   SuperAdministrador + idEmp>0  -> Admins y Empleados del emprendimiento indicado
 *   Administrador                 -> solo Empleados de su propio emprendimiento
 *   Empleado                      -> bloqueado en el Servlet (nunca llega aqui)
 */
public class EmpleadoDAO {

    // Query base con JOIN a todas las tablas necesarias para obtener información completa
    private static final String SELECT_BASE =
            "SELECT u.id, " +
            "       c.correo, " +
            "       u.estado, " +
            "       u.id_rol, " +
            "       r.nombre         AS nombre_rol, " +
            "       p.nombre_completo, " +
            "       u.id_emprendimiento, " +
            "       e.nombre         AS nombre_emprendimiento " +
            "FROM usuarios u " +
            "JOIN correos           c ON c.id         = u.id_correo " +
            "JOIN roles             r ON r.id         = u.id_rol " +
            "JOIN perfil_usuario    p ON p.id_usuario = u.id " +
            "JOIN emprendimientos   e ON e.id         = u.id_emprendimiento ";

    /**
     * Lista empleados/admins visibles para el solicitante con filtro opcional
     * de emprendimiento.
     *
     * Este método implementa las reglas de visibilidad según el rol del solicitante:
     *
     * SuperAdministrador:
     * - idEmprendimiento = 0: ve todos los Admins y Empleados de todos los emprendimientos
     * - idEmprendimiento > 0: ve Admins y Empleados solo de ese emprendimiento
     *
     * Administrador:
     * - Solo puede ver Empleados de su propio emprendimiento
     * - No puede ver otros Administradores
     *
     * @param rolSolicitante    rol del usuario que hace la consulta ("SuperAdministrador" o "Administrador")
     * @param idEmprendimiento  para SuperAdmin: 0=todos, >0=filtrar por ese emprendimiento
     *                          para Admin: siempre su propio id de emprendimiento
     * @return lista de usuarios visibles según las reglas de acceso
     * @throws SQLException si hay error en la consulta
     */
    public List<Usuario> listarFiltrado(String rolSolicitante, int idEmprendimiento)
            throws SQLException {

        String sql;
        boolean filtrarEmp = false;

        // Construir SQL dinámico según el rol del solicitante
        if ("SuperAdministrador".equals(rolSolicitante)) {
            if (idEmprendimiento > 0) {
                // SuperAdmin filtrando por emprendimiento específico
                sql = SELECT_BASE +
                      "WHERE r.nombre IN ('Administrador', 'Empleado') " +
                      "AND u.id_emprendimiento = ? " +
                      "ORDER BY r.nombre, p.nombre_completo";
                filtrarEmp = true;
            } else {
                // SuperAdmin viendo todos los emprendimientos
                sql = SELECT_BASE +
                      "WHERE r.nombre IN ('Administrador', 'Empleado') " +
                      "ORDER BY e.nombre, r.nombre, p.nombre_completo";
            }
        } else {
            // Administrador: solo puede ver Empleados de su emprendimiento
            sql = SELECT_BASE +
                  "WHERE r.nombre = 'Empleado' " +
                  "AND u.id_emprendimiento = ? " +
                  "ORDER BY p.nombre_completo";
            filtrarEmp = true;
        }

        List<Usuario> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Asignar parámetro de emprendimiento si se necesita filtrar
            if (filtrarEmp) ps.setInt(1, idEmprendimiento);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Mapear ResultSet a objeto Usuario con todos los campos
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setCorreo(rs.getString("correo"));
                    u.setEstado(rs.getString("estado"));
                    u.setIdRol(rs.getInt("id_rol"));
                    u.setNombreRol(rs.getString("nombre_rol"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setIdEmprendimiento(rs.getInt("id_emprendimiento"));
                    u.setNombreEmprendimiento(rs.getString("nombre_emprendimiento"));
                    lista.add(u);
                }
            }
        }
        return lista;
    }

    // =========================================================
    // RETROCOMPATIBILIDAD - Método para código antiguo
    // =========================================================
    
    /**
     * Método de retrocompatibilidad para código anterior.
     *
     * Permite mantener compatibilidad con código que usa el método
     * antiguo listarSegunRol() llamando al nuevo método listarFiltrado()
     * con idEmprendimiento = 0 (todos los emprendimientos).
     *
     * @param rolSolicitante rol del solicitante
     * @return lista de usuarios según el rol
     * @throws SQLException si hay error en la consulta
     */
    public List<Usuario> listarSegunRol(String rolSolicitante) throws SQLException {
        return listarFiltrado(rolSolicitante, 0);
    }
}
