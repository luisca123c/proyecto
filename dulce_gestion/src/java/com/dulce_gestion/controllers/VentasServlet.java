package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.CarritoDAO;
import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.utils.EmpresaUtil;
import com.dulce_gestion.models.CarritoItem;
import com.dulce_gestion.models.Emprendimiento;
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

/**
 * ============================================================
 * SERVLET: VentasServlet
 * URL:     /ventas
 * MÉTODOS: GET, POST
 * ============================================================
 *
 */
@WebServlet("/ventas")
public class VentasServlet extends HttpServlet {

    /** Ruta interna del JSP del carrito de ventas */
    private static final String VISTA = "/WEB-INF/jsp/ventas/carrito.jsp";

    // =========================================================
    // GET /ventas
    // =========================================================

    /**
     * Muestra el carrito activo del usuario con el catálogo de productos.
     *
     * @param request  contiene la sesión activa y posibles params ?exito= / ?error=
     * @param response para redirigir si no hay sesión, o forward al JSP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return; // Ya se redirigió al login

        cargarCarrito(request, usuario); // Cargar todos los datos del carrito
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // POST /ventas
    // =========================================================

    /**
     * Procesa la acción del carrito indicada por el parámetro "accion".
     *
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = getUsuario(request, response);
        if (usuario == null) return;

        String accion  = request.getParameter("accion"); // "agregar", "actualizar", "eliminar", "vaciar", "confirmar"
        CarritoDAO dao = new CarritoDAO();

        try {
            // Obtener el carrito activo, o crear uno nuevo si es la primera vez
            int idCarrito = dao.obtenerOCrearCarrito(usuario.getId());

            switch (accion == null ? "" : accion) {

                // ── Acción: agregar un producto al carrito ────────────────
                case "agregar": {
                    int idProducto = Integer.parseInt(request.getParameter("idProducto"));
                    int cantidad   = Integer.parseInt(request.getParameter("cantidad"));
                    if (cantidad < 1) cantidad = 1; // Mínimo 1 unidad
                    // El DAO verifica stock antes de agregar
                    dao.agregarProducto(idCarrito, idProducto, cantidad);
                    break;
                }

                // ── Acción: actualizar la cantidad de un ítem del carrito ─
                case "actualizar": {
                    int idDetalle     = Integer.parseInt(request.getParameter("idDetalle")); // PK en detalle_carrito
                    int nuevaCantidad = Integer.parseInt(request.getParameter("cantidad"));
                    // El DAO verifica stock y elimina el ítem si cantidad = 0
                    dao.actualizarCantidad(idDetalle, nuevaCantidad);
                    break;
                }

                // ── Acción: quitar un ítem específico del carrito ─────────
                case "eliminar": {
                    int idDetalle = Integer.parseInt(request.getParameter("idDetalle"));
                    dao.eliminarItem(idDetalle); // Borra la fila en detalle_carrito
                    break;
                }

                // ── Acción: vaciar todos los ítems del carrito ────────────
                case "vaciar": {
                    dao.vaciarCarrito(idCarrito); // DELETE en detalle_carrito para este carrito
                    break;
                }

                // ── Acción: confirmar la venta (la más importante) ────────
                case "confirmar": {
                    int idMetodoPago = Integer.parseInt(request.getParameter("idMetodoPago"));

                    // confirmarVenta() en una transacción:
                    //   1. Verifica stock de todos los ítems
                    //   2. Descuenta stock de cada producto
                    //   3. Inserta registro en ventas
                    //   4. Vacía el carrito
                    int idVenta = dao.confirmarVenta(idCarrito, usuario.getId(), idMetodoPago);

                    // PRG Pattern: redirect para evitar re-envío del POST al recargar
                    // ?id=X permite que el JSP muestre "Venta #X registrada exitosamente"
                    response.sendRedirect(request.getContextPath() + "/ventas?exito=venta&id=" + idVenta);
                    return; // Detener — no continuar al forward al final del método
                }

                default:
                    // Acción desconocida → no hacer nada, recargar el carrito
                    break;
            }

        } catch (NumberFormatException e) {
            // Algún ID o cantidad no era un número válido (¿manipulación del form?)
            request.setAttribute("error", "Datos inválidos.");
        } catch (SQLException e) {
            e.printStackTrace();
            // Puede ser "Stock insuficiente" o cualquier error de BD
            request.setAttribute("error", e.getMessage());
        }

        // Para todas las acciones excepto "confirmar":
        // Recargar el carrito y mostrar el estado actualizado
        cargarCarrito(request, usuario);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // MÉTODOS AUXILIARES (HELPERS)
    // =========================================================

    /**
     * Carga todos los datos que necesita el JSP del carrito y los pone
     * como atributos del request.
     *
     * Datos cargados:
     *   idCarrito  → ID del carrito activo del usuario
     *   items      → lista de CarritoItem con los productos en el carrito
     *   productos  → catálogo de productos disponibles (stock > 0)
     *   metodos    → métodos de pago para el modal de confirmación
     *   total      → suma de subtotales (BigDecimal)
     *
     * En caso de error de BD, se pone un mensaje de error y los datos
     * quedan vacíos — el JSP mostrará el carrito vacío con el error.
     *
     * @param request   para poner los atributos
     * @param idUsuario ID del usuario propietario del carrito
     */
    private void cargarCarrito(HttpServletRequest request, Usuario usuario) {
        int idUsuario        = usuario.getId();
        int idEmprendimiento = EmpresaUtil.resolverEmprendimiento(usuario, request);
        boolean esSuperAdmin = "SuperAdministrador".equals(usuario.getNombreRol());
        try {
            CarritoDAO dao = new CarritoDAO();

            int idCarrito = dao.obtenerOCrearCarrito(idUsuario);
            List<CarritoItem> items = dao.listarItems(idCarrito);

            // Detectar el emprendimiento de los ítems ya en el carrito
            int empCarrito = dao.obtenerEmprendimientoDelCarrito(idCarrito);

            // SuperAdmin: lee ?emp= de la URL como filtro visual del catálogo
            // Pero si el carrito ya tiene ítems, el filtro se fuerza al emprendimiento del carrito
            int empFiltroProductos;
            if (esSuperAdmin) {
                if (empCarrito > 0) {
                    empFiltroProductos = empCarrito; // Forzar al del carrito
                } else {
                    String empParam = request.getParameter("emp");
                    try { empFiltroProductos = (empParam != null) ? Integer.parseInt(empParam) : 0; }
                    catch (NumberFormatException e) { empFiltroProductos = 0; }
                }
                // Cargar emprendimientos para el desplegable
                List<Emprendimiento> emprendimientos = new EmprendimientoDAO().listarActivos();
                request.setAttribute("emprendimientos", emprendimientos);
                request.setAttribute("empFiltro",       empFiltroProductos);
            } else {
                empFiltroProductos = idEmprendimiento;
            }

            List<Producto>  productos = dao.listarProductosActivos(empFiltroProductos);
            List<String[]>  metodos   = dao.listarMetodosPago();
            BigDecimal      total     = dao.calcularTotal(items);

            request.setAttribute("idCarrito",   idCarrito);
            request.setAttribute("items",       items);
            request.setAttribute("productos",   productos);
            request.setAttribute("metodos",     metodos);
            request.setAttribute("total",       total);
            request.setAttribute("empCarrito",  empCarrito); // 0 = carrito vacío
            request.setAttribute("esSuperAdmin", esSuperAdmin);

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cargar el carrito.");
        }
    }

    /**
     * Obtiene el usuario en sesión. Si no hay sesión activa, redirige al login.
     * El llamador debe retornar inmediatamente si el resultado es null.
     *
     * @return Usuario en sesión, o null si no hay sesión (ya se redirigió)
     */
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
