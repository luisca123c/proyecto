package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.HistorialDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * GET  /historial          → lista de ventas según rol
 * GET  /historial?id=X     → ítems de una venta (responde JSON para AJAX)
 * Todos los roles autenticados pueden acceder.
 */
@WebServlet("/historial")
public class HistorialServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/ventas/historial.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Usuario u = session != null ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }

        boolean esAdminOSuper = "SuperAdministrador".equals(u.getNombreRol())
                             || "Administrador".equals(u.getNombreRol());

        // Si viene ?id= → responder JSON con ítems de esa venta (AJAX)
        String idParam = request.getParameter("id");
        if (idParam != null) {
            responderItems(request, response, u, esAdminOSuper, idParam);
            return;
        }

        // Normal → cargar lista de ventas y forward al JSP
        try {
            HistorialDAO dao = new HistorialDAO();
            request.setAttribute("ventas",       dao.listar(u.getId(), esAdminOSuper));
            request.setAttribute("esAdminOSuper", esAdminOSuper);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar el historial de ventas.");
        }
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    /** Responde JSON con los ítems de una venta (para el detalle expandible) */
    private void responderItems(HttpServletRequest request, HttpServletResponse response,
                                Usuario u, boolean esAdminOSuper, String idParam)
            throws IOException {

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            int idVenta = Integer.parseInt(idParam);
            var items = new HistorialDAO().listarItems(idVenta, u.getId(), esAdminOSuper);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < items.size(); i++) {
                var iv = items.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                  .append("\"producto\":\"").append(esc(iv.nombreProducto)).append("\",")
                  .append("\"cantidad\":").append(iv.cantidad).append(",")
                  .append("\"precio\":").append(iv.precioUnitario).append(",")
                  .append("\"subtotal\":").append(iv.subtotal)
                  .append("}");
            }
            sb.append("]");
            out.print(sb);
        } catch (Exception e) {
            out.print("[]");
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
