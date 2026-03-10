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
 * ¿QUÉ ES UN FILTRO Y EN QUÉ SE DIFERENCIA DE UN SERVLET?
 * ---------------------------------------------------------
 * Un Servlet genera una respuesta (HTML, JSON, redirección).
 * Un Filter es un intermediario que se ejecuta ANTES y/o DESPUÉS
 * de que la petición llegue al Servlet. Puede:
 *   - Dejar pasar la petición (chain.doFilter)
 *   - Bloquearla (sendRedirect, sendError)
 *   - Modificarla (añadir atributos, cambiar headers)
 *
 * Diagrama del ciclo de vida de una petición:
 *
 *   Navegador → [FiltroAutenticacion] → Servlet → JSP → Respuesta
 *                      ↑
 *             Si no hay sesión → redirige al login (el Servlet nunca se ejecuta)
 *
 * ¿QUÉ HACE ESTE FILTRO?
 * ----------------------
 * Es el portero global de la aplicación. Garantiza que ningún Servlet
 * se ejecute a menos que el usuario esté autenticado.
 *
 * Sin este filtro, cualquier persona que conociera la URL directa de
 * un Servlet (ej: /productos, /ventas) podría acceder sin iniciar sesión.
 * Los Servlets verifican la sesión individualmente, pero es una segunda
 * línea de defensa. Este filtro es la primera línea.
 *
 * ¿QUÉ ES @WebFilter("/*")?
 * --------------------------
 * Registra esta clase como filtro que intercepta TODAS las peticiones
 * cuya URL coincida con el patrón "/*". El asterisco es un comodín:
 *   /*       → todas las URLs bajo el contexto
 *   /admin/* → solo las URLs que empiecen con /admin/
 *
 * Como web.xml tiene metadata-complete="true", esta anotación es
 * ignorada por Tomcat en favor de la declaración manual en web.xml.
 * Sin embargo, se mantiene como documentación del patrón que se usa.
 *
 * ¿POR QUÉ EXISTEN RUTAS PÚBLICAS?
 * ----------------------------------
 * No todo debe estar protegido. Hay rutas que deben ser accesibles
 * sin sesión:
 *
 *   /login    → el formulario de login. Si se bloqueara, nadie
 *               podría entrar nunca (loop infinito de redirects)
 *
 *   /assets/  → archivos estáticos: CSS, JS, imágenes de la interfaz.
 *               Si se bloquearan, la pantalla de login aparecería
 *               sin estilos ni scripts.
 *
 *   /uploads/ → imágenes de productos (servidas por ImagenServlet).
 *               En este sistema se decidió que las imágenes de productos
 *               son accesibles sin sesión (ej: para mostrar en emails
 *               o en integraciones externas). Si se quisiera protegerlas,
 *               se eliminaría esta excepción.
 *
 * ¿QUÉ ES LA FilterChain?
 * ------------------------
 * chain.doFilter(request, response) pasa la petición al siguiente
 * elemento en la cadena de procesamiento. Si hay más filtros definidos,
 * se ejecutan en orden. Después del último filtro, se ejecuta el Servlet.
 *
 * Si chain.doFilter NO se llama, la petición se detiene en este filtro
 * y el Servlet nunca se ejecuta. Así funciona el bloqueo por falta de sesión.
 *
 * ¿POR QUÉ init() Y destroy() ESTÁN VACÍOS?
 * -------------------------------------------
 * La interfaz Filter requiere implementar estos tres métodos:
 *   init(FilterConfig)  → se llama cuando Tomcat carga el filtro.
 *                         Útil para cargar configuración inicial.
 *   doFilter(...)       → se llama en cada petición que coincide con el patrón.
 *   destroy()           → se llama cuando Tomcat detiene la aplicación.
 *                         Útil para liberar recursos.
 *
 * Este filtro no necesita inicialización ni limpieza porque no mantiene
 * estado propio. Los métodos existen solo para cumplir el contrato de la interfaz.
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
     * FLUJO PASO A PASO:
     *
     * Paso 1 — Castear a HTTP:
     *   La interfaz Filter recibe ServletRequest/ServletResponse (genéricos).
     *   Se castean a HttpServletRequest/HttpServletResponse para acceder
     *   a métodos específicos de HTTP (getRequestURI, getSession, sendRedirect).
     *
     * Paso 2 — Verificar si la ruta es pública:
     *   Se compara la URI de la petición contra la lista de rutas públicas.
     *   Si es pública → dejar pasar sin verificar sesión.
     *
     * Paso 3 — Verificar sesión activa:
     *   getSession(false) busca una sesión existente SIN crear una nueva.
     *   Si la sesión existe, se busca el atributo "usuario" guardado por LoginServlet.
     *   Si no hay sesión o no hay usuario → redirigir al login.
     *
     * Paso 4 — Dejar pasar:
     *   Si hay usuario en sesión → chain.doFilter() pasa la petición al Servlet.
     *
     * ¿POR QUÉ getSession(false) Y NO getSession()?
     * -----------------------------------------------
     * getSession() sin argumento equivale a getSession(true): crea una sesión
     * nueva si no existe. Si se hiciera eso aquí, CADA petición sin sesión
     * crearía una sesión vacía en el servidor, consumiendo memoria innecesariamente.
     * Con false: si no hay sesión, retorna null. Seguro y eficiente.
     *
     * ¿POR QUÉ EL OPERADOR TERNARIO EN VEZ DE if/null?
     * --------------------------------------------------
     * Si session fuera null y se llamara session.getAttribute() directamente,
     * se lanzaría NullPointerException. El ternario evita eso en una línea:
     *   (session != null) ? session.getAttribute("usuario") : null
     *
     * @param req    petición HTTP entrante
     * @param res    respuesta HTTP (para hacer el redirect si no hay sesión)
     * @param chain  cadena de filtros y Servlet que se ejecutan después
     * @throws IOException      si ocurre error de I/O al hacer el redirect
     * @throws ServletException si ocurre error al procesar la petición
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
