package com.dulce_gestion.dao;

import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * DAO para listar usuarios según el rol del solicitante.
 * SuperAdministrador → ve Administradores y Empleados.
 * Administrador      → ve solo Empleados.
 */
public class EmpleadoDAO {

    /*
     * Retorna la lista de usuarios visibles para el rol dado.
     * rolSolicitante: nombre del rol de quien hace la consulta.
     */
    public List<Usuario> listarSegunRol(String rolSolicitante) throws SQLException {

        String sql;

        if ("SuperAdministrador".equals(rolSolicitante)) {
            // SuperAdmin ve Admins y Empleados (no otros SuperAdmins)
            sql = """
                    SELECT u.id,
                           c.correo,
                           u.estado,
                           u.id_rol,
                           r.nombre    AS nombre_rol,
                           p.nombre_completo
                    FROM usuarios u
                    JOIN correos        c ON c.id         = u.id_correo
                    JOIN roles          r ON r.id         = u.id_rol
                    JOIN perfil_usuario p ON p.id_usuario = u.id
                    WHERE r.nombre IN ('Administrador', 'Empleado')
                    ORDER BY r.nombre, p.nombre_completo
                    """;
        } else {
            // Admin solo ve Empleados
            sql = """
                    SELECT u.id,
                           c.correo,
                           u.estado,
                           u.id_rol,
                           r.nombre    AS nombre_rol,
                           p.nombre_completo
                    FROM usuarios u
                    JOIN correos        c ON c.id         = u.id_correo
                    JOIN roles          r ON r.id         = u.id_rol
                    JOIN perfil_usuario p ON p.id_usuario = u.id
                    WHERE r.nombre = 'Empleado'
                    ORDER BY p.nombre_completo
                    """;
        }

        List<Usuario> lista = new ArrayList<>();

        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setCorreo(rs.getString("correo"));
                u.setEstado(rs.getString("estado"));
                u.setIdRol(rs.getInt("id_rol"));
                u.setNombreRol(rs.getString("nombre_rol"));
                u.setNombreCompleto(rs.getString("nombre_completo"));
                lista.add(u);
            }
        }

        return lista;
    }
}
