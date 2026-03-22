package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EditarEmpleadoDAO;
import com.dulce_gestion.dao.EliminarEmpleadoDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * ============================================================
 * SERVLET: EliminarEmpleadoServlet
 * URL:     POST /empleados/eliminar
 * MÉTODOS: POST
 * ============================================================
 *
 * Inactiva un usuario (eliminación lógica).
 * Cambia el campo estado a 'Inactivo' — no borra el registro.
 * Esto preserva el historial de ventas y transacciones.
 *
 * Permisos:
 *   SuperAdministrador → puede inactivar Admin y Empleado
 *   Administrador      → puede inactivar solo Empleados
 */
@WebServlet("/empleados/eliminar")
public class EliminarEmpleadoServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath();

        // ── Paso 1: verificar sesión y rol ───────────────────────────────
        HttpSession session = request.getSession(false);
        Usuario solicitante = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (solicitante == null) { response.sendRedirect(ctx + "/login"); return; }

        String rolSolicitante = solicitante.getNombreRol();
        if (!"SuperAdministrador".equals(rolSolicitante) && !"Administrador".equals(rolSolicitante)) {
            response.sendRedirect(ctx + "/dashboard"); return;
        }

        // ── Paso 2: leer y validar el ID del usuario a inactivar ─────────
        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(ctx + "/empleados"); return;
        }

        int idUsuario;
        try { idUsuario = Integer.parseInt(idParam); }
        catch (NumberFormatException e) { response.sendRedirect(ctx + "/empleados"); return; }

        try {
            // ── Paso 3: verificar que el usuario objetivo existe ──────────
            Usuario objetivo = new EditarEmpleadoDAO().buscarPorId(idUsuario);
            if (objetivo == null) {
                response.sendRedirect(ctx + "/empleados?error=noexiste"); return;
            }

            // ── Paso 4: verificar permisos sobre el rol del objetivo ──────
            // SuperAdmin puede inactivar Admin y Empleado.
            // Admin solo puede inactivar Empleados.
            String rolObjetivo = objetivo.getNombreRol();
            boolean tienePermiso =
                ("SuperAdministrador".equals(rolSolicitante) &&
                    ("Administrador".equals(rolObjetivo) || "Empleado".equals(rolObjetivo)))
                ||
                ("Administrador".equals(rolSolicitante) && "Empleado".equals(rolObjetivo));

            if (!tienePermiso) {
                response.sendRedirect(ctx + "/empleados?error=sinpermiso"); return;
            }

            // ── Paso 5: inactivar mediante el DAO ─────────────────────────
            new EliminarEmpleadoDAO().inactivar(idUsuario);

            response.sendRedirect(ctx + "/empleados?exito=eliminado");

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(ctx + "/empleados?error=eliminacion");
        }
    }
}
