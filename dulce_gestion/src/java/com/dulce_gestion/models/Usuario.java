package com.dulce_gestion.models;

/**
 * Representa los datos del usuario que se guardan en sesión
 * después de autenticarse correctamente.
 */
public class Usuario {

    private int    id;
    private String correo;
    private String estado;
    private int    idRol;
    private String nombreRol;
    private String nombreCompleto;

    public Usuario() {}

    // ── Getters ──────────────────────────────────────────────

    public int    getId()             { return id; }
    public String getCorreo()         { return correo; }
    public String getEstado()         { return estado; }
    public int    getIdRol()          { return idRol; }
    public String getNombreRol()      { return nombreRol; }
    public String getNombreCompleto() { return nombreCompleto; }

    // ── Setters ──────────────────────────────────────────────

    public void setId(int id)                    { this.id = id; }
    public void setCorreo(String correo)         { this.correo = correo; }
    public void setEstado(String estado)         { this.estado = estado; }
    public void setIdRol(int idRol)              { this.idRol = idRol; }
    public void setNombreRol(String nombreRol)   { this.nombreRol = nombreRol; }
    public void setNombreCompleto(String nombre) { this.nombreCompleto = nombre; }
}
