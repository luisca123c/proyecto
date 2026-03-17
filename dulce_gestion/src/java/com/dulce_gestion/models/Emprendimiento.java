package com.dulce_gestion.models;

import java.time.LocalDateTime;

public class Emprendimiento {

    private int    id;
    private String nombre;
    private String nit;
    private String direccion;
    private String ciudad;
    private String telefono;
    private String correo;
    private String estado;           // "Activo" | "Inactivo"
    private String fechaCreacion;    // formateada para display

    public Emprendimiento() {}

    // ── Getters ──────────────────────────────────────────────────────────
    public int    getId()           { return id; }
    public String getNombre()       { return nombre; }
    public String getNit()          { return nit; }
    public String getDireccion()    { return direccion; }
    public String getCiudad()       { return ciudad; }
    public String getTelefono()     { return telefono; }
    public String getCorreo()       { return correo; }
    public String getEstado()       { return estado; }
    public String getFechaCreacion(){ return fechaCreacion; }

    // ── Setters ──────────────────────────────────────────────────────────
    public void setId(int id)                    { this.id = id; }
    public void setNombre(String nombre)         { this.nombre = nombre; }
    public void setNit(String nit)               { this.nit = nit; }
    public void setDireccion(String direccion)   { this.direccion = direccion; }
    public void setCiudad(String ciudad)         { this.ciudad = ciudad; }
    public void setTelefono(String telefono)     { this.telefono = telefono; }
    public void setCorreo(String correo)         { this.correo = correo; }
    public void setEstado(String estado)         { this.estado = estado; }
    public void setFechaCreacion(String f)       { this.fechaCreacion = f; }

    public boolean isActivo() { return "Activo".equals(estado); }
}
