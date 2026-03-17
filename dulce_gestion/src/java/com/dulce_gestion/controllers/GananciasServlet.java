package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.utils.EmpresaUtil;
import com.dulce_gestion.dao.GananciasDAO;
import com.dulce_gestion.models.Emprendimiento;
import java.util.List;
import com.dulce_gestion.models.Usuario;

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
 * SERVLET: GananciasServlet
 * URL:     /ganancias
 * MÉTODOS: GET
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Calcula y muestra el resumen financiero del negocio para un período
 * de tiempo. Todos los roles autenticados pueden acceder, pero ven
 * información diferente según su rol.
 *
 * ¿QUÉ PERÍODOS SOPORTA?
 * -----------------------
 * El parámetro ?periodo= controla el rango de fechas:
 *
 *   GET /ganancias               → semana actual (valor por defecto)
 *   GET /ganancias?periodo=semana → lunes a domingo de la semana actual
 *   GET /ganancias?periodo=mes    → mes actual completo
 *   GET /ganancias?periodo=2025-01 → enero 2025 (formato YYYY-MM)
 *
 * ¿QUIÉN VE QUÉ?
 * ---------------
 * El flag esAdminOSuper controla la visibilidad de la información:
 *
 *   Admin / SuperAdmin:
 *     - Ve TODAS las ventas del período (de todos los empleados)
 *     - Ve todos los gastos del período
 *     - Ganancia = total ventas − total gastos
 *
 *   Empleado:
 *     - Ve SOLO sus propias ventas del período
 *     - NO ve los gastos (información financiera restringida)
 *     - La ganancia mostrada es solo de sus ventas (sin restar gastos)
 *
 * ¿QUÉ ES GananciasDAO.ResumenPeriodo?
 * -------------------------------------
 * Es una clase interna de GananciasDAO que agrupa todos los datos
 * calculados: totalVentas, totalGastos, ganancia, las listas de filas,
 * y las fechas de inicio/fin del período.
 *
 * El JSP recibe este objeto y accede directamente a sus campos:
 *   resumen.totalVentas, resumen.ventas, resumen.ganancia, etc.
 *
 * ¿POR QUÉ SE PASAN TAMBIÉN "meses" Y "periodo" AL JSP?
 * --------------------------------------------------------
 * "meses" → la lista de los últimos 12 meses para el <select> del filtro.
 *            El usuario puede seleccionar un mes histórico específico.
 * "periodo" → el período actual seleccionado, para que el <select>
 *              muestre la opción correcta como seleccionada.
 */
@WebServlet("/ganancias")
public class GananciasServlet extends HttpServlet {

    /** Ruta interna del JSP del módulo de ganancias */
    private static final String VISTA = "/WEB-INF/jsp/ganancias/ganancias.jsp";

    /**
     * GET /ganancias → calcula el resumen financiero y hace forward al JSP.
     *
     * FLUJO PASO A PASO:
     *
     * Paso 1 — Verificar sesión activa.
     *
     * Paso 2 — Determinar el rol para saber qué datos mostrar.
     *   esAdminOSuper = true  → ve todo (ventas + gastos)
     *   esAdminOSuper = false → solo sus propias ventas
     *
     * Paso 3 — Leer el parámetro ?periodo= de la URL.
     *   Si falta o está vacío → usar "semana" como valor por defecto.
     *
     * Paso 4 — Llamar a GananciasDAO.obtenerResumen() y listarMesesDisponibles().
     *   El DAO calcula las fechas de inicio/fin, consulta ventas y gastos,
     *   y retorna el ResumenPeriodo con todos los datos calculados.
     *
     * Paso 5 — Poner los datos en el request y hacer forward al JSP.
     *
     * @param request  contiene la sesión y el parámetro ?periodo=
     * @param response para redirigir si no hay sesión, o forward al JSP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Paso 1: verificar sesión activa ──────────────────────────────
        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) {
            // Sin sesión → redirigir al login
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // ── Paso 2: determinar qué datos puede ver según el rol ──────────
        String rol = usuario.getNombreRol();
        // true si es Admin o SuperAdmin → ve todas las ventas y todos los gastos
        boolean esAdminOSuper = "SuperAdministrador".equals(rol) || "Administrador".equals(rol);

        // ── Paso 3: leer el período solicitado ────────────────────────────
        String periodo = request.getParameter("periodo");
        // Si no se especificó período, mostrar la semana actual por defecto
        if (periodo == null || periodo.isBlank()) periodo = "semana";

        // ── Paso 4: calcular el resumen financiero ────────────────────────
        try {
            GananciasDAO dao = new GananciasDAO();

            // Filtro por emprendimiento para SuperAdmin
            boolean esSuperAdmin = "SuperAdministrador".equals(rol);
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
            request.setAttribute("resumen", dao.obtenerResumen(usuario.getId(), esAdminOSuper, periodo, empFiltro));

            // Lista de los últimos 12 meses para el selector del filtro de período
            request.setAttribute("meses",        dao.listarMesesDisponibles());

            // El rol del usuario (para que el JSP muestre/oculte la sección de gastos)
            request.setAttribute("esAdminOSuper", esAdminOSuper);

            // El período actual (para que el <select> muestre la opción correcta seleccionada)
            request.setAttribute("periodo",      periodo);

        } catch (SQLException e) {
            e.printStackTrace(); // Log en consola de Tomcat
            // En caso de error, el JSP muestra el mensaje y la página queda vacía
            request.setAttribute("error", "Error al cargar las ganancias: " + e.getMessage());
        }

        // ── Paso 5: forward al JSP ──────────────────────────────────────
        request.getRequestDispatcher(VISTA).forward(request, response);
    }
}
