package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.InsumosDAO;
import com.dulce_gestion.models.DetalleCompraInsumo;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * Devuelve los ítems de una compra en formato JSON.
 * Usado por el JS del JSP de compras para mostrar el detalle expandible.
 * URL: GET /compras/detalle?id=X
 */
@WebServlet("/compras/detalle")
public class ComprasDetalleServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificar sesión
        HttpSession session = request.getSession(false);
        Usuario u = session != null ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null) { response.sendError(401); return; }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) { response.sendError(400); return; }

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            List<DetalleCompraInsumo> items = new InsumosDAO().listarDetalle(Integer.parseInt(idParam));
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < items.size(); i++) {
                DetalleCompraInsumo d = items.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                  .append("\"nombre\":\"").append(esc(d.getNombreInsumo())).append("\",")
                  .append("\"unidad\":\"").append(esc(d.getUnidad())).append("\",")
                  .append("\"cantidad\":").append(d.getCantidad()).append(",")
                  .append("\"precio\":").append(d.getPrecioUnitario()).append(",")
                  .append("\"subtotal\":").append(d.getSubtotal())
                  .append("}");
            }
            sb.append("]");
            out.print(sb.toString());

        } catch (SQLException | NumberFormatException e) {
            out.print("[]");
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
