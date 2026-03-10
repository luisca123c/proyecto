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
 * ¿QUÉ HACE?
 * ----------
 * Cierra la sesión del usuario y lo redirige al formulario de login.
 * Se activa cuando el usuario hace clic en "Cerrar sesión" en el sidebar.
 *
 * ¿QUÉ HACE session.invalidate()?
 * ---------------------------------
 * invalidate() destruye completamente la sesión HTTP del servidor:
 *   1. Elimina el objeto "usuario" y todos los demás atributos guardados.
 *   2. Marca el ID de sesión (JSESSIONID) como inválido en el servidor.
 *   3. La cookie JSESSIONID del navegador queda apuntando a un ID
 *      que ya no existe — la próxima petición creará una sesión nueva.
 *
 * Después de invalidate(), FiltroAutenticacion detectará que no hay
 * sesión activa en cualquier petición y redirigirá al login.
 *
 * ¿POR QUÉ getSession(false) Y NO getSession()?
 * -----------------------------------------------
 * getSession() sin argumento equivale a getSession(true), que crea
 * una sesión nueva si no existe ninguna.
 *
 * Usando getSession(false) obtenemos la sesión existente SIN crear
 * una nueva. Si ya no hay sesión activa (caso raro pero posible),
 * retorna null y simplemente saltamos el invalidate() — nada que destruir.
 *
 * Crear una sesión solo para destruirla sería ineficiente e innecesario.
 *
 * ¿POR QUÉ sendRedirect Y NO forward?
 * -------------------------------------
 * sendRedirect envía al navegador una respuesta HTTP 302 ("Moved Temporarily").
 * El navegador hace una petición GET nueva a /login.
 *
 * Esto es importante por dos razones:
 *   1. La URL del navegador queda en /login (no en /logout).
 *   2. Si el usuario presiona "Atrás", va a /login, no a una acción
 *      de logout que ya se ejecutó.
 *
 * Con forward, la URL seguiría mostrando /logout, lo cual sería confuso.
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
