package com.dulce_gestion.models;

/*
 * Representa al usuario autenticado en el sistema.
 * El LoginServlet guarda un objeto de esta clase en la sesión HTTP,
 * así cualquier página puede saber quién está conectado y qué rol tiene.
 *
 * Los datos vienen de un JOIN entre las tablas:
 * usuarios, correos, roles y perfil_usuario.
 */
public class Usuario {

    private int    id;             // PK de la tabla usuarios
    private String correo;         // Correo (tabla correos)
    private String estado;         // "Activo" o "Inactivo"
    private int    idRol;          // ID del rol
    private String nombreRol;      // Nombre del rol (tabla roles)
    private String nombreCompleto; // Nombre para mostrar (tabla perfil_usuario)

    public Usuario() {}

    // Getters
    public int    getId()             { return id; }
    public String getCorreo()         { return correo; }
    public String getEstado()         { return estado; }
    public int    getIdRol()          { return idRol; }
    public String getNombreRol()      { return nombreRol; }
    public String getNombreCompleto() { return nombreCompleto; }

    // Setters
    public void setId(int id)                    { this.id = id; }
    public void setCorreo(String correo)         { this.correo = correo; }
    public void setEstado(String estado)         { this.estado = estado; }
    public void setIdRol(int idRol)              { this.idRol = idRol; }
    public void setNombreRol(String nombreRol)   { this.nombreRol = nombreRol; }
    public void setNombreCompleto(String nombre) { this.nombreCompleto = nombre; }
}
