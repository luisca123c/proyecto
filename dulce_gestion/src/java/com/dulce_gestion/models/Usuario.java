package com.dulce_gestion.models;

/*
 * Representa al usuario autenticado o a un usuario listado.
 * Incluye teléfono y género para el formulario de edición.
 */
public class Usuario {

    private int    id;
    private String correo;
    private String estado;
    private int    idRol;
    private String nombreRol;
    private String nombreCompleto;
    private String telefono;   // para edición
    private String genero;     // para edición

    public Usuario() {}

    // Getters
    public int    getId()             { return id; }
    public String getCorreo()         { return correo; }
    public String getEstado()         { return estado; }
    public int    getIdRol()          { return idRol; }
    public String getNombreRol()      { return nombreRol; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getTelefono()       { return telefono != null ? telefono : ""; }
    public String getGenero()         { return genero  != null ? genero   : ""; }

    // Setters
    public void setId(int id)                    { this.id = id; }
    public void setCorreo(String correo)         { this.correo = correo; }
    public void setEstado(String estado)         { this.estado = estado; }
    public void setIdRol(int idRol)              { this.idRol = idRol; }
    public void setNombreRol(String nombreRol)   { this.nombreRol = nombreRol; }
    public void setNombreCompleto(String nombre) { this.nombreCompleto = nombre; }
    public void setTelefono(String telefono)     { this.telefono = telefono; }
    public void setGenero(String genero)         { this.genero = genero; }
}
