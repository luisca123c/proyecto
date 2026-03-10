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
 * Tablas del JOIN:    correos, roles, perfil_usuario
 * Usado por:          EmpleadosServlet
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Lista los usuarios del sistema filtrando según el rol del solicitante.
 * El filtro se aplica directamente en SQL (no en Java) para que la BD
 * haga el trabajo pesado y solo viaje por la red lo que se necesita.
 *
 * ¿POR QUÉ EL FILTRO ESTÁ EN EL DAO Y NO EN EL SERVLET?
 * -------------------------------------------------------
 * EmpleadosServlet podría cargar todos los usuarios y luego filtrar en Java.
 * Eso sería ineficiente: si hay 1.000 usuarios, se traerían los 1.000
 * para quedarse solo con 50. Poniendo el WHERE en el SQL, la BD devuelve
 * únicamente las filas necesarias.
 *
 * Además, si la lógica de "quién ve a quién" cambia (ej: agregar un nuevo
 * rol), solo hay que modificar este DAO, no el Servlet.
 */
public class EmpleadoDAO {

    /**
     * Retorna la lista de usuarios visibles para el rol dado.
     *
     * REGLA DE VISIBILIDAD:
     * ----------------------
     *   SuperAdministrador → ve Administradores Y Empleados
     *                        (no ve a otros SuperAdmins)
     *   Administrador      → ve solo Empleados
     *   Empleado           → no debería llegar aquí (el Servlet lo bloquea)
     *
     * ¿POR QUÉ DOS QUERIES DISTINTAS EN LUGAR DE UN PARÁMETRO?
     * ----------------------------------------------------------
     * La cláusula WHERE varía estructuralmente según el rol:
     *   SuperAdmin → WHERE r.nombre IN ('Administrador', 'Empleado')
     *   Admin      → WHERE r.nombre = 'Empleado'
     *
     * No es solo un valor diferente en el mismo campo, son condiciones
     * distintas. Por legibilidad se escribe cada query completa en lugar
     * de construirla con concatenación condicional (más propenso a bugs).
     *
     * @param rolSolicitante nombre del rol del usuario que hace la consulta
     *                       ("SuperAdministrador" o "Administrador")
     * @return               lista de usuarios visibles para ese rol,
     *                       ordenada por rol y luego por nombre
     * @throws SQLException  si hay error al consultar la BD
     */
    public List<Usuario> listarSegunRol(String rolSolicitante) throws SQLException {

        String sql;

        if ("SuperAdministrador".equals(rolSolicitante)) {
            // SuperAdmin ve a todos menos a otros SuperAdmins
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

        // PreparedStatement sin parámetros "?" porque los valores están
        // embebidos de forma segura como literales string en el SQL
        try (Connection con = DB.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Cada fila del ResultSet se convierte en un objeto Usuario
                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setCorreo(rs.getString("correo"));
                u.setEstado(rs.getString("estado"));         // "Activo" o "Inactivo"
                u.setIdRol(rs.getInt("id_rol"));
                u.setNombreRol(rs.getString("nombre_rol"));  // "Administrador" o "Empleado"
                u.setNombreCompleto(rs.getString("nombre_completo"));
                lista.add(u);
            }
        }

        return lista;
    }
}
