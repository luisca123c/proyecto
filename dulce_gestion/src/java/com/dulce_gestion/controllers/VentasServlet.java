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

/**
 * ============================================================
 * SERVLET: VentasServlet
 * URL:     /ventas
 * MÉTODOS: GET, POST
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Maneja el carrito de ventas completo. Es el servlet más versátil
 * del proyecto porque atiende múltiples acciones con un solo doPost():
 *
 *   GET  /ventas                      → muestra el carrito con catálogo
 *   POST /ventas?accion=agregar       → agrega un producto al carrito
 *   POST /ventas?accion=actualizar    → cambia la cantidad de un ítem
 *   POST /ventas?accion=eliminar      → quita un ítem del carrito
 *   POST /ventas?accion=vaciar        → vacía todo el carrito
 *   POST /ventas?accion=confirmar     → registra la venta (cierre de venta)
 *
 * ¿QUIÉN PUEDE ACCEDER?
 * ----------------------
 * Todos los roles autenticados (SuperAdmin, Admin y Empleado).
 * El carrito es PERSONAL: cada usuario tiene el suyo en la BD.
 *
 * ¿CÓMO SE MANEJAN MÚLTIPLES ACCIONES EN UN SOLO doPost()?
 * ----------------------------------------------------------
 * Cada formulario del JSP incluye un campo oculto con la acción:
 *   <input type="hidden" name="accion" value="agregar">
 *
 * doPost() lee ese parámetro y usa un switch para ejecutar la
 * lógica de cada caso. Este es el patrón "Front Controller":
 * un solo punto de entrada que delega a sub-operaciones.
 *
 * ¿POR QUÉ "confirmar" HACE REDIRECT Y LAS OTRAS ACCIONES NO?
 * -------------------------------------------------------------
 * Después de confirmar una venta, si el usuario recarga la página,
 * el navegador re-enviaría el POST (el formulario de confirmación).
 * Eso registraría la venta dos veces y descontaría el stock dos veces.
 *
 * Solución: PRG Pattern (Post-Redirect-Get).
 * Al confirmar → redirect a /ventas?exito=venta&id=X
 * Así, si el usuario recarga, solo recarga el GET que muestra el carrito vacío.
 *
 * Las otras acciones (agregar, actualizar, etc.) usan forward porque
 * no se repiten si el usuario recarga — no tienen consecuencias financieras.
 *
 * ¿QUÉ HACE cargarCarrito()?
 * ---------------------------
 * Centraliza la carga de todos los datos que necesita el JSP:
 *   idCarrito → para los forms de modificar/eliminar ítem
 *   items     → lista de CarritoItem para la tabla del carrito
 *   productos → catálogo de productos disponibles (stock > 0)
 *   metodos   → métodos de pago para el modal de confirmación
 *   total     → suma de subtotales de todos los ítems
 *
 * Se llama tanto en el GET como al terminar cada acción del POST.
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

        cargarCarrito(request, usuario.getId()); // Cargar todos los datos del carrito
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // POST /ventas
    // =========================================================

    /**
     * Procesa la acción del carrito indicada por el parámetro "accion".
     *
     * FLUJO GENERAL:
     * 1. Verificar sesión activa.
     * 2. Leer el parámetro "accion".
     * 3. Obtener o crear el carrito activo del usuario.
     * 4. Switch sobre la acción → ejecutar la lógica correspondiente.
     * 5. En caso de error → poner mensaje de error en el request.
     * 6. Recargar el carrito y hacer forward al JSP.
     *    (Excepción: "confirmar" hace redirect en lugar de forward)
     *
     * @param request  contiene el formulario del carrito y el parámetro "accion"
     * @param response para redirigir en confirmar, o forward al JSP en los demás casos
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
        cargarCarrito(request, usuario.getId());
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
    private void cargarCarrito(HttpServletRequest request, int idUsuario) {
        try {
            CarritoDAO dao = new CarritoDAO();

            // Obtener o crear el carrito activo del usuario
            int idCarrito = dao.obtenerOCrearCarrito(idUsuario);

            // Cargar los ítems del carrito (JOIN con productos e imágenes)
            List<CarritoItem> items    = dao.listarItems(idCarrito);

            // Catálogo de productos con stock > 0 para el selector "Agregar producto"
            List<Producto>    productos = dao.listarProductosActivos();

            // Métodos de pago para el select del modal de confirmación
            List<String[]>    metodos   = dao.listarMetodosPago();

            // Calcular el total sumando los subtotales de todos los ítems
            BigDecimal        total     = dao.calcularTotal(items);

            // Poner todos los datos en el request para que el JSP los use
            request.setAttribute("idCarrito", idCarrito);
            request.setAttribute("items",     items);
            request.setAttribute("productos", productos);
            request.setAttribute("metodos",   metodos);
            request.setAttribute("total",     total);

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
