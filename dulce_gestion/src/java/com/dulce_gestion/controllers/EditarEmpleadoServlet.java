package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EditarEmpleadoDAO;
import com.dulce_gestion.dao.EmprendimientoDAO;
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
 * SERVLET: EditarEmpleadoServlet
 * URL:     /empleados/editar?id=X
 * MÉTODOS: GET, POST
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Maneja la edición de los datos de un usuario existente.
 *   GET  → carga los datos del usuario y muestra el formulario prellenado
 *   POST → valida los cambios y actualiza los datos en la BD
 *
 * ¿QUIÉN PUEDE EDITAR A QUIÉN?
 * -----------------------------
 * La regla de permisos es:
 *   SuperAdministrador → puede editar Administradores y Empleados
 *   Administrador      → puede editar SOLO Empleados
 *
 * Nadie puede editarse a sí mismo desde aquí (para eso existe /perfil/editar).
 * Nadie puede editar a un SuperAdministrador.
 *
 * Esta lógica está encapsulada en el método privado puedeEditar().
 *
 * ¿QUÉ ES EL "OBJETIVO"?
 * -----------------------
 * En este servlet se usan dos usuarios:
 *   solicitante → el usuario en sesión (quien hace la edición)
 *   objetivo    → el usuario que se está editando (el afectado)
 *
 * Ambos se cargan y se verifican permisos cruzados.
 *
 * ¿POR QUÉ LA CONTRASEÑA ES OPCIONAL EN LA EDICIÓN?
 * ---------------------------------------------------
 * Al editar un empleado, el administrador puede NO cambiar la contraseña.
 * Si el campo contraseña llega vacío → se interpreta como "no cambiar".
 * EditarEmpleadoDAO.actualizar() detecta esto y omite la columna contraseña en el UPDATE.
 *
 * ¿POR QUÉ ADMIN NO PUEDE CAMBIAR EL ROL?
 * -----------------------------------------
 * Solo el SuperAdministrador puede cambiar el rol de un usuario.
 * Si el solicitante es Admin, el parámetro "rolFinal" se fuerza
 * al rol actual del objetivo (ignorando lo que llegó en el formulario).
 * Esto evita que un Admin se otorgue a sí mismo o a otro permisos superiores.
 */
@WebServlet("/empleados/editar")
public class EditarEmpleadoServlet extends HttpServlet {

    /** Ruta interna del JSP del formulario de edición */
    private static final String VISTA = "/WEB-INF/jsp/empleados/editar.jsp";

    // =========================================================
    // GET /empleados/editar?id=X
    // =========================================================

    /**
     * Carga los datos del usuario a editar y muestra el formulario prellenado.
     *
     * FLUJO PASO A PASO:
     * 1. Verificar que el solicitante es Admin o SuperAdmin.
     * 2. Leer el parámetro ?id=X de la URL.
     * 3. Si id es inválido o falta → redirigir a la lista.
     * 4. Buscar el usuario objetivo en la BD por ID.
     * 5. Verificar que el solicitante tiene permiso para editar al objetivo.
     * 6. Pasar el objeto objetivo y el flag esSuperAdmin al JSP.
     *
     * @param request  contiene la sesión y el parámetro ?id=
     * @param response para redirigir si hay error, o para el forward al JSP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificar que el solicitante tiene permiso para editar empleados
        Usuario solicitante = getSolicitante(request, response);
        if (solicitante == null) return; // Ya se redirigió

        // Leer el ID del usuario a editar de la URL (?id=X)
        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            // Sin ID → no sabemos qué editar, volver a la lista
            response.sendRedirect(request.getContextPath() + "/empleados");
            return;
        }

        try {
            int id = Integer.parseInt(idParam); // Parsear el ID — puede lanzar NumberFormatException
            EditarEmpleadoDAO dao = new EditarEmpleadoDAO();

            // Buscar el usuario en la BD. Retorna null si no existe.
            Usuario objetivo = dao.buscarPorId(id);

            // Verificar: ¿existe el usuario? ¿puede el solicitante editarlo?
            if (objetivo == null || !puedeEditar(solicitante, objetivo)) {
                // Sin permiso o usuario no encontrado → volver a la lista sin error visible
                response.sendRedirect(request.getContextPath() + "/empleados");
                return;
            }

            // Pasar al JSP el usuario a editar y si el solicitante es SuperAdmin
            boolean esSuper = "SuperAdministrador".equals(solicitante.getNombreRol());
            request.setAttribute("objetivo",    objetivo);
            request.setAttribute("esSuperAdmin", esSuper);
            if (esSuper) {
                List<Emprendimiento> emps = new EmprendimientoDAO().listarActivos();
                request.setAttribute("emprendimientos", emps);
            }

            request.getRequestDispatcher(VISTA).forward(request, response);

        } catch (NumberFormatException e) {
            // El ID de la URL no es un número válido
            response.sendRedirect(request.getContextPath() + "/empleados");
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/empleados");
        }
    }

    // =========================================================
    // POST /empleados/editar
    // =========================================================

    /**
     * Valida los datos del formulario y actualiza el usuario en la BD.
     *
     * FLUJO PASO A PASO:
     * 1. Verificar acceso (Admin o SuperAdmin).
     * 2. Leer todos los campos del formulario.
     * 3. Validar que los campos obligatorios no estén vacíos.
     * 4. Si hay nueva contraseña, validar que tiene al menos 6 caracteres.
     * 5. Parsear el ID — si no es número, volver a la lista.
     * 6. Verificar que el objetivo existe y puede ser editado por el solicitante.
     * 7. Verificar unicidad de correo y teléfono (excluyendo al propio usuario).
     * 8. Determinar el rolFinal: si Admin → usar rol actual; si SuperAdmin → usar el del form.
     * 9. Llamar a dao.actualizar() en transacción.
     * 10. Redirigir a /empleados?exito=editado.
     *
     * @param request  contiene los datos del formulario (POST)
     * @param response para redirigir en éxito o reenviar al form en error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: verificar acceso ─────────────────────────────────────
        Usuario solicitante = getSolicitante(request, response);
        if (solicitante == null) return;

        // ¿El solicitante es SuperAdmin? Determina si puede cambiar el rol.
        boolean esSuperAdmin = "SuperAdministrador".equals(solicitante.getNombreRol());

        // ── Paso 2: leer parámetros del formulario ───────────────────────
        String idParam         = request.getParameter("id");           // ID del usuario a editar
        String nombreCompleto  = request.getParameter("nombreCompleto");
        String telefono        = request.getParameter("telefono");
        String genero          = request.getParameter("genero");
        String correo          = request.getParameter("correo");
        String nuevaContrasena = request.getParameter("contrasena");   // Puede ser vacío si no cambia
        String estado          = request.getParameter("estado");
        String rol             = request.getParameter("rol");          // Solo aplica si esSuperAdmin
        String empParam        = request.getParameter("idEmprendimiento");

        // ── Paso 3: validar campos obligatorios ──────────────────────────
        // La contraseña NO es obligatoria — campo vacío = no cambiar
        if (estaVacio(idParam) || estaVacio(nombreCompleto) || estaVacio(telefono)
                || estaVacio(genero) || estaVacio(correo) || estaVacio(estado)) {
            reenviar(request, response, "Todos los campos son obligatorios (excepto contraseña).", idParam);
            return;
        }

        // ── Paso 4: si hay nueva contraseña, validar longitud mínima ─────
        if (nuevaContrasena != null && !nuevaContrasena.isBlank()
                && nuevaContrasena.trim().length() < 6) {
            reenviar(request, response, "La nueva contraseña debe tener al menos 6 caracteres.", idParam);
            return;
        }

        // ── Paso 5: parsear el ID ─────────────────────────────────────────
        int idUsuario;
        try {
            idUsuario = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/empleados");
            return;
        }

        EditarEmpleadoDAO dao = new EditarEmpleadoDAO();
        try {
            // ── Paso 6: verificar que el objetivo existe y se puede editar ─
            Usuario objetivo = dao.buscarPorId(idUsuario);
            if (objetivo == null || !puedeEditar(solicitante, objetivo)) {
                response.sendRedirect(request.getContextPath() + "/empleados");
                return;
            }

            // ── Paso 7: verificar unicidad de correo y teléfono ───────────
            // "EnOtro" excluye al usuario actual de la verificación
            // (para que pueda guardar su propio correo/teléfono sin error)
            if (dao.correoExisteEnOtro(correo, idUsuario)) {
                reenviar(request, response, "El correo ya está en uso por otro usuario.", idParam);
                return;
            }
            if (dao.telefonoExisteEnOtro(telefono, idUsuario)) {
                reenviar(request, response, "El teléfono ya está en uso por otro usuario.", idParam);
                return;
            }

            // ── Paso 8: determinar el rol final ───────────────────────────
            // Admin no puede cambiar el rol → se usa el rol actual del objetivo
            // SuperAdmin sí puede → se usa el valor del formulario
            String rolFinal = esSuperAdmin ? rol : objetivo.getNombreRol();

            // Resolver idEmprendimiento: SuperAdmin usa el del form, sino conserva el actual
            int idEmpresaFinal = objetivo.getIdEmprendimiento(); // valor actual por defecto
            if (esSuperAdmin && empParam != null && !empParam.isBlank()) {
                try { idEmpresaFinal = Integer.parseInt(empParam); } catch (NumberFormatException ignored) {}
            }

            // ── Paso 9: actualizar en la BD (transacción de 4 tablas) ─────
            dao.actualizar(idUsuario, nombreCompleto, telefono, genero,
                           correo, nuevaContrasena, estado, rolFinal, esSuperAdmin, idEmpresaFinal);

        } catch (SQLException e) {
            e.printStackTrace();
            reenviar(request, response, "Error al guardar los cambios. Intenta de nuevo.", idParam);
            return;
        }

        // ── Paso 10: redirigir a la lista con mensaje de éxito ────────────
        response.sendRedirect(request.getContextPath() + "/empleados?exito=editado");
    }

    // =========================================================
    // MÉTODOS AUXILIARES (HELPERS)
    // =========================================================

    /**
     * Obtiene el usuario en sesión y verifica que tenga rol de gestión.
     *
     * Retorna null si no hay sesión (redirige a /login) o si el rol
     * no tiene permiso (redirige a /dashboard).
     * El llamador debe retornar inmediatamente si el resultado es null.
     */
    private Usuario getSolicitante(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        Usuario u = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (u == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }

        String rol = u.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            // Un Empleado intentando editar → redirigir al inicio
            res.sendRedirect(req.getContextPath() + "/dashboard");
            return null;
        }

        return u;
    }

    /**
     * Verifica si el solicitante tiene permiso para editar al objetivo.
     *
     * Regla de permisos:
     *   SuperAdmin → puede editar Admin y Empleado
     *   Admin      → solo puede editar Empleado
     *
     * @param solicitante  usuario que realiza la acción de edición
     * @param objetivo     usuario que va a ser editado
     * @return             true si tiene permiso, false si no
     */
    private boolean puedeEditar(Usuario solicitante, Usuario objetivo) {
        String rolSol = solicitante.getNombreRol(); // Rol del que edita
        String rolObj = objetivo.getNombreRol();    // Rol del que es editado

        if ("SuperAdministrador".equals(rolSol)) {
            // SuperAdmin puede editar a Admin y a Empleado (no a otro SuperAdmin)
            return "Administrador".equals(rolObj) || "Empleado".equals(rolObj);
        }
        if ("Administrador".equals(rolSol)) {
            // Admin solo puede editar a Empleados
            return "Empleado".equals(rolObj);
        }
        return false; // Empleado no puede editar a nadie
    }

    /**
     * Retorna true si el string es null o está compuesto solo de espacios.
     */
    private boolean estaVacio(String v) { return v == null || v.isBlank(); }

    /**
     * Recarga los datos del usuario objetivo y reenvía al formulario de edición
     * con el mensaje de error. Se usa cuando una validación falla.
     *
     * Carga:
     *   - El objeto "objetivo" para prellenar el formulario con los datos actuales.
     *   - El flag "esSuperAdmin" para mostrar el selector de rol correctamente.
     *   - El atributo "error" con el mensaje a mostrar.
     *
     * @param req    request de la petición actual
     * @param res    response de la petición actual
     * @param error  mensaje de error a mostrar en el formulario
     * @param idParam string del ID del usuario (puede ser inválido)
     */
    private void reenviar(HttpServletRequest req, HttpServletResponse res,
                          String error, String idParam)
            throws ServletException, IOException {
        try {
            int id = Integer.parseInt(idParam);
            EditarEmpleadoDAO dao = new EditarEmpleadoDAO();
            Usuario objetivo = dao.buscarPorId(id);
            req.setAttribute("objetivo", objetivo); // Para prellenar el formulario

            // Determinar si el solicitante es SuperAdmin (para el select de rol)
            HttpSession session = req.getSession(false);
            Usuario sol = (Usuario) session.getAttribute("usuario");
            req.setAttribute("esSuperAdmin",
                "SuperAdministrador".equals(sol.getNombreRol()));

        } catch (Exception ignored) {
            // Si algo falla aquí, el formulario se mostrará sin prellenar
        }

        req.setAttribute("error", error); // Mensaje de error a mostrar en el JSP
        req.getRequestDispatcher(VISTA).forward(req, res);
    }
}
