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

/*
 * Servlet para /empleados.
 * Carga la lista de usuarios según el rol del solicitante
 * y hace forward al JSP correspondiente.
 */
@WebServlet("/empleados")
public class EmpleadosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Usuario solicitante = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (solicitante == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String rol = solicitante.getNombreRol();

        // Solo SuperAdmin y Admin pueden ver esta sección
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        EmpleadoDAO dao = new EmpleadoDAO();
        List<Usuario> lista;

        try {
            lista = dao.listarSegunRol(rol);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("errorEmpleados", "Error al cargar los empleados.");
            lista = List.of();
        }

        request.setAttribute("empleados", lista);
        request.setAttribute("rolSolicitante", rol);

        request.getRequestDispatcher("/WEB-INF/jsp/empleados/lista.jsp")
               .forward(request, response);
    }
}
