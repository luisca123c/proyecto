package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmpleadoDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * ============================================================
 * SERVLET: EmpleadosServlet
 * URL:     /empleados
 * MÉTODOS: GET
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Carga la lista de usuarios del sistema y la envía al JSP
 * para mostrarla en la tabla de gestión de empleados.
 *
 * La lista que se muestra DEPENDE DEL ROL del solicitante:
 *   SuperAdministrador → ve Administradores Y Empleados
 *   Administrador      → ve solo Empleados
 *   Empleado           → NO puede acceder a esta sección
 *
 * ¿POR QUÉ SE PASA "rolSolicitante" AL JSP?
 * -------------------------------------------
 * El JSP necesita saber el rol para:
 *   1. Mostrar/ocultar el botón "Nuevo empleado"
 *      (solo SuperAdmin puede crear Admins; Admin puede crear Empleados)
 *   2. Mostrar/ocultar los botones Editar y Eliminar por fila
 *      (según quién puede gestionar a quién)
 *   3. Renderizar la etiqueta correcta en la columna de rol
 *
 * ¿QUÉ PASA CON LOS PARÁMETROS ?exito= Y ?error= EN LA URL?
 * -----------------------------------------------------------
 * Cuando NuevoEmpleadoServlet, EditarEmpleadoServlet o EliminarEmpleadoServlet
 * terminan con éxito o error, redirigen a:
 *   /empleados?exito=creado
 *   /empleados?exito=editado
 *   /empleados?exito=eliminado
 *   /empleados?error=eliminacion
 *
 * Este servlet no hace nada especial con esos parámetros — los pasa
 * automáticamente al JSP a través del request. El JSP los lee con
 * request.getParameter("exito") y muestra la notificación correspondiente.
 */
@WebServlet("/empleados")
public class EmpleadosServlet extends HttpServlet {

    /**
     * GET /empleados → carga la lista filtrada por rol y hace forward al JSP.
     *
     * FLUJO PASO A PASO:
     *
     * 1. Verificar que hay sesión activa y extraer el usuario.
     * 2. Verificar que el rol permite ver esta sección (Admin o SuperAdmin).
     *    Los Empleados son redirigidos al dashboard.
     * 3. Llamar a EmpleadoDAO.listarSegunRol() — el DAO aplica el filtro SQL.
     * 4. Poner la lista y el rol como atributos del request.
     * 5. Forward al JSP lista.jsp.
     *
     * @param request  contiene la sesión activa y posibles parámetros ?exito= / ?error=
     * @param response para redirigir si no hay permiso, o para el forward al JSP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: obtener el usuario de la sesión ─────────────────────
        HttpSession session = request.getSession(false);
        // Ternario: evita NullPointerException si session es null
        Usuario solicitante = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (solicitante == null) {
            // Sin sesión → redirigir al login
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // ── Paso 2: verificar que el rol tiene permiso ──────────────────
        String rol = solicitante.getNombreRol();

        // Los Empleados no pueden gestionar otros usuarios → redirigir al dashboard
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        // ── Paso 3: consultar la BD con el filtro de rol ─────────────────
        EmpleadoDAO dao = new EmpleadoDAO();
        List<Usuario> lista;

        try {
            // El DAO aplica la cláusula WHERE correcta según el rol:
            //   SuperAdmin → WHERE rol IN ('Administrador', 'Empleado')
            //   Admin      → WHERE rol = 'Empleado'
            lista = dao.listarSegunRol(rol);

        } catch (SQLException e) {
            e.printStackTrace(); // Log en consola de Tomcat
            // En caso de error, mostrar la página vacía con mensaje de error
            request.setAttribute("errorEmpleados", "Error al cargar los empleados.");
            lista = List.of(); // Lista vacía inmutable para evitar NullPointerException en el JSP
        }

        // ── Paso 4: poner los datos en el request para el JSP ───────────
        request.setAttribute("empleados",      lista);    // La lista de usuarios para la tabla
        request.setAttribute("rolSolicitante", rol);      // Para mostrar/ocultar botones en el JSP

        // ── Paso 5: forward al JSP ──────────────────────────────────────
        // Los parámetros ?exito= y ?error= de la URL también llegan al JSP
        // a través del mismo request (request.getParameter() funciona en el JSP).
        request.getRequestDispatcher("/WEB-INF/jsp/empleados/lista.jsp")
               .forward(request, response);
    }
}
