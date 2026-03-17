package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.dao.UsuarioDAO;
import com.dulce_gestion.utils.EmpresaUtil;
import com.dulce_gestion.dao.GastosDAO;
import com.dulce_gestion.dao.GastosDAO.FilaGasto;
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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ============================================================
 * SERVLET: GastosServlet
 * URL:     /gastos
 * MÉTODOS: GET, POST
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Maneja el módulo completo de gastos del negocio:
 *
 *   GET  /gastos            → muestra la lista de gastos + modal para agregar
 *   GET  /gastos?editar=ID  → muestra la lista + modal de edición prellenado
 *   POST /gastos?accion=crear  → registra un nuevo gasto
 *   POST /gastos?accion=editar → guarda los cambios en un gasto existente
 *
 * ¿QUIÉN PUEDE ACCEDER?
 * ----------------------
 * Solo SuperAdministrador y Administrador.
 * Los Empleados no tienen acceso a información financiera de gastos.
 *
 * ¿CÓMO FUNCIONA EL MODAL DE EDICIÓN?
 * -------------------------------------
 * La pantalla de gastos usa un solo JSP con dos modales (ventanas emergentes):
 *   Modal 1 → "Agregar gasto" (siempre visible en botón)
 *   Modal 2 → "Editar gasto" (se prellenar con datos del gasto a editar)
 *
 * Cuando el admin hace clic en "Editar" en una fila:
 *   → El JSP genera un enlace: /gastos?editar=5
 *   → El servidor recibe el GET, carga el gasto con ID 5
 *   → Pone el objeto FilaGasto en el request con nombre "gastoEditar"
 *   → El JSP detecta que existe "gastoEditar" y abre el modal de edición
 *      con los datos prellenados
 *
 * ¿POR QUÉ SE USA SimpleDateFormat PARA LA FECHA?
 * -------------------------------------------------
 * El formulario tiene un input type="date" que envía la fecha en formato
 * "yyyy-MM-dd" (ej: "2025-03-15"). Pero la BD espera un datetime completo
 * para la columna fecha_gasto (ej: "2025-03-15 14:30:00").
 *
 * SimpleDateFormat.format(new Date()) obtiene la hora actual del servidor
 * y se concatena con la fecha del formulario para generar el datetime completo.
 *
 * ¿QUÉ HACE EL MÉTODO validar()?
 * --------------------------------
 * Es un helper que verifica que un parámetro no esté vacío.
 * Si está vacío, lanza IllegalArgumentException con el mensaje dado.
 * Esto permite centralizar la validación de campos obligatorios.
 */
@WebServlet("/gastos")
public class GastosServlet extends HttpServlet {

    /** Ruta interna del JSP del módulo de gastos */
    private static final String VISTA = "/WEB-INF/jsp/gastos/gastos.jsp";

    // =========================================================
    // GET /gastos
    // =========================================================

    /**
     * Muestra la lista de gastos. Si viene ?editar=ID, carga ese gasto
     * para prellenar el modal de edición.
     *
     * FLUJO PASO A PASO:
     * 1. Verificar acceso (solo Admin y SuperAdmin).
     * 2. Si hay ?editar=ID → cargar el gasto específico para el modal de edición.
     * 3. Cargar la lista completa de gastos y los métodos de pago.
     * 4. Forward al JSP.
     *
     * @param request  contiene la sesión y posible parámetro ?editar=
     * @param response para redirigir si no tiene acceso, o forward al JSP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificar que el usuario es Admin o SuperAdmin
        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return; // Ya se redirigió

        // Si viene ?editar=ID, cargar ese gasto para prellenar el modal de edición
        String editarId = request.getParameter("editar");
        if (editarId != null) {
            try {
                GastosDAO dao = new GastosDAO();
                // Cargar el gasto específico → el JSP lo detecta y abre el modal de edición
                FilaGasto gastoEditar = dao.obtenerPorId(Integer.parseInt(editarId));
                request.setAttribute("gastoEditar", gastoEditar);
            } catch (Exception e) {
                // Si no se encontró el gasto, mostrar error pero continuar con la lista
                request.setAttribute("error", "Gasto no encontrado.");
            }
        }

        // Cargar la lista completa de gastos y los métodos de pago
        cargarDatos(request, usuario);

        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // POST /gastos
    // =========================================================

    /**
     * Procesa el formulario de creación o edición de un gasto.
     *
     * El parámetro ?accion= determina qué hacer:
     *   "crear" → registrar nuevo gasto
     *   "editar" → actualizar gasto existente
     *
     * FLUJO PASO A PASO:
     * 1. Verificar acceso.
     * 2. Leer el parámetro "accion".
     * 3. Leer y validar los campos comunes (descripcion, total, idMetodoPago, fecha).
     * 4. Construir el datetime completo combinando la fecha del form con la hora actual.
     * 5. Ejecutar crear o editar según la acción.
     * 6. Redirigir con mensaje de éxito.
     *
     * Si cualquier validación falla (campo vacío, monto inválido, error de BD),
     * se muestra el formulario de nuevo con el modal correcto abierto.
     *
     * @param request  contiene el formulario del modal y el parámetro "accion"
     * @param response para redirigir en éxito o reenviar al JSP en error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: verificar acceso ─────────────────────────────────────
        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return;

        String accion = request.getParameter("accion"); // "crear" o "editar"
        GastosDAO dao = new GastosDAO();

        try {
            // ── Paso 3: leer y validar campos comunes ─────────────────────
            // validar() lanza IllegalArgumentException si el campo está vacío
            String descripcion   = validar(request.getParameter("descripcion"), "La descripción es obligatoria.");
            String totalStr      = validar(request.getParameter("total"),       "El monto es obligatorio.");
            String metodoPagoStr = request.getParameter("idMetodoPago");
            String fecha         = validar(request.getParameter("fecha"),       "La fecha es obligatoria.");

            // Parsear el monto. Reemplazar coma por punto para aceptar "1.500,50" o "1500.50"
            BigDecimal total = new BigDecimal(totalStr.replace(",", "."));
            if (total.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("El monto debe ser mayor a cero.");

            int idMetodoPago = Integer.parseInt(metodoPagoStr);

            // ── Paso 4: construir datetime completo ───────────────────────
            // fecha viene como "yyyy-MM-dd" del input type="date"
            // new Date() obtiene la hora actual del servidor
            String horaActual    = new SimpleDateFormat("HH:mm:ss").format(new Date());
            // Validar que la fecha no sea futura
            java.time.LocalDate fechaDate = java.time.LocalDate.parse(fecha);
            if (fechaDate.isAfter(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("La fecha no puede ser una fecha futura.");
            }
            String fechaDatetime = fecha + " " + horaActual; // "2025-03-15 14:30:00"

            // ── Paso 5: ejecutar la acción correspondiente ────────────────
            if ("editar".equals(accion)) {
                // Edición: necesita el ID del gasto, del detalle y de la compra
                int idGasto         = Integer.parseInt(request.getParameter("idGasto"));
                int idDetalleCompra = Integer.parseInt(request.getParameter("idDetalleCompra"));
                int idCompra        = Integer.parseInt(request.getParameter("idCompra"));

                // Actualizar las 3 tablas relacionadas (transacción en el DAO)
                // Si SuperAdmin cambió el emprendimiento, actualizar id_usuario en detalle_compra
                int idNuevoUsuarioGasto = 0;
                if ("SuperAdministrador".equals(usuario.getNombreRol())) {
                    String empEditR = request.getParameter("idEmpresaRegistro");
                    if (empEditR != null && !empEditR.isBlank()) {
                        try {
                            int empEditId = Integer.parseInt(empEditR);
                            if (empEditId > 0) idNuevoUsuarioGasto = new UsuarioDAO().obtenerAdminDeEmprendimiento(empEditId);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                dao.editar(idGasto, idDetalleCompra, idCompra, descripcion.trim(),
                           total, idMetodoPago, fechaDatetime, idNuevoUsuarioGasto);

                // ── Paso 6a: redirigir con éxito ─────────────────────────
                response.sendRedirect(request.getContextPath() + "/gastos?exito=editado");

            } else {
                // Creación: solo necesita los datos del gasto y el usuario que lo registra
                // usuario.getId() asocia el gasto al usuario que lo registró
                // SuperAdmin registra a nombre del admin del emprendimiento filtrado
                int idUsuarioRegistra = usuario.getId();
                if ("SuperAdministrador".equals(usuario.getNombreRol())) {
                    String empR = request.getParameter("idEmpresaRegistro");
                    int empIdR = 0;
                    if (empR != null && !empR.isBlank()) {
                        try { empIdR = Integer.parseInt(empR); } catch (NumberFormatException ignored) {}
                    }
                    if (empIdR > 0) {
                        int adminId = new UsuarioDAO().obtenerAdminDeEmprendimiento(empIdR);
                        if (adminId > 0) idUsuarioRegistra = adminId;
                    }
                }
                dao.registrar(idUsuarioRegistra, descripcion.trim(), total, idMetodoPago, fechaDatetime);

                // ── Paso 6b: redirigir con éxito ─────────────────────────
                response.sendRedirect(request.getContextPath() + "/gastos?exito=creado");
            }
            return; // Detener aquí — no continuar al bloque de error

        } catch (NumberFormatException e) {
            // El monto o algún ID numérico tiene formato inválido
            request.setAttribute("error", "Monto o ID inválido.");
        } catch (IllegalArgumentException e) {
            // Lanzado por validar() o por la verificación del monto > 0
            request.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error en base de datos: " + e.getMessage());
        }

        // ── Manejo de error: re-mostrar el modal correcto ────────────────
        // Si fue una edición fallida, recargar el gasto para el modal de edición
        if ("editar".equals(accion)) {
            try {
                int idGasto = Integer.parseInt(request.getParameter("idGasto"));
                // Recargar el gasto para que el modal de edición se abra con datos
                request.setAttribute("gastoEditar", dao.obtenerPorId(idGasto));
            } catch (Exception ignored) {}
        }

        // Recargar la lista completa para mostrar la página con el error
        cargarDatos(request, usuario);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // MÉTODOS AUXILIARES (HELPERS)
    // =========================================================

    /**
     * Valida que un parámetro del formulario no esté vacío.
     * Si está vacío → lanza IllegalArgumentException con el mensaje dado.
     * Si tiene contenido → retorna el valor para usarlo directamente.
     *
     * Permite escribir validaciones en una línea:
     *   String desc = validar(request.getParameter("descripcion"), "La descripción es obligatoria.");
     *
     * @param val  valor del parámetro del formulario (puede ser null)
     * @param msg  mensaje de error si el valor está vacío
     * @return     el valor si no está vacío
     * @throws IllegalArgumentException si el valor es null o solo espacios
     */
    private String validar(String val, String msg) {
        if (val == null || val.isBlank()) throw new IllegalArgumentException(msg);
        return val;
    }

    /**
     * Carga la lista de gastos y los métodos de pago para el JSP.
     * Se llama en el GET y al reenviar el formulario con error.
     */
    private void cargarDatos(HttpServletRequest request, Usuario usuario) {
        try {
            GastosDAO dao = new GastosDAO();
            boolean esSuperAdmin = "SuperAdministrador".equals(usuario.getNombreRol());
            int empFiltro = 0;
            if (esSuperAdmin) {
                String empParam = request.getParameter("emp");
                if (empParam != null && !empParam.isBlank()) {
                    try { empFiltro = Integer.parseInt(empParam); } catch (NumberFormatException ignored) {}
                }
                List<Emprendimiento> emps = new EmprendimientoDAO().listarActivos();
                request.setAttribute("emprendimientos", emps);
                request.setAttribute("empFiltro", empFiltro);
            } else {
                // EmpresaUtil corrige sesiones antiguas con idEmprendimiento=0
                empFiltro = EmpresaUtil.resolverEmprendimiento(usuario, request);
            }
            request.setAttribute("gastos",  dao.listar(empFiltro));
            request.setAttribute("metodos", dao.listarMetodosPago());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar los gastos.");
        }
    }

    /**
     * Obtiene el usuario en sesión y verifica que sea Admin o SuperAdmin.
     *
     * Retorna null si:
     *   - No hay sesión activa → redirige a /login
     *   - El rol es Empleado → redirige a /dashboard
     *
     * El llamador debe retornar inmediatamente si el resultado es null.
     */
    private Usuario getUsuario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Usuario u = session != null ? (Usuario) session.getAttribute("usuario") : null;

        if (u == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }

        String rol = u.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            // Empleado → no tiene acceso al módulo de gastos
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return null;
        }

        return u;
    }
}
