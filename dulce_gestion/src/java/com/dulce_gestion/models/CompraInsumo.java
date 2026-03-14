package com.dulce_gestion.models;

import java.math.BigDecimal;
import java.util.List;

/** Encabezado de una compra de insumos */
public class CompraInsumo {
    private int        id;
    private int        idUsuario;
    private String     nombreUsuario;
    private String     fechaCompra;
    private BigDecimal total;
    private int        idMetodoPago;
    private String     metodoPago;
    private List<DetalleCompraInsumo> items;

    public CompraInsumo() {}

    public int        getId()             { return id; }
    public int        getIdUsuario()      { return idUsuario; }
    public String     getNombreUsuario()  { return nombreUsuario; }
    public String     getFechaCompra()    { return fechaCompra; }
    public BigDecimal getTotal()          { return total; }
    public int        getIdMetodoPago()   { return idMetodoPago; }
    public String     getMetodoPago()     { return metodoPago; }
    public List<DetalleCompraInsumo> getItems() { return items; }

    public void setId(int id)                          { this.id = id; }
    public void setIdUsuario(int v)                    { this.idUsuario = v; }
    public void setNombreUsuario(String v)             { this.nombreUsuario = v; }
    public void setFechaCompra(String v)               { this.fechaCompra = v; }
    public void setTotal(BigDecimal v)                 { this.total = v; }
    public void setIdMetodoPago(int v)                 { this.idMetodoPago = v; }
    public void setMetodoPago(String v)                { this.metodoPago = v; }
    public void setItems(List<DetalleCompraInsumo> v)  { this.items = v; }
}
