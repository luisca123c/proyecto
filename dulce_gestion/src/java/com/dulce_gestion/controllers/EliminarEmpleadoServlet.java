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

/**
 * ============================================================
 * SERVLET: EliminarEmpleadoServlet
 * URL:     /empleados/eliminar
 * MÉTODOS: POST (solo POST — sin GET)
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Elimina un usuario del sistema. Recibe el ID por POST, verifica
 * permisos, y llama al DAO para borrar el usuario y todos sus datos.
 *
 * ¿POR QUÉ SOLO POST Y NO GET?
 * --------------------------------
 * Si hubiera un GET /empleados/eliminar?id=5, cualquiera que
 * tuviera acceso a ese enlace (un bot, una etiqueta <img>, etc.)
 * podría eliminar usuarios accidentalmente con solo cargar la URL.
 *
 * Al requerir POST, la eliminación solo puede ocurrir con un
 * formulario HTML que el usuario haya enviado explícitamente
 * (clic en "Sí, eliminar" del modal de confirmación).
 *
 * ¿QUIÉN PUEDE ELIMINAR A QUIÉN?
 * --------------------------------
 *   SuperAdministrador → puede eliminar Administradores y Empleados
 *   Administrador      → puede eliminar solo Empleados
 *   Empleado           → no puede eliminar a nadie
 *
 * ¿QUÉ PASA SI EL USUARIO TIENE COMPRAS?
 * ----------------------------------------
 * La tabla detalle_compra tiene una FK hacia usuarios con
 * ON DELETE RESTRICT. Si el usuario tiene compras registradas,
 * la BD rechaza el DELETE con SQLException.
 *
 * Este servlet captura esa excepción y redirige con ?error=eliminacion
 * para que el JSP muestre un mensaje explicando por qué no se pudo eliminar.
 *
 * ¿POR QUÉ SE VERIFICA EL OBJETIVO ANTES DE ELIMINAR?
 * -----------------------------------------------------
 * El ID llega como parámetro de un formulario. Un usuario malintencionado
 * podría manipular ese ID para intentar eliminar un usuario para el que
 * no tiene permiso (ej: un Admin intentando borrar a un SuperAdmin).
 *
 * La verificación de permisos en el servidor (no en el JSP) es la única
 * defensa real contra este tipo de manipulación.
 */
public class EliminarEmpleadoServlet extends HttpServlet {

    /**
     * POST /empleados/eliminar → verifica permisos y elimina el usuario.
     *
     * FLUJO PASO A PASO:
     *
     * Paso 1 — Verificar sesión activa y rol del solicitante.
     *   Solo Admin y SuperAdmin pueden llegar aquí.
     *   Los Empleados son redirigidos al dashboard.
     *
     * Paso 2 — Leer y parsear el parámetro "id" del formulario.
     *   Si falta o no es número → redirigir a la lista.
     *
     * Paso 3 — Buscar el usuario objetivo en la BD.
     *   Si no existe → redirigir con ?error=noexiste.
     *
     * Paso 4 — Verificar permisos cruzados.
     *   ¿Puede este solicitante eliminar a este objetivo?
     *   Si no → redirigir con ?error=sinpermiso.
     *
     * Paso 5 — Llamar a EliminarEmpleadoDAO.eliminar().
     *   El DAO elimina en transacción: perfil → teléfono → correo (CASCADE).
     *
     * Paso 6 — Redirigir según resultado.
     *   Éxito → /empleados?exito=eliminado
     *   Error de BD (ej: tiene compras) → /empleados?error=eliminacion
     *
     * @param request  contiene la sesión y el parámetro "id" del form POST
     * @param response para redirigir al resultado
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath(); // "/dulce_gestion" — prefijo de todas las URLs

        // ── Paso 1: verificar sesión y rol ───────────────────────────────
        HttpSession session = request.getSession(false);
        // Extraer el usuario en sesión (null si no hay sesión)
        Usuario solicitante = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (solicitante == null) {
            // Sin sesión → redirigir al login
            response.sendRedirect(ctx + "/login");
            return;
        }

        String rolSol = solicitante.getNombreRol();
        if (!"SuperAdministrador".equals(rolSol) && !"Administrador".equals(rolSol)) {
            // Empleado intentando eliminar → redirigir al inicio
            response.sendRedirect(ctx + "/dashboard");
            return;
        }

        // ── Paso 2: leer y parsear el ID ─────────────────────────────────
        String idParam = request.getParameter("id"); // El campo <input name="id"> del formulario
        if (idParam == null || idParam.isBlank()) {
            // Sin ID → no sabemos qué eliminar, volver a la lista
            response.sendRedirect(ctx + "/empleados");
            return;
        }

        int idUsuario;
        try {
            idUsuario = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            // ID no es número (¿fue manipulado?) → volver a la lista
            response.sendRedirect(ctx + "/empleados");
            return;
        }

        try {
            // ── Paso 3: buscar el usuario objetivo ────────────────────────
            // Usamos EditarEmpleadoDAO.buscarPorId() que ya existe y tiene el JOIN correcto
            EditarEmpleadoDAO buscarDao = new EditarEmpleadoDAO();
            Usuario objetivo = buscarDao.buscarPorId(idUsuario);

            if (objetivo == null) {
                // El usuario ya no existe (quizás ya fue eliminado)
                response.sendRedirect(ctx + "/empleados?error=noexiste");
                return;
            }

            // ── Paso 4: verificar permisos cruzados ───────────────────────
            String rolObj = objetivo.getNombreRol();

            // Calcular si el solicitante tiene permiso para eliminar al objetivo
            boolean tienePermiso =
                // SuperAdmin puede eliminar Admin y Empleado
                ("SuperAdministrador".equals(rolSol) &&
                    ("Administrador".equals(rolObj) || "Empleado".equals(rolObj)))
                ||
                // Admin solo puede eliminar Empleados
                ("Administrador".equals(rolSol) && "Empleado".equals(rolObj));

            if (!tienePermiso) {
                // Intento de eliminar a alguien para quien no tiene permiso
                response.sendRedirect(ctx + "/empleados?error=sinpermiso");
                return;
            }

            // ── Paso 5: eliminar el usuario (transacción en el DAO) ───────
            // El DAO elimina en orden: perfil_usuario → telefonos → correos
            // Al borrar correos, CASCADE elimina automáticamente usuarios y carrito
            new EliminarEmpleadoDAO().eliminar(idUsuario);

            // ── Paso 6: éxito → redirigir con notificación ────────────────
            response.sendRedirect(ctx + "/empleados?exito=eliminado");

        } catch (SQLException e) {
            // La BD rechazó la eliminación (ej: el usuario tiene compras registradas
            // con ON DELETE RESTRICT en detalle_compra)
            e.printStackTrace(); // Log en consola de Tomcat
            response.sendRedirect(ctx + "/empleados?error=eliminacion");
        }
    }
}
