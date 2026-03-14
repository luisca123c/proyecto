package com.dulce_gestion.models;

import java.math.BigDecimal;

/** Un ítem dentro de una compra de insumos */
public class DetalleCompraInsumo {
    private int        id;
    private int        idCompra;
    private int        idInsumo;
    private String     nombreInsumo;
    private String     unidad;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public DetalleCompraInsumo() {}

    public int        getId()             { return id; }
    public int        getIdCompra()       { return idCompra; }
    public int        getIdInsumo()       { return idInsumo; }
    public String     getNombreInsumo()   { return nombreInsumo; }
    public String     getUnidad()         { return unidad; }
    public BigDecimal getCantidad()       { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal()       { return subtotal; }

    public void setId(int id)                        { this.id = id; }
    public void setIdCompra(int v)                   { this.idCompra = v; }
    public void setIdInsumo(int v)                   { this.idInsumo = v; }
    public void setNombreInsumo(String v)            { this.nombreInsumo = v; }
    public void setUnidad(String v)                  { this.unidad = v; }
    public void setCantidad(BigDecimal v)            { this.cantidad = v; }
    public void setPrecioUnitario(BigDecimal v)      { this.precioUnitario = v; }
    public void setSubtotal(BigDecimal v)            { this.subtotal = v; }
}
