package com.dulce_gestion.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * ============================================================
 * SERVLET: LogoutServlet
 * URL:     /logout
 * MÉTODOS: GET
 * ============================================================
 *
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    /**
     * GET /logout → destruye la sesión y redirige al login.
     *
     * @param request  contiene la petición HTTP con posible sesión activa
     * @param response para enviar el redirect al navegador
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // getSession(false): obtener la sesión existente SIN crear una nueva.
        // Si no hay sesión activa, retorna null — no hay nada que cerrar.
        HttpSession session = request.getSession(false);

        if (session != null) {
            // Destruye la sesión y todos sus atributos (incluido el objeto "usuario").
            // Después de esto, el servidor ya no reconoce al usuario como autenticado.
            session.invalidate();
        }

        // Redirigir al login con una petición GET limpia.
        // getContextPath() retorna "/dulce_gestion" (el nombre del proyecto en Tomcat).
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
