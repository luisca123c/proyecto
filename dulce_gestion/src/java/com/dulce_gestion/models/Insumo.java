package com.dulce_gestion.models;

public class Insumo {
    private int    id;
    private String nombre;
    private int    idUnidad;
    private String unidad;      // nombre de la unidad (JOIN)
    private String descripcion;
    private String estado;

    public Insumo() {}

    public int    getId()          { return id; }
    public String getNombre()      { return nombre; }
    public int    getIdUnidad()    { return idUnidad; }
    public String getUnidad()      { return unidad; }
    public String getDescripcion() { return descripcion; }
    public String getEstado()      { return estado; }

    public void setId(int id)                  { this.id = id; }
    public void setNombre(String nombre)       { this.nombre = nombre; }
    public void setIdUnidad(int idUnidad)      { this.idUnidad = idUnidad; }
    public void setUnidad(String unidad)       { this.unidad = unidad; }
    public void setDescripcion(String d)       { this.descripcion = d; }
    public void setEstado(String estado)       { this.estado = estado; }
}
