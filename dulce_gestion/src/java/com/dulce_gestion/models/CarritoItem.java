package com.dulce_gestion.models;

import java.math.BigDecimal;

/*
 * Representa una fila de detalle_carrito enriquecida con datos del producto.
 */
public class CarritoItem {

    private int        idDetalle;
    private int        idProducto;
    private String     nombreProducto;
    private String     pathImagen;
    private int        cantidad;
    private BigDecimal precioUnitario;
    private int        stockDisponible;

    public CarritoItem() {}

    public int        getIdDetalle()       { return idDetalle; }
    public int        getIdProducto()      { return idProducto; }
    public String     getNombreProducto()  { return nombreProducto; }
    public String     getPathImagen()      { return pathImagen != null ? pathImagen : ""; }
    public int        getCantidad()        { return cantidad; }
    public BigDecimal getPrecioUnitario()  { return precioUnitario; }
    public int        getStockDisponible() { return stockDisponible; }
    public BigDecimal getSubtotal()        { return precioUnitario.multiply(BigDecimal.valueOf(cantidad)); }

    public void setIdDetalle(int v)            { this.idDetalle = v; }
    public void setIdProducto(int v)           { this.idProducto = v; }
    public void setNombreProducto(String v)    { this.nombreProducto = v; }
    public void setPathImagen(String v)        { this.pathImagen = v; }
    public void setCantidad(int v)             { this.cantidad = v; }
    public void setPrecioUnitario(BigDecimal v){ this.precioUnitario = v; }
    public void setStockDisponible(int v)      { this.stockDisponible = v; }
}
