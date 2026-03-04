package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.PerfilDAO;
import com.dulce_gestion.models.Usuario;
import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/perfil/ver")
public class VerPerfilServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener usuario de sesión
        HttpSession sesion = request.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
        String idUsuarioParam = request.getParameter("id");

        if (idUsuarioParam == null || idUsuarioParam.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/perfil");
            return;
        }

        try {
            int idUsuario = Integer.parseInt(idUsuarioParam);
            PerfilDAO dao = new PerfilDAO();
            
            // Obtener perfil del usuario solicitado
            Usuario perfilSolicitado = dao.obtenerPerfil(idUsuario);

            if (perfilSolicitado == null) {
                request.setAttribute("error", "Usuario no encontrado");
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/error.jsp")
                        .forward(request, response);
                return;
            }

            // Obtener lista de géneros
            request.setAttribute("generos", dao.listarGeneros());
            request.setAttribute("perfil", perfilSolicitado);
            request.setAttribute("esOtroPerfil", true);

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
