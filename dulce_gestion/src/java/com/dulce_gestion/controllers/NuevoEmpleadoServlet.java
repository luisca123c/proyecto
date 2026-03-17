package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.CrearEmpleadoDAO;
import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.utils.EmpresaUtil;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.models.Usuario;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * ============================================================
 * SERVLET: NuevoEmpleadoServlet
 * URL:     /empleados/nuevo
 * MÉTODOS: GET, POST
 * ============================================================
 *
 */
@WebServlet("/empleados/nuevo")
public class NuevoEmpleadoServlet extends HttpServlet {

    /** Ruta interna del JSP del formulario */
    private static final String VISTA = "/WEB-INF/jsp/empleados/nuevo.jsp";

    // =========================================================
    // GET /empleados/nuevo
    // =========================================================

    /**
     * Muestra el formulario vacío para crear un nuevo empleado o administrador.
     *
     * Solo pasa el atributo "esSuperAdmin" al JSP para que renderice
     * las opciones correctas en el selector de rol.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificar que el solicitante es Admin o SuperAdmin
        if (!verificarAcceso(request, response)) return;

        // Pasar al JSP si el solicitante es SuperAdmin
        setEsSuperAdmin(request);
        cargarEmprendimientos(request);

        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // POST /empleados/nuevo
    // =========================================================

    /**
     * Valida los datos del formulario y crea el nuevo usuario en la BD.
     *
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: verificar acceso ─────────────────────────────────────
        if (!verificarAcceso(request, response)) return;

        // Leer sesión una sola vez (se usa en varios pasos)
        HttpSession session   = request.getSession(false);
        Usuario solicitante   = (Usuario) session.getAttribute("usuario");

        // ── Paso 2: leer parámetros del formulario ───────────────────────
        String nombreCompleto = request.getParameter("nombreCompleto");
        String telefono       = request.getParameter("telefono");
        String genero         = request.getParameter("genero");
        String correo         = request.getParameter("correo");
        String contrasena     = request.getParameter("contrasena");
        String estado         = request.getParameter("estado");
        String rol            = request.getParameter("rol");
        String empParam       = request.getParameter("idEmprendimiento");

        // ── Paso 3: validar que todos los campos obligatorios tienen valor ─
        // SuperAdmin debe elegir emprendimiento; Admin usa el suyo automáticamente
        int idEmprendimiento;
        if ("SuperAdministrador".equals(solicitante.getNombreRol())) {
            if (estaVacio(empParam)) {
                setEsSuperAdmin(request);
                cargarEmprendimientos(request);
                request.setAttribute("error", "Debes seleccionar un emprendimiento.");
                request.getRequestDispatcher(VISTA).forward(request, response);
                return;
            }
            try { idEmprendimiento = Integer.parseInt(empParam); }
            catch (NumberFormatException e) { idEmprendimiento = 0; }
        } else {
            idEmprendimiento = EmpresaUtil.resolverEmprendimiento(solicitante, request);
        }

        if (estaVacio(nombreCompleto) || estaVacio(telefono) || estaVacio(genero)
                || estaVacio(correo) || estaVacio(contrasena)
                || estaVacio(estado) || estaVacio(rol)) {

            setEsSuperAdmin(request); // Para que el JSP muestre las opciones de rol correctas
            request.setAttribute("error", "Todos los campos son obligatorios.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // ── Paso 4: validar longitud mínima de la contraseña ─────────────
        // .trim() elimina espacios antes de contar la longitud
        if (contrasena.trim().length() < 6) {
            setEsSuperAdmin(request);
            request.setAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // ── Paso 5: verificar que un Admin no intente crear un Admin ──────
        // Solo SuperAdministrador puede asignar el rol Administrador a otro usuario
        if ("Administrador".equals(rol)
                && !"SuperAdministrador".equals(solicitante.getNombreRol())) {
            setEsSuperAdmin(request);
            request.setAttribute("error", "No tienes permiso para crear Administradores.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // ── Paso 6: verificar unicidad antes de insertar ─────────────────
        CrearEmpleadoDAO dao = new CrearEmpleadoDAO();
        try {
            // Verificar que el correo no esté ya registrado por otro usuario
            if (dao.correoExiste(correo)) {
                setEsSuperAdmin(request);
                request.setAttribute("error", "El correo ya está registrado.");
                request.getRequestDispatcher(VISTA).forward(request, response);
                return;
            }

            // Verificar que el teléfono no esté ya registrado por otro usuario
            if (dao.telefonoExiste(telefono)) {
                setEsSuperAdmin(request);
                request.setAttribute("error", "El teléfono ya está registrado.");
                request.getRequestDispatcher(VISTA).forward(request, response);
                return;
            }

            // ── Paso 7: convertir nombre de rol y género a sus IDs en la BD ─
            int idRol    = dao.obtenerIdRol(rol);       // "Empleado" → 3
            int idGenero = dao.obtenerIdGenero(genero); // "Masculino" → 1

            // ── Paso 8: crear el usuario en 4 tablas (transacción) ────────
            // La contraseña se hashea internamente en el DAO con SHA-256
            dao.crear(nombreCompleto, telefono, idGenero,
                      correo, contrasena, estado, idRol, idEmprendimiento);

        } catch (SQLException e) {
            e.printStackTrace(); // Log en consola de Tomcat
            setEsSuperAdmin(request);
            request.setAttribute("error", "Error al guardar. Intenta de nuevo.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // ── Paso 9: redirigir a la lista con mensaje de éxito ────────────
        // El parámetro ?exito=creado lo lee el JSP de la lista para mostrar notificación
        response.sendRedirect(request.getContextPath() + "/empleados?exito=creado");
    }

    // =========================================================
    // MÉTODOS AUXILIARES (HELPERS)
    // =========================================================

    /**
     * Verifica que el solicitante es SuperAdministrador o Administrador.
     * Los Empleados son redirigidos al dashboard y se retorna false.
     * Los no autenticados son redirigidos al login y se retorna false.
     *
     * El servlet que llama a este método debe retornar inmediatamente si
     * el resultado es false (ya se hizo la redirección).
     *
     * @return true si tiene permiso, false si ya se redirigió
     */
    private boolean verificarAcceso(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        Usuario u = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (u == null) {
            // Sin sesión → redirigir al login
            res.sendRedirect(req.getContextPath() + "/login");
            return false;
        }

        String rol = u.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            // Es Empleado → no tiene acceso a gestión de usuarios
            res.sendRedirect(req.getContextPath() + "/dashboard");
            return false;
        }

        return true; // Tiene permiso
    }

    /**
     * Retorna true si el string es null o está compuesto solo de espacios.
     * Se usa para validar que los campos del formulario tienen contenido.
     */
    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /**
     * Pone el atributo "esSuperAdmin" en el request para que el JSP
     * muestre u oculte la opción "Administrador" en el selector de rol.
     *
     * Se llama tanto en el GET como al reenviar el formulario con error.
     */
    private void setEsSuperAdmin(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) {
            Usuario u = (Usuario) s.getAttribute("usuario");
            req.setAttribute("esSuperAdmin",
                u != null && "SuperAdministrador".equals(u.getNombreRol()));
        }
    }

    /**
     * Carga la lista de emprendimientos activos en el request.
     * Solo es relevante para SuperAdmin (el JSP la ignora si no es SuperAdmin).
     */
    private void cargarEmprendimientos(HttpServletRequest req) {
        try {
            List<Emprendimiento> emps = new EmprendimientoDAO().listarActivos();
            req.setAttribute("emprendimientos", emps);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            req.setAttribute("emprendimientos", java.util.List.of());
        }
    }
}
