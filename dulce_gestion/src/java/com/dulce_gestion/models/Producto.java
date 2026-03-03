package com.dulce_gestion.models;

import java.math.BigDecimal;

/*
 * Modelo que representa un producto del catalogo.
 * Los datos vienen de un JOIN entre productos, categorias, unidad_medida
 * e imagenes_producto (LEFT JOIN, puede ser null).
 */
public class Producto {

    private int        id;
    private String     nombre;
    private String     descripcion;
    private int        stockActual;
    private BigDecimal precioUnitario;
    private String     estado;
    private String     fechaVencimiento;
    private int        idCategoria;
    private String     nombreCategoria;
    private int        idUnidad;
    private String     nombreUnidad;
    private String     pathImagen;  // Ruta relativa: assets/images/productos/archivo.jpg
    private String     altImagen;   // Texto alternativo para la imagen

    public Producto() {}

    public int        getId()              { return id; }
    public String     getNombre()          { return nombre; }
    public String     getDescripcion()     { return descripcion; }
    public int        getStockActual()     { return stockActual; }
    public BigDecimal getPrecioUnitario()  { return precioUnitario; }
    public String     getEstado()          { return estado; }
    public String     getFechaVencimiento(){ return fechaVencimiento; }
    public int        getIdCategoria()     { return idCategoria; }
    public String     getNombreCategoria() { return nombreCategoria; }
    public int        getIdUnidad()        { return idUnidad; }
    public String     getNombreUnidad()    { return nombreUnidad; }
    public String     getPathImagen()      { return pathImagen; }
    public String     getAltImagen()       { return altImagen; }

    public void setId(int id)                          { this.id = id; }
    public void setNombre(String nombre)               { this.nombre = nombre; }
    public void setDescripcion(String descripcion)     { this.descripcion = descripcion; }
    public void setStockActual(int stockActual)        { this.stockActual = stockActual; }
    public void setPrecioUnitario(BigDecimal precio)   { this.precioUnitario = precio; }
    public void setEstado(String estado)               { this.estado = estado; }
    public void setFechaVencimiento(String fecha)      { this.fechaVencimiento = fecha; }
    public void setIdCategoria(int idCategoria)        { this.idCategoria = idCategoria; }
    public void setNombreCategoria(String nombre)      { this.nombreCategoria = nombre; }
    public void setIdUnidad(int idUnidad)              { this.idUnidad = idUnidad; }
    public void setNombreUnidad(String nombre)         { this.nombreUnidad = nombre; }
    public void setPathImagen(String pathImagen)       { this.pathImagen = pathImagen; }
    public void setAltImagen(String altImagen)         { this.altImagen = altImagen; }
}
