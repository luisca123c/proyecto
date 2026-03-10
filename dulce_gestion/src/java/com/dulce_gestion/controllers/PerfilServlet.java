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
 * SERVLET: PerfilServlet
 * URL:     /perfil
 * MÉTODOS: GET
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Carga y muestra el perfil completo del usuario que está en sesión.
 * Este es el perfil PROPIO — para ver el perfil de otro usuario
 * existe VerPerfilServlet (/perfil/ver?id=X).
 *
 * ¿QUÉ DATOS MUESTRA?
 * --------------------
 * Carga un objeto Usuario completo con todos sus campos:
 *   nombre, correo, teléfono, género, rol, estado, fechas de alta.
 *
 * También carga la lista de géneros (para el <select> del formulario
 * de edición que está embebido en la misma pantalla).
 *
 * ¿POR QUÉ SE VUELVE A CARGAR EL PERFIL DESDE LA BD?
 * ----------------------------------------------------
 * El objeto "usuario" guardado en sesión solo tiene los campos básicos
 * que se cargan en el login (id, correo, rol, estado, nombreCompleto).
 * No tiene teléfono, género, ni fechas de alta.
 *
 * Por eso este servlet hace una consulta completa a la BD con
 * PerfilDAO.obtenerPerfil() para traer todos los campos.
 *
 * ¿POR QUÉ SE ENVÍA A error.jsp SI NO SE ENCUENTRA EL PERFIL?
 * ------------------------------------------------------------
 * En circunstancias normales, el perfil siempre debería existir
 * (se crea junto con el usuario en una transacción). Si no se
 * encuentra, hay un problema grave de datos en la BD y se muestra
 * una pantalla de error en lugar de una pantalla rota.
 */
@WebServlet("/perfil")
public class PerfilServlet extends HttpServlet {

    /**
     * GET /perfil → carga el perfil completo del usuario en sesión.
     *
     * FLUJO PASO A PASO:
     * 1. Verificar sesión activa.
     * 2. Cargar el perfil completo desde la BD (todos los campos).
     * 3. Cargar la lista de géneros (para el formulario de edición).
     * 4. Forward al JSP mi_perfil.jsp.
     *
     * @param request  contiene la sesión activa y posibles params ?exito=
     * @param response para redirigir si no hay sesión, o forward al JSP
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

        // El usuario básico de sesión (solo campos del login)
        Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
        PerfilDAO dao = new PerfilDAO();

        try {
            // ── Paso 2: cargar el perfil COMPLETO desde la BD ─────────────
            // obtenerPerfil() hace JOIN con 6 tablas para traer todos los campos
            Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());

            if (perfil == null) {
                // El perfil no existe en la BD — error de datos
                request.setAttribute("error", "Perfil no encontrado");
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/error.jsp")
                        .forward(request, response);
                return;
            }

            // ── Paso 3: cargar la lista de géneros para el formulario ─────
            // La pantalla de perfil tiene embebido el formulario de edición
            // que necesita el <select> de géneros para funcionar
            request.setAttribute("generos", dao.listarGeneros());
            request.setAttribute("perfil",  perfil); // El objeto con todos los datos del usuario

            // ── Paso 4: forward al JSP ────────────────────────────────────
            // Los parámetros ?exito= de la URL también llegan al JSP
            request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                    .forward(request, response);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar perfil");
            request.getRequestDispatcher("/WEB-INF/jsp/perfil/error.jsp")
                    .forward(request, response);
        }
    }
}
