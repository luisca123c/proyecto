package com.dulce_gestion.models;

/**
 * ============================================================
 * MODELO: Usuario
 * Tabla principal: usuarios
 * Tablas del JOIN: correos, roles, perfil_usuario, telefonos, generos
 * Usado por: casi todos los DAOs y Servlets
 * ============================================================
 *
 * ¿QUÉ ES UN MODELO (POJO)?
 * --------------------------
 * Un modelo (o POJO, Plain Old Java Object) es una clase Java simple
 * que representa una entidad del sistema. Solo tiene:
 *   - Campos privados (los datos)
 *   - Constructor vacío
 *   - Getters (leer datos) y Setters (escribir datos)
 *   - Sin lógica de negocio, sin SQL, sin HTTP
 *
 * ¿Para qué sirve esta separación?
 * Los DAOs consultan la BD y crean objetos Usuario.
 * Los Servlets reciben esos objetos y los ponen en el request.
 * Los JSPs los leen con ${usuario.nombreCompleto}.
 * Cada capa hace una sola cosa.
 *
 * ¿QUÉ CONTIENE ESTE MODELO?
 * ---------------------------
 * Usuario tiene DOS usos en el proyecto:
 *
 * 1. Usuario autenticado en sesión (cargado en LoginServlet):
 *    Solo se necesitan: id, correo, estado, idRol, nombreRol, nombreCompleto.
 *    Son los campos mínimos para verificar permisos y mostrar el nombre
 *    en el sidebar sin hacer consultas adicionales en cada petición.
 *
 * 2. Usuario listado/editado (cargado en EmpleadoDAO, EditarEmpleadoDAO, PerfilDAO):
 *    Se agregan: telefono, genero.
 *    Son los campos extra que se muestran en el formulario de edición
 *    y en la pantalla de perfil.
 *
 * Unificar ambos usos en una sola clase evita tener UsuarioSesion y
 * UsuarioDetalle por separado. Los campos extras simplemente quedan
 * null cuando no se necesitan.
 *
 * ¿POR QUÉ LOS GETTERS DE telefono Y genero RETORNAN "" EN VEZ DE null?
 * -----------------------------------------------------------------------
 * Cuando el objeto Usuario viene de LoginServlet (solo datos básicos),
 * telefono y genero no se cargan → quedan null.
 * Si el JSP hiciera ${usuario.telefono} y el valor fuera null,
 * algunos motores de templates podrían mostrar "null" como texto.
 * Retornar "" garantiza que el campo aparezca vacío, no como texto "null".
 */
public class Usuario {

    // ── Campos básicos (siempre presentes) ────────────────────────────────
    private int    id;
    private String correo;
    private String estado;          // "Activo" o "Inactivo"
    private int    idRol;           // FK numérico a la tabla roles
    private String nombreRol;       // "SuperAdministrador", "Administrador" o "Empleado"
    private String nombreCompleto;

    // ── Campos de perfil extendido (cargados solo al ver/editar el perfil) ─
    private String telefono;        // null cuando viene del login básico
    private String genero;          // null cuando viene del login básico
    private int    idEmprendimiento;       // 0 para SuperAdministrador
    private String nombreEmprendimiento;   // nombre del emprendimiento (para mostrar en listas)

    /** Constructor vacío requerido para que los DAOs puedan crear instancias
     *  con new Usuario() y luego poblar los campos con setters. */
    public Usuario() {}

    // =========================================================
    // GETTERS
    // =========================================================

    /** ID del usuario (PK en la tabla usuarios). */
    public int    getId()             { return id; }

    /** Correo electrónico normalizado (siempre en minúsculas). */
    public String getCorreo()         { return correo; }

    /** Estado de la cuenta: "Activo" o "Inactivo". */
    public String getEstado()         { return estado; }

    /** ID numérico del rol (FK a la tabla roles). */
    public int    getIdRol()          { return idRol; }

    /**
     * Nombre del rol como texto.
     * Usado en Servlets y JSPs para verificar permisos y mostrar etiquetas:
     *   if ("SuperAdministrador".equals(usuario.getNombreRol())) { ... }
     *   ${usuario.nombreRol}
     */
    public String getNombreRol()      { return nombreRol; }

    /** Nombre completo del usuario (del campo perfil_usuario.nombre_completo). */
    public String getNombreCompleto() { return nombreCompleto; }

    /**
     * Teléfono del usuario.
     * Retorna "" en vez de null para evitar que el JSP muestre "null"
     * cuando el objeto viene del login básico sin datos de perfil extendido.
     */
    public String getTelefono()       { return telefono != null ? telefono : ""; }

    /**
     * Nombre del género del usuario.
     * Retorna "" en vez de null por la misma razón que getTelefono().
     */
    public String getGenero()         { return genero  != null ? genero   : ""; }
    public int    getIdEmprendimiento()          { return idEmprendimiento; }
    public String getNombreEmprendimiento()      { return nombreEmprendimiento != null ? nombreEmprendimiento : ""; }

    // =========================================================
    // SETTERS
    // =========================================================

    public void setId(int id)                              { this.id = id; }
    public void setCorreo(String correo)                   { this.correo = correo; }
    public void setEstado(String estado)                   { this.estado = estado; }
    public void setIdRol(int idRol)                        { this.idRol = idRol; }
    public void setNombreRol(String nombreRol)             { this.nombreRol = nombreRol; }
    public void setNombreCompleto(String nombre)           { this.nombreCompleto = nombre; }
    public void setTelefono(String telefono)               { this.telefono = telefono; }
    public void setGenero(String genero)                   { this.genero = genero; }
    public void setIdEmprendimiento(int id)                { this.idEmprendimiento = id; }
    public void setNombreEmprendimiento(String nombre)     { this.nombreEmprendimiento = nombre; }
}
