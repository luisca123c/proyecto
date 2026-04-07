package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.CarritoDAO;
import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.models.CarritoItem;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.models.Producto;
import com.dulce_gestion.models.Usuario;
import com.dulce_gestion.utils.EmpresaUtil;

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

/**
 * Servlet del carrito de ventas.
 *
 * Métodos soportados:
 * - GET  /ventas - muestra el carrito activo del usuario junto con
 *     el catálogo de productos disponibles.
 * - POST /ventas?accion=agregar    - agrega un producto al carrito.
 * - POST /ventas?accion=actualizar - modifica la cantidad de un ítem.
 * - POST /ventas?accion=eliminar   - quita un ítem del carrito.
 * - POST /ventas?accion=vaciar     - vacía todos los ítems del carrito.
 * - POST /ventas?accion=confirmar  - confirma la venta: descuenta stock,
 *     registra la venta y abre un carrito nuevo.
 *
 * El emprendimiento de la venta se toma directamente de los productos presentes
 * en el carrito (todos pertenecen al mismo emprendimiento via
 * productos.id_emprendimiento), sin depender del emprendimiento del usuario.
 */
@WebServlet("/ventas")
public class VentasServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/ventas/carrito.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return;
        cargarCarrito(request, usuario);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return;

        String accion  = request.getParameter("accion");
        CarritoDAO dao = new CarritoDAO();

        try {
            int idCarrito = dao.obtenerOCrearCarrito(usuario.getId());

            switch (accion == null ? "" : accion) {

                case "agregar": {
                    String idProdStr = request.getParameter("idProducto");
                    String cantStr   = request.getParameter("cantidad");
                    if (idProdStr == null || idProdStr.isBlank() || cantStr == null || cantStr.isBlank())
                        throw new NumberFormatException("Datos de producto incompletos.");
                    int cantidad = Integer.parseInt(cantStr);
                    if (cantidad < 1) cantidad = 1;
                    dao.agregarProducto(idCarrito, Integer.parseInt(idProdStr), cantidad);
                    break;
                }

                case "actualizar": {
                    String idDetStr = request.getParameter("idDetalle");
                    String cantStr  = request.getParameter("cantidad");
                    if (idDetStr == null || idDetStr.isBlank() || cantStr == null || cantStr.isBlank())
                        throw new NumberFormatException("Datos de actualización incompletos.");
                    dao.actualizarCantidad(Integer.parseInt(idDetStr), Integer.parseInt(cantStr));
                    break;
                }

                case "eliminar": {
                    String idDetStr = request.getParameter("idDetalle");
                    if (idDetStr == null || idDetStr.isBlank())
                        throw new NumberFormatException("Ítem de carrito no especificado.");
                    dao.eliminarItem(Integer.parseInt(idDetStr));
                    break;
                }

                case "vaciar": {
                    dao.vaciarCarrito(idCarrito);
                    break;
                }

                case "confirmar": {
                    String mpStr = request.getParameter("idMetodoPago");
                    if (mpStr == null || mpStr.isBlank())
                        throw new SQLException("Debes seleccionar un método de pago.");
                    int idMetodoPago = Integer.parseInt(mpStr);

                    // El emprendimiento de la venta se toma de los productos del carrito,
                    // ya que todos pertenecen al mismo via productos.id_emprendimiento.
                    int idEmprendimiento = dao.obtenerEmprendimientoDelCarrito(idCarrito);

                    int idVenta = dao.confirmarVenta(idCarrito, usuario.getId(),
                                                     idMetodoPago, idEmprendimiento);
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

        cargarCarrito(request, usuario);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Carga todos los datos del carrito como atributos del request para el JSP.
     *
     * Para el SuperAdmin, si el carrito ya tiene ítems el catálogo se filtra
     * automáticamente al emprendimiento de esos ítems. Si está vacío, se respeta
     * el parámetro ?emp= de la URL.
     *
     * Atributos depositados: idCarrito, items, productos,
     * metodos, total, empCarrito, esSuperAdmin,
     * y para SuperAdmin también emprendimientos y empFiltro.
     *
     * @param request   request donde se depositan los atributos.
     * @param usuario   usuario propietario del carrito.
     */
    private void cargarCarrito(HttpServletRequest request, Usuario usuario) {
        boolean esSuperAdmin = "SuperAdministrador".equals(usuario.getNombreRol());
        try {
            CarritoDAO dao = new CarritoDAO();

            int idCarrito = dao.obtenerOCrearCarrito(usuario.getId());
            List<CarritoItem> items = dao.listarItems(idCarrito);
            int empCarrito = dao.obtenerEmprendimientoDelCarrito(idCarrito);

            int empFiltroProductos;
            if (esSuperAdmin) {
                if (empCarrito > 0) {
                    empFiltroProductos = empCarrito;
                } else {
                    String empParam = request.getParameter("emp");
                    try { empFiltroProductos = (empParam != null) ? Integer.parseInt(empParam) : 0; }
                    catch (NumberFormatException e) { empFiltroProductos = 0; }
                }
                request.setAttribute("emprendimientos", new EmprendimientoDAO().listarActivos());
                request.setAttribute("empFiltro", empFiltroProductos);
            } else {
                empFiltroProductos = EmpresaUtil.resolverEmprendimiento(usuario, request);
            }

            List<Producto>  productos = dao.listarProductosActivos(empFiltroProductos);
            List<String[]>  metodos   = dao.listarMetodosPago();
            BigDecimal      total     = dao.calcularTotal(items);

            request.setAttribute("idCarrito",   idCarrito);
            request.setAttribute("items",       items);
            request.setAttribute("productos",   productos);
            request.setAttribute("metodos",     metodos);
            request.setAttribute("total",       total);
            request.setAttribute("empCarrito",  empCarrito);
            request.setAttribute("esSuperAdmin", esSuperAdmin);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar el carrito.");
        }
    }

    /**
     * Obtiene el usuario en sesión. Redirige a {@code /login} si no hay sesión activa.
     *
     * @return el usuario en sesión, o {@code null} si ya se redirigió.
     */
    private Usuario getUsuario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Usuario u = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return null; }
        return u;
    }
}
