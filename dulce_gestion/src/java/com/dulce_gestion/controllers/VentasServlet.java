package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.CarritoDAO;
import com.dulce_gestion.models.CarritoItem;
import com.dulce_gestion.models.Producto;
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
import java.util.List;

/*
 * Servlet principal del carrito de ventas.
 * GET  /ventas          → muestra el carrito + productos disponibles
 * POST /ventas          → acciones: agregar, actualizar, eliminar, vaciar, confirmar
 */
@WebServlet("/ventas")
public class VentasServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/ventas/carrito.jsp";

    /* ── GET: mostrar carrito ────────────────────────────── */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return;

        cargarCarrito(request, usuario.getId());
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    /* ── POST: acciones ──────────────────────────────────── */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return;

        String accion = request.getParameter("accion");
        CarritoDAO dao = new CarritoDAO();

        try {
            int idCarrito = dao.obtenerOCrearCarrito(usuario.getId());

            switch (accion == null ? "" : accion) {

                case "agregar": {
                    int idProducto = Integer.parseInt(request.getParameter("idProducto"));
                    int cantidad   = Integer.parseInt(request.getParameter("cantidad"));
                    if (cantidad < 1) cantidad = 1;
                    dao.agregarProducto(idCarrito, idProducto, cantidad);
                    break;
                }

                case "actualizar": {
                    int idDetalle    = Integer.parseInt(request.getParameter("idDetalle"));
                    int nuevaCantidad = Integer.parseInt(request.getParameter("cantidad"));
                    dao.actualizarCantidad(idDetalle, nuevaCantidad);
                    break;
                }

                case "eliminar": {
                    int idDetalle = Integer.parseInt(request.getParameter("idDetalle"));
                    dao.eliminarItem(idDetalle);
                    break;
                }

                case "vaciar": {
                    dao.vaciarCarrito(idCarrito);
                    break;
                }

                case "confirmar": {
                    int idMetodoPago = Integer.parseInt(request.getParameter("idMetodoPago"));
                    int idVenta = dao.confirmarVenta(idCarrito, usuario.getId(), idMetodoPago);
                    response.sendRedirect(request.getContextPath() + "/ventas?exito=venta&id=" + idVenta);
                    return;
                }

                default:
                    break;
            }

        } catch (NumberFormatException e) {
            request.setAttribute("error", "Datos inválidos.");
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", e.getMessage());
        }

        cargarCarrito(request, usuario.getId());
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    /* ── Helpers ─────────────────────────────────────────── */

    private void cargarCarrito(HttpServletRequest request, int idUsuario) {
        try {
            CarritoDAO dao = new CarritoDAO();
            int idCarrito  = dao.obtenerOCrearCarrito(idUsuario);
            List<CarritoItem> items    = dao.listarItems(idCarrito);
            List<Producto>    productos = dao.listarProductosActivos();
            List<String[]>    metodos   = dao.listarMetodosPago();
            BigDecimal        total     = dao.calcularTotal(items);

            request.setAttribute("idCarrito",  idCarrito);
            request.setAttribute("items",      items);
            request.setAttribute("productos",  productos);
            request.setAttribute("metodos",    metodos);
            request.setAttribute("total",      total);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar el carrito.");
        }
    }

    private Usuario getUsuario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Usuario u = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        return u;
    }
}
