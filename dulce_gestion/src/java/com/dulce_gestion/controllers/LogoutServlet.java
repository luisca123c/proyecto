package com.dulce_gestion.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/*
 * Servlet que cierra la sesion del usuario en la URL /logout.
 * Se activa al hacer clic en "Cerrar sesion" en el sidebar.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    /*
     * Invalida la sesion activa y redirige al login.
     * session.invalidate() elimina todos los datos guardados en la sesion,
     * incluido el objeto "usuario". Despues de esto el acceso queda bloqueado.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // getSession(false) evita crear una sesion nueva si ya no existe ninguna
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Redirect para que el navegador haga una peticion limpia al login
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
