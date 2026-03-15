package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.ComprasDAO;
import com.dulce_gestion.dao.ComprasDAO.FilaCompra;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@WebServlet("/compras")
public class ComprasServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/compras/compras.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;

        String editarId = request.getParameter("editar");
        if (editarId != null) {
            try {
                FilaCompra ce = new ComprasDAO().obtenerPorId(Integer.parseInt(editarId));
                request.setAttribute("compraEditar", ce);
            } catch (Exception e) {
                request.setAttribute("error", "Compra no encontrada.");
            }
        }

        cargarDatos(request);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;

        Usuario usuario = (Usuario) request.getSession(false).getAttribute("usuario");
        String accion   = request.getParameter("accion");
        ComprasDAO dao  = new ComprasDAO();

        try {
            String descripcion   = validar(request.getParameter("descripcion"), "La descripcion es obligatoria.");
            String totalStr      = validar(request.getParameter("total"),       "El monto es obligatorio.");
            String metodoPagoStr = request.getParameter("idMetodoPago");
            String fecha         = validar(request.getParameter("fecha"),       "La fecha es obligatoria.");

            BigDecimal total = new BigDecimal(totalStr.replace(",", "."));
            if (total.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("El monto debe ser mayor a cero.");

            int idMetodoPago = Integer.parseInt(metodoPagoStr);
            // Validar que la fecha no sea futura
            java.time.LocalDate fechaDate = java.time.LocalDate.parse(fecha);
            if (fechaDate.isAfter(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("La fecha no puede ser una fecha futura.");
            }
            String fechaHora = fecha + " " + new SimpleDateFormat("HH:mm:ss").format(new Date());

            if ("editar".equals(accion)) {
                int idCompra = Integer.parseInt(request.getParameter("idCompra"));
                dao.editar(idCompra, descripcion.trim(), total, idMetodoPago, fechaHora);
                response.sendRedirect(request.getContextPath() + "/compras?exito=editado");
            } else {
                dao.registrar(usuario.getId(), descripcion.trim(), total, idMetodoPago, fechaHora);
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
        cargarDatos(request);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private void cargarDatos(HttpServletRequest request) {
        try {
            ComprasDAO dao = new ComprasDAO();
            request.setAttribute("compras",  dao.listar());
            request.setAttribute("metodos",  dao.listarMetodosPago());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar las compras.");
        }
    }

    private String validar(String val, String msg) {
        if (val == null || val.isBlank()) throw new IllegalArgumentException(msg);
        return val;
    }

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
