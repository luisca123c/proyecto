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
 * ============================================================
 * FILTRO: FiltroAutenticacion
 * URL:    /*  (intercepta TODAS las peticiones)
 * ============================================================
 *
 */
@WebFilter("/*")
public class FiltroAutenticacion implements Filter {

    /**
     * Llamado por Tomcat al cargar el filtro (arranque de la aplicación).
     * No se necesita inicialización para este filtro.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    /**
     * Intercepta cada petición HTTP y decide si dejarla pasar o redirigir al login.
     *
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // ── Paso 1: castear a interfaces HTTP ─────────────────────────────
        // Los tipos genéricos no tienen getRequestURI ni sendRedirect.
        // Siempre son instancias HTTP en una aplicación web Jakarta EE.
        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri         = request.getRequestURI();    // Ej: "/dulce_gestion/productos"
        String contextPath = request.getContextPath();   // Ej: "/dulce_gestion"

        // ── Paso 2: verificar si la ruta es pública ───────────────────────
        // Se compara la URI completa (con contextPath) para evitar falsos positivos.
        // startsWith en vez de equals para /assets/ y /uploads/ porque abarcan
        // múltiples archivos bajo ese prefijo (CSS, JS, imágenes).
        boolean esPublica = uri.equals(contextPath + "/login")          // El formulario de login
                         || uri.startsWith(contextPath + "/assets/")    // CSS, JS, imágenes de UI
                         || uri.startsWith(contextPath + "/uploads/");  // Imágenes de productos

        if (esPublica) {
            // Ruta pública → no verificar sesión, pasar directamente al Servlet
            chain.doFilter(request, response);
            return; // Detener ejecución de este filtro aquí
        }

        // ── Paso 3: verificar sesión activa ───────────────────────────────
        // getSession(false): busca sesión existente SIN crear una nueva.
        // Si no hay sesión → retorna null (no consume recursos).
        HttpSession session = request.getSession(false);

        // Extraer el objeto Usuario guardado en sesión por LoginServlet.
        // Si session es null → operador ternario retorna null sin NullPointerException.
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) {
            // Sin sesión o sin usuario → no está autenticado.
            // Redirigir al login para que el usuario se identifique.
            response.sendRedirect(contextPath + "/login");
            return; // El Servlet de destino NO se ejecuta
        }

        // ── Paso 4: usuario autenticado → dejar pasar ────────────────────
        // chain.doFilter pasa la petición al siguiente filtro o al Servlet final.
        chain.doFilter(request, response);
    }

    /**
     * Llamado por Tomcat al detener la aplicación.
     * No se necesita liberar recursos para este filtro.
     */
    @Override
    public void destroy() {}
}
