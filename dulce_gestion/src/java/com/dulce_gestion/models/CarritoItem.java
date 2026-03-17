package com.dulce_gestion.models;

import java.math.BigDecimal;

/**
 * ============================================================
 * MODELO: CarritoItem
 * Tablas del JOIN: detalle_carrito + productos + imagenes_producto
 * Usado por: CarritoDAO, VentasServlet
 * ============================================================
 *
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
