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
 */
@WebServlet("/perfil/ver")
public class VerPerfilServlet extends HttpServlet {

    /**
     * GET /perfil/ver?id=X → muestra el perfil del usuario con ese ID.
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
