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
 * ============================================================
 * DAO: EmpleadoDAO
 * Tabla principal:    usuarios
 * Tablas del JOIN:    correos, roles, perfil_usuario, emprendimientos
 * Usado por:          EmpleadosServlet
 * ============================================================
 *
 * REGLAS DE VISIBILIDAD:
 *   SuperAdministrador + idEmp=0  -> todos los Admins y Empleados
 *   SuperAdministrador + idEmp>0  -> Admins y Empleados del emprendimiento indicado
 *   Administrador                 -> solo Empleados de su propio emprendimiento
 *   Empleado                      -> bloqueado en el Servlet (nunca llega aqui)
 */
public class EmpleadoDAO {

    // Query base con JOIN a emprendimientos para obtener el nombre
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
     * @param rolSolicitante    "SuperAdministrador" o "Administrador"
     * @param idEmprendimiento  SuperAdmin: 0=todos, >0=filtrar por ese emprendimiento
     *                          Admin: siempre su propio id
     */
    public List<Usuario> listarFiltrado(String rolSolicitante, int idEmprendimiento)
            throws SQLException {

        String sql;
        boolean filtrarEmp = false;

        if ("SuperAdministrador".equals(rolSolicitante)) {
            if (idEmprendimiento > 0) {
                sql = SELECT_BASE +
                      "WHERE r.nombre IN ('Administrador', 'Empleado') " +
                      "AND u.id_emprendimiento = ? " +
                      "ORDER BY r.nombre, p.nombre_completo";
                filtrarEmp = true;
            } else {
                // Todos los emprendimientos
                sql = SELECT_BASE +
                      "WHERE r.nombre IN ('Administrador', 'Empleado') " +
                      "ORDER BY e.nombre, r.nombre, p.nombre_completo";
            }
        } else {
            // Admin: solo Empleados de su emprendimiento
            sql = SELECT_BASE +
                  "WHERE r.nombre = 'Empleado' " +
                  "AND u.id_emprendimiento = ? " +
                  "ORDER BY p.nombre_completo";
            filtrarEmp = true;
        }

        List<Usuario> lista = new ArrayList<>();
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (filtrarEmp) ps.setInt(1, idEmprendimiento);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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

    /** Compatibilidad con codigo anterior. */
    public List<Usuario> listarSegunRol(String rolSolicitante) throws SQLException {
        return listarFiltrado(rolSolicitante, 0);
    }
}
