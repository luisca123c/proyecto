package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EditarEmpleadoDAO;
import com.dulce_gestion.dao.EliminarEmpleadoDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/*
 * Servlet para POST /empleados/eliminar
 *
 * Solo acepta POST (no GET) para evitar eliminaciones accidentales por URL.
 * SuperAdmin puede eliminar Admins y Empleados.
 * Admin solo puede eliminar Empleados.
 *
 * Si el usuario tiene compras registradas, la BD rechaza la eliminación
 * y se redirige a la lista con un mensaje de error.
 */
public class EliminarEmpleadoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath();

        // Verificar que hay sesión activa y que el rol tiene permiso
        HttpSession session = request.getSession(false);
        Usuario solicitante = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (solicitante == null) {
            response.sendRedirect(ctx + "/login");
            return;
        }

        String rolSol = solicitante.getNombreRol();
        if (!"SuperAdministrador".equals(rolSol) && !"Administrador".equals(rolSol)) {
            response.sendRedirect(ctx + "/dashboard");
            return;
        }

        // Leer el ID del usuario a eliminar
        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(ctx + "/empleados");
            return;
        }

        int idUsuario;
        try {
            idUsuario = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.sendRedirect(ctx + "/empleados");
            return;
        }

        try {
            // Verificar que el solicitante tiene permiso para eliminar a este usuario
            EditarEmpleadoDAO buscarDao = new EditarEmpleadoDAO();
            Usuario objetivo = buscarDao.buscarPorId(idUsuario);

            if (objetivo == null) {
                response.sendRedirect(ctx + "/empleados?error=noexiste");
                return;
            }

            // Verificar permisos según el rol
            String rolObj = objetivo.getNombreRol();
            boolean tienePermiso =
                ("SuperAdministrador".equals(rolSol) &&
                    ("Administrador".equals(rolObj) || "Empleado".equals(rolObj)))
                ||
                ("Administrador".equals(rolSol) && "Empleado".equals(rolObj));

            if (!tienePermiso) {
                response.sendRedirect(ctx + "/empleados?error=sinpermiso");
                return;
            }

            // Eliminar el usuario
            new EliminarEmpleadoDAO().eliminar(idUsuario);
            response.sendRedirect(ctx + "/empleados?exito=eliminado");

        } catch (SQLException e) {
            // Si la BD rechaza por tener compras registradas u otro error
            e.printStackTrace();
            response.sendRedirect(ctx + "/empleados?error=eliminacion");
        }
    }
}
