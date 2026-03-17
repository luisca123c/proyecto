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
 */
@WebServlet("/perfil")
public class PerfilServlet extends HttpServlet {

    /**
     * GET /perfil → carga el perfil completo del usuario en sesión.
     *
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
