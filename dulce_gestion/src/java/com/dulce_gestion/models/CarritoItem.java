package com.dulce_gestion.models;

import java.math.BigDecimal;

/**
 * ============================================================
 * MODELO: CarritoItem
 * Tablas del JOIN: detalle_carrito + productos + imagenes_producto
 * Usado por: CarritoDAO, VentasServlet
 * ============================================================
 *
 * ¿QUÉ REPRESENTA?
 * -----------------
 * Una fila del carrito enriquecida con datos del producto.
 * En la BD, detalle_carrito solo guarda (id_carrito, id_producto, cantidad).
 * Para mostrar el carrito en el JSP se necesitan más datos del producto:
 * su nombre, precio, stock y imagen.
 *
 * CarritoDAO.listarItems() hace el JOIN y construye estos objetos,
 * evitando que el JSP tenga que hacer consultas adicionales.
 *
 * ¿POR QUÉ EXISTE ESTE MODELO SEPARADO DE Producto?
 * ---------------------------------------------------
 * Producto representa un artículo del catálogo con todos sus datos
 * (descripción, categoría, unidad, fecha de vencimiento, etc.).
 *
 * CarritoItem representa una línea del carrito: solo necesita los
 * datos mínimos para mostrar la tabla del carrito y calcular el total.
 * Usar Producto completo sería un desperdicio de memoria y de columnas
 * en el SELECT cuando solo se necesitan 5 de sus 13 campos.
 *
 * ¿QUÉ ES getSubtotal() Y POR QUÉ ESTÁ EN EL MODELO?
 * -----------------------------------------------------
 * getSubtotal() calcula precio × cantidad para esa línea del carrito.
 * Es una derivación directa de los datos del objeto (no consulta la BD).
 * Al estar en el modelo, tanto el JSP como el DAO pueden usarlo:
 *   JSP:  ${item.subtotal}               → muestra el subtotal en la tabla
 *   DAO:  calcularTotal(items)           → suma todos los subtotales
 *
 * ¿POR QUÉ getPathImagen() RETORNA "" EN VEZ DE null?
 * -----------------------------------------------------
 * Un producto puede no tener imagen (LEFT JOIN en CarritoDAO devuelve null).
 * Si el JSP evaluara ${item.pathImagen} y fuera null, podría mostrar
 * "null" como texto o romper la construcción de la URL del <img>.
 * Retornar "" garantiza que la condición <c:if test="${not empty item.pathImagen}">
 * funcione correctamente sin verificación adicional en el JSP.
 */
public class CarritoItem {

    // ── Campos de detalle_carrito ─────────────────────────────────────────
    private int        idDetalle;       // PK de detalle_carrito (para actualizar/eliminar ítems)
    private int        idProducto;      // FK a productos (para agregar/quitar del carrito)
    private int        cantidad;        // Unidades pedidas de este producto

    // ── Campos del JOIN con productos ────────────────────────────────────
    private String     nombreProducto;  // Para mostrar en la tabla del carrito
    private BigDecimal precioUnitario;  // Para calcular subtotales
    private int        stockDisponible; // Para validar el máximo del input de cantidad

    // ── Campo del LEFT JOIN con imagenes_producto ─────────────────────────
    private String     pathImagen;      // Puede ser null si el producto no tiene imagen

    /** Constructor vacío requerido para que CarritoDAO pueda crear instancias
     *  con new CarritoItem() y poblar los campos con setters. */
    public CarritoItem() {}

    // =========================================================
    // GETTERS
    // =========================================================

    /** ID de la fila en detalle_carrito.
     *  VentasServlet lo usa como parámetro para actualizar y eliminar ítems. */
    public int        getIdDetalle()       { return idDetalle; }

    /** ID del producto en el carrito. */
    public int        getIdProducto()      { return idProducto; }

    /** Nombre del producto para mostrar en la tabla del carrito. */
    public String     getNombreProducto()  { return nombreProducto; }

    /**
     * Ruta relativa de la imagen del producto.
     * Retorna "" (no null) si el producto no tiene imagen, para que el
     * JSP pueda evaluarla con <c:if test="${not empty item.pathImagen}">
     * sin producir "null" como texto ni romper la URL del <img>.
     */
    public String     getPathImagen()      { return pathImagen != null ? pathImagen : ""; }

    /** Cantidad de unidades de este producto en el carrito. */
    public int        getCantidad()        { return cantidad; }

    /** Precio unitario del producto (BigDecimal para exactitud monetaria). */
    public BigDecimal getPrecioUnitario()  { return precioUnitario; }

    /** Stock disponible del producto.
     *  El JSP lo usa como atributo max del input de cantidad:
     *  <input type="number" max="${item.stockDisponible}"> */
    public int        getStockDisponible() { return stockDisponible; }

    /**
     * Calcula el subtotal de esta línea: precioUnitario × cantidad.
     *
     * ¿POR QUÉ BigDecimal.valueOf(cantidad) Y NO new BigDecimal(cantidad)?
     * ----------------------------------------------------------------------
     * BigDecimal.valueOf(int) crea el BigDecimal directamente desde el
     * valor entero sin riesgo de imprecisión. new BigDecimal(int) también
     * es exacto para enteros, pero valueOf es la forma idiomática preferida.
     * La multiplicación BigDecimal × BigDecimal mantiene la precisión exacta.
     *
     * Ejemplo: precio=2.50, cantidad=3 → subtotal=7.50 (exacto, sin 7.4999...)
     */
    public BigDecimal getSubtotal()        { return precioUnitario.multiply(BigDecimal.valueOf(cantidad)); }

    // =========================================================
    // SETTERS
    // =========================================================

    public void setIdDetalle(int v)             { this.idDetalle = v; }
    public void setIdProducto(int v)            { this.idProducto = v; }
    public void setNombreProducto(String v)     { this.nombreProducto = v; }
    public void setPathImagen(String v)         { this.pathImagen = v; }
    public void setCantidad(int v)              { this.cantidad = v; }
    public void setPrecioUnitario(BigDecimal v) { this.precioUnitario = v; }
    public void setStockDisponible(int v)       { this.stockDisponible = v; }
}
