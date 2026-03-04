package com.dulce_gestion.filters;

import com.dulce_gestion.models.Usuario;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filtro de autenticación global.
 * Intercepta todas las rutas. Deja pasar /login y /assets/.
 * Cualquier otra ruta sin sesión activa → redirige al login.
 */
@WebFilter("/*")
public class FiltroAutenticacion implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri         = request.getRequestURI();
        String contextPath = request.getContextPath();

        // Rutas públicas: login y assets estáticos
        boolean esPublica = uri.equals(contextPath + "/login")
                         || uri.startsWith(contextPath + "/assets/")
                         || uri.startsWith(contextPath + "/uploads/");

        if (esPublica) {
            chain.doFilter(request, response);
            return;
        }

        // Verificar sesión activa
        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) {
            response.sendRedirect(contextPath + "/login");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
