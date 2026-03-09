package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.GastosDAO;
import com.dulce_gestion.dao.GastosDAO.FilaGasto;
import com.dulce_gestion.models.Usuario;

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

/*
 * GET  /gastos              → lista + modal agregar
 * GET  /gastos?editar=ID    → lista + modal editar pre-cargado
 * POST /gastos?accion=crear → registrar nuevo
 * POST /gastos?accion=editar → guardar cambios
 */
@WebServlet("/gastos")
public class GastosServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/gastos/gastos.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return;

        // Si viene ?editar=ID, cargar ese gasto para el modal
        String editarId = request.getParameter("editar");
        if (editarId != null) {
            try {
                GastosDAO dao = new GastosDAO();
                FilaGasto gastoEditar = dao.obtenerPorId(Integer.parseInt(editarId));
                request.setAttribute("gastoEditar", gastoEditar);
            } catch (Exception e) {
                request.setAttribute("error", "Gasto no encontrado.");
            }
        }

        cargarDatos(request);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return;

        String accion = request.getParameter("accion");
        GastosDAO dao = new GastosDAO();

        try {
            // Parámetros comunes
            String descripcion   = validar(request.getParameter("descripcion"), "La descripción es obligatoria.");
            String totalStr      = validar(request.getParameter("total"), "El monto es obligatorio.");
            String metodoPagoStr = request.getParameter("idMetodoPago");
            String fecha         = validar(request.getParameter("fecha"), "La fecha es obligatoria.");

            BigDecimal total = new BigDecimal(totalStr.replace(",", "."));
            if (total.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("El monto debe ser mayor a cero.");

            int idMetodoPago = Integer.parseInt(metodoPagoStr);
            String fechaDatetime = fecha + " " + new SimpleDateFormat("HH:mm:ss").format(new Date());

            if ("editar".equals(accion)) {
                int idGasto         = Integer.parseInt(request.getParameter("idGasto"));
                int idDetalleCompra = Integer.parseInt(request.getParameter("idDetalleCompra"));
                int idCompra        = Integer.parseInt(request.getParameter("idCompra"));
                dao.editar(idGasto, idDetalleCompra, idCompra, descripcion.trim(),
                           total, idMetodoPago, fechaDatetime);
                response.sendRedirect(request.getContextPath() + "/gastos?exito=editado");

            } else {
                dao.registrar(usuario.getId(), descripcion.trim(), total, idMetodoPago, fechaDatetime);
                response.sendRedirect(request.getContextPath() + "/gastos?exito=creado");
            }
            return;

        } catch (NumberFormatException e) {
            request.setAttribute("error", "Monto o ID inválido.");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error en base de datos: " + e.getMessage());
        }

        // Si hubo error, re-mostrar el modal correcto
        if ("editar".equals(accion)) {
            try {
                int idGasto = Integer.parseInt(request.getParameter("idGasto"));
                request.setAttribute("gastoEditar", dao.obtenerPorId(idGasto));
            } catch (Exception ignored) {}
        }
        cargarDatos(request);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    /* ── Helpers ─────────────────────────────────────────── */
    private String validar(String val, String msg) {
        if (val == null || val.isBlank()) throw new IllegalArgumentException(msg);
        return val;
    }

    private void cargarDatos(HttpServletRequest request) {
        try {
            GastosDAO dao = new GastosDAO();
            request.setAttribute("gastos",  dao.listar());
            request.setAttribute("metodos", dao.listarMetodosPago());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar los gastos.");
        }
    }

    private Usuario getUsuario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Usuario u = session != null ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return null; }
        String rol = u.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return null;
        }
        return u;
    }
}
