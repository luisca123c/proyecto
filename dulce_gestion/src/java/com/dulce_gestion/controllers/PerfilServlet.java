package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.PerfilDAO;
import com.dulce_gestion.models.Usuario;
import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/perfil")
public class PerfilServlet extends HttpServlet {

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
        PerfilDAO dao = new PerfilDAO();

        try {
            // Obtener perfil completo del usuario actual
            Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());

            if (perfil == null) {
                request.setAttribute("error", "Perfil no encontrado");
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/error.jsp")
                        .forward(request, response);
                return;
            }

            // Obtener lista de géneros
            request.setAttribute("generos", dao.listarGeneros());
            request.setAttribute("perfil", perfil);

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
