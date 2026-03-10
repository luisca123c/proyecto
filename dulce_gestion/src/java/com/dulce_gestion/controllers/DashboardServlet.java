package com.dulce_gestion.controllers;

import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * ============================================================
 * SERVLET: DashboardServlet
 * URL:     /dashboard
 * MÉTODOS: GET
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Es el "distribuidor de inicio". Cuando cualquier parte del
 * sistema redirige a /dashboard, este servlet lee el ROL del
 * usuario en sesión y lo envía al JSP que le corresponde:
 *
 *   SuperAdministrador → /WEB-INF/jsp/superadmin/dashboard.jsp
 *   Administrador      → /WEB-INF/jsp/admin/dashboard.jsp
 *   Empleado           → /WEB-INF/jsp/empleado/dashboard.jsp
 *
 * ¿POR QUÉ EXISTE ESTE SERVLET?
 * ------------------------------
 * Sin él, cada componente (LoginServlet, LogoutServlet, botones del sidebar)
 * tendría que conocer qué JSP corresponde a cada rol. Eso duplicaría
 * la lógica en muchos lugares.
 *
 * Con este servlet, todos simplemente redirigen a /dashboard y aquí
 * se resuelve el rol en un único punto. Es el principio DRY
 * (Don't Repeat Yourself) aplicado a la navegación.
 *
 * ¿QUÉ ES @WebServlet?
 * ----------------------
 * Registra esta clase como servlet que responde a la URL /dashboard.
 * Como web.xml tiene metadata-complete="true", esta anotación es
 * ignorada y el mapping está también declarado manualmente en web.xml.
 *
 * ¿QUÉ ES UN SWITCH EXPRESSION (→)?
 * -----------------------------------
 * A partir de Java 14, el switch puede usarse como expresión que
 * retorna un valor directamente:
 *
 *   String vista = switch (rol) {
 *       case "SuperAdministrador" -> VISTA_SUPERADMIN;
 *       case "Empleado"           -> VISTA_EMPLEADO;
 *       default                   -> VISTA_ADMIN;
 *   };
 *
 * Es más compacto que el switch clásico con break y variable auxiliar.
 * Este proyecto usa Java 22, así que el switch expression funciona.
 *
 * ¿FORWARD VS REDIRECT?
 * ----------------------
 * Este servlet usa forward (no redirect) para enviar al JSP:
 *
 *   forward:  El servidor procesa el JSP internamente.
 *             La URL del navegador sigue mostrando /dashboard.
 *             Una sola petición HTTP — más eficiente.
 *
 *   redirect: El servidor le dice al navegador "ve a esta otra URL".
 *             La URL del navegador cambia.
 *             Dos peticiones HTTP.
 *
 * Usamos forward para que el navegador siempre muestre /dashboard
 * sin revelar la ruta interna del JSP.
 */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    // =========================================================
    // CONSTANTES: rutas internas de los JSPs
    // Están en /WEB-INF/ para que no sean accesibles directamente
    // por el navegador — solo se pueden servir mediante forward.
    // =========================================================

    /** JSP del panel de control del SuperAdministrador */
    private static final String VISTA_SUPERADMIN = "/WEB-INF/jsp/superadmin/dashboard.jsp";

    /** JSP del panel de control del Administrador */
    private static final String VISTA_ADMIN      = "/WEB-INF/jsp/admin/dashboard.jsp";

    /** JSP del panel de control del Empleado */
    private static final String VISTA_EMPLEADO   = "/WEB-INF/jsp/empleado/dashboard.jsp";

    // =========================================================
    // GET /dashboard
    // =========================================================

    /**
     * Determina el rol del usuario en sesión y hace forward
     * al JSP de dashboard correspondiente.
     *
     * FLUJO PASO A PASO:
     *
     * 1. Obtener la sesión sin crear una nueva (false).
     * 2. Extraer el objeto Usuario de la sesión.
     * 3. Si no hay usuario (sesión inválida) → redirigir al login.
     *    Nota: FiltroAutenticacion ya debería haber bloqueado esto,
     *    pero esta es una segunda línea de defensa.
     * 4. Usar switch expression para seleccionar la vista según el rol.
     * 5. Hacer forward al JSP seleccionado.
     *
     * @param request  objeto que contiene la petición HTTP (sesión, parámetros)
     * @param response objeto para construir la respuesta HTTP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // getSession(false) busca la sesión existente SIN crear una nueva.
        // Si el usuario no tiene sesión, retorna null.
        HttpSession session = request.getSession(false);

        // Extraer el usuario de la sesión. Si session es null, el operador
        // ternario retorna null sin lanzar NullPointerException.
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        // Seguridad adicional: si no hay usuario, redirigir al login.
        // (El filtro ya debería haberlo bloqueado antes de llegar aquí)
        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return; // Detener la ejecución para que no continúe al forward
        }

        // Switch expression: asigna la vista según el rol del usuario.
        // getNombreRol() retorna "SuperAdministrador", "Administrador" o "Empleado".
        String vista = switch (usuario.getNombreRol()) {
            case "SuperAdministrador" -> VISTA_SUPERADMIN; // Panel completo con gestión de admins
            case "Empleado"           -> VISTA_EMPLEADO;   // Panel simplificado solo lectura
            default                   -> VISTA_ADMIN;      // Panel de administrador (caso default)
        };

        // Forward: el servidor procesa el JSP y envía la respuesta.
        // La URL del navegador sigue siendo /dashboard.
        request.getRequestDispatcher(vista).forward(request, response);
    }
}
