package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.GananciasDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/*
 * GET /ganancias              → semana actual (default)
 * GET /ganancias?periodo=semana
 * GET /ganancias?periodo=mes
 * GET /ganancias?periodo=YYYY-MM
 */
@WebServlet("/ganancias")
public class GananciasServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/ganancias/ganancias.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuario") : null;
        if (usuario == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }

        String rol = usuario.getNombreRol();
        boolean esAdminOSuper = "SuperAdministrador".equals(rol) || "Administrador".equals(rol);

        // Período: semana | mes | YYYY-MM  (default: semana)
        String periodo = request.getParameter("periodo");
        if (periodo == null || periodo.isBlank()) periodo = "semana";

        try {
            GananciasDAO dao = new GananciasDAO();
            request.setAttribute("resumen",      dao.obtenerResumen(usuario.getId(), esAdminOSuper, periodo));
            request.setAttribute("meses",        dao.listarMesesDisponibles());
            request.setAttribute("esAdminOSuper", esAdminOSuper);
            request.setAttribute("periodo",      periodo);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar las ganancias: " + e.getMessage());
        }

        request.getRequestDispatcher(VISTA).forward(request, response);
    }
}
