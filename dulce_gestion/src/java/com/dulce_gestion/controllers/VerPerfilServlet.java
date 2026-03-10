package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.PerfilDAO;
import com.dulce_gestion.models.Usuario;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * ============================================================
 * SERVLET: VerPerfilServlet
 * URL:     /perfil/ver?id=X
 * MÉTODOS: GET
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Muestra el perfil completo de OTRO usuario (no el propio).
 * Se accede desde la lista de empleados cuando el administrador
 * hace clic en el nombre o icono de perfil de un empleado.
 *
 * ¿EN QUÉ SE DIFERENCIA DE PerfilServlet (/perfil)?
 * ---------------------------------------------------
 *   /perfil       → tu propio perfil (editable, con formularios)
 *   /perfil/ver   → perfil de otra persona (solo lectura, sin formularios)
 *
 * ¿POR QUÉ SE PASA "esOtroPerfil = true"?
 * -----------------------------------------
 * El JSP ver_perfil.jsp y mi_perfil.jsp pueden compartir partes del
 * diseño, pero el primero NO muestra los formularios de edición.
 * El atributo esOtroPerfil permite que el JSP sepa que está en modo
 * "solo lectura" y no muestre los botones de edición.
 *
 * ¿HAY RESTRICCIÓN DE ROL?
 * -------------------------
 * No. Cualquier usuario autenticado puede ver el perfil de otro.
 * Esto es útil por ejemplo para que un empleado vea los datos de
 * contacto de su jefe sin necesitar ir a la sección de empleados.
 *
 * ¿QUÉ PASA SI EL ID ES DEL PROPIO USUARIO?
 * -------------------------------------------
 * Si el usuario solicita ver su propio perfil con /perfil/ver?id=X,
 * este servlet lo procesa igual (no hay validación para eso).
 * Sin embargo, lo normal es que el sidebar siempre apunte a /perfil.
 */
@WebServlet("/perfil/ver")
public class VerPerfilServlet extends HttpServlet {

    /**
     * GET /perfil/ver?id=X → muestra el perfil del usuario con ese ID.
     *
     * FLUJO PASO A PASO:
     * 1. Verificar sesión activa.
     * 2. Leer y validar el parámetro ?id= de la URL.
     *    Si falta → redirigir a /perfil (el propio perfil).
     * 3. Cargar el perfil solicitado desde la BD.
     *    Si no existe → mostrar error.
     * 4. Poner el perfil y el flag esOtroPerfil en el request.
     * 5. Forward al JSP ver_perfil.jsp.
     *
     * @param request  contiene la sesión activa y el parámetro ?id=
     * @param response para redirigir si hay error, o forward al JSP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: verificar sesión activa ──────────────────────────────
        HttpSession sesion = request.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // ── Paso 2: leer y validar el parámetro ?id= ─────────────────────
        String idUsuarioParam = request.getParameter("id");
        if (idUsuarioParam == null || idUsuarioParam.isEmpty()) {
            // Sin ID → no sabemos de quién ver el perfil, ir al propio
            response.sendRedirect(request.getContextPath() + "/perfil");
            return;
        }

        try {
            int idUsuario = Integer.parseInt(idUsuarioParam);
            PerfilDAO dao = new PerfilDAO();

            // ── Paso 3: cargar el perfil del usuario solicitado ───────────
            // obtenerPerfil() hace el JOIN completo con 6 tablas
            Usuario perfilSolicitado = dao.obtenerPerfil(idUsuario);

            if (perfilSolicitado == null) {
                // El usuario no existe en la BD
                request.setAttribute("error", "Usuario no encontrado");
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/error.jsp")
                        .forward(request, response);
                return;
            }

            // ── Paso 4: poner datos en el request ─────────────────────────
            request.setAttribute("generos",      dao.listarGeneros());    // Por si el JSP los usa
            request.setAttribute("perfil",       perfilSolicitado);       // El usuario solicitado
            request.setAttribute("esOtroPerfil", true);                   // Flag: modo "solo lectura"

            // ── Paso 5: forward al JSP de ver perfil ──────────────────────
            request.getRequestDispatcher("/WEB-INF/jsp/perfil/ver_perfil.jsp")
                    .forward(request, response);

        } catch (NumberFormatException | SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar perfil");
            try {
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/error.jsp")
                        .forward(request, response);
            } catch (ServletException ex) {
                ex.printStackTrace();
            }
        }
    }
}
