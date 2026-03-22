package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.ComprasDAO;
import com.dulce_gestion.dao.ComprasDAO.FilaCompra;
import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.dao.UsuarioDAO;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.EmpresaUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Servlet para el módulo de Compras de Insumos.
 *
 * <ul>
 *   <li>{@code GET  /compras}              — lista el historial de compras. Si viene
 *       {@code ?editar=ID} precarga la compra en el modal de edición.</li>
 *   <li>{@code POST /compras?accion=crear}  — registra una nueva compra.</li>
 *   <li>{@code POST /compras?accion=editar} — actualiza una compra existente.</li>
 * </ul>
 *
 * <p>Solo accesible para roles {@code Administrador} y {@code SuperAdministrador}.</p>
 */
@WebServlet("/compras")
public class ComprasServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/compras/compras.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;
        Usuario usuario = (Usuario) request.getSession(false).getAttribute("usuario");

        String editarId = request.getParameter("editar");
        if (editarId != null) {
            try {
                FilaCompra ce = new ComprasDAO().obtenerPorId(Integer.parseInt(editarId));
                request.setAttribute("compraEditar", ce);
            } catch (Exception e) {
                request.setAttribute("error", "Compra no encontrada.");
            }
        }

        cargarDatos(request, usuario);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;

        Usuario usuario  = (Usuario) request.getSession(false).getAttribute("usuario");
        String accion    = request.getParameter("accion");
        ComprasDAO dao   = new ComprasDAO();

        try {
            String descripcion   = validar(request.getParameter("descripcion"),  "La descripcion es obligatoria.");
            String totalStr      = validar(request.getParameter("total"),         "El monto es obligatorio.");
            String metodoPagoStr = validar(request.getParameter("idMetodoPago"),  "Debes seleccionar un método de pago.");
            String fecha         = validar(request.getParameter("fecha"),         "La fecha es obligatoria.");

            BigDecimal total = new BigDecimal(totalStr.replace(",", "."));
            if (total.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("El monto debe ser mayor a cero.");

            int idMetodoPago = Integer.parseInt(metodoPagoStr);

            java.time.LocalDate fechaDate = java.time.LocalDate.parse(fecha);
            if (fechaDate.isAfter(java.time.LocalDate.now()))
                throw new IllegalArgumentException("La fecha no puede ser una fecha futura.");

            String fechaHora = fecha + " " + new SimpleDateFormat("HH:mm:ss").format(new Date());

            if ("editar".equals(accion)) {
                int idCompra = Integer.parseInt(request.getParameter("idCompra"));

                int idEmprendimientoCompra = 0;
                if ("SuperAdministrador".equals(usuario.getNombreRol())) {
                    String empEditR = request.getParameter("idEmpresaRegistro");
                    if (empEditR != null && !empEditR.isBlank()) {
                        try { idEmprendimientoCompra = Integer.parseInt(empEditR); } catch (NumberFormatException ignored) {}
                    }
                }
                dao.editar(idCompra, descripcion.trim(), total, idMetodoPago, fechaHora, idEmprendimientoCompra);
                response.sendRedirect(request.getContextPath() + "/compras?exito=editado");

            } else {
                int idEmpresaRegistro = 0;
                if ("SuperAdministrador".equals(usuario.getNombreRol())) {
                    String empR = request.getParameter("idEmpresaRegistro");
                    if (empR != null && !empR.isBlank()) {
                        try { idEmpresaRegistro = Integer.parseInt(empR); } catch (NumberFormatException ignored) {}
                    }
                }
                dao.registrar(usuario.getId(), descripcion.trim(), total,
                              idMetodoPago, fechaHora, idEmpresaRegistro);
                response.sendRedirect(request.getContextPath() + "/compras?exito=creado");
            }
            return;

        } catch (NumberFormatException e) {
            request.setAttribute("error", "Monto o ID invalido.");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error en base de datos: " + e.getMessage());
        }

        if ("editar".equals(accion)) {
            try {
                int id = Integer.parseInt(request.getParameter("idCompra"));
                request.setAttribute("compraEditar", dao.obtenerPorId(id));
            } catch (Exception ignored) {}
        }
        cargarDatos(request, usuario);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Carga la lista de compras y los métodos de pago como atributos del request.
     * Para el SuperAdmin también carga la lista de emprendimientos y el filtro activo.
     *
     * @param request el request donde se depositan los atributos para el JSP.
     * @param usuario el usuario en sesión.
     */
    private void cargarDatos(HttpServletRequest request, Usuario usuario) {
        try {
            ComprasDAO dao = new ComprasDAO();
            boolean esSuperAdmin = "SuperAdministrador".equals(usuario.getNombreRol());
            int empFiltro = 0;
            if (esSuperAdmin) {
                String empParam = request.getParameter("emp");
                if (empParam != null && !empParam.isBlank()) {
                    try { empFiltro = Integer.parseInt(empParam); } catch (NumberFormatException ignored) {}
                }
                request.setAttribute("emprendimientos", new EmprendimientoDAO().listarActivos());
                request.setAttribute("empFiltro", empFiltro);
            } else {
                empFiltro = EmpresaUtil.resolverEmprendimiento(usuario, request);
            }
            request.setAttribute("compras", dao.listar(empFiltro));
            request.setAttribute("metodos", dao.listarMetodosPago());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar las compras.");
        }
    }

    /**
     * Lanza {@link IllegalArgumentException} si {@code val} es nulo o vacío.
     *
     * @param val valor del parámetro del formulario.
     * @param msg mensaje de error a mostrar al usuario.
     * @return {@code val} si no está vacío.
     */
    private String validar(String val, String msg) {
        if (val == null || val.isBlank()) throw new IllegalArgumentException(msg);
        return val;
    }

    /**
     * Verifica que el usuario en sesión tenga rol de Administrador o SuperAdministrador.
     * Redirige a {@code /login} si no hay sesión activa, o a {@code /dashboard} si el
     * rol es Empleado.
     *
     * @return {@code true} si el acceso está permitido; {@code false} si ya se redirigió.
     */
    private boolean verificarAcceso(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Usuario u = session != null ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return false; }
        String rol = u.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return false;
        }
        return true;
    }
}
