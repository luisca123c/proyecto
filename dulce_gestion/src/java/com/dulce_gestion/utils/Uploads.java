package com.dulce_gestion.utils;

import java.io.File;

/*
 * Las imágenes se guardan en:
 *   [directorio_deploy_tomcat]/dulce_gestion_uploads/productos/
 *
 * getRealPath("/") apunta a donde Tomcat desplegó la app, por ejemplo:
 *   D:/ruta/proyecto/build/web/
 *
 * La carpeta "dulce_gestion_uploads" queda al mismo nivel que "build/web/",
 * es decir fuera del build — Tomcat no la borra al redesplegar.
 * Funciona igual en cualquier equipo sin configurar nada.
 */
public class Uploads {

    public static final String CARPETA_PRODUCTOS = "productos";

    /*
     * Retorna la carpeta física donde se guardan las imágenes de productos.
     * Crea la carpeta automáticamente si no existe.
     */
    public static File carpetaProductos(jakarta.servlet.ServletContext ctx) {
        // Subir dos niveles desde build/web/ → raíz del proyecto
        File webDir   = new File(ctx.getRealPath("/"));   // .../build/web
        File buildDir = webDir.getParentFile();            // .../build
        File projDir  = buildDir.getParentFile();          // raíz del proyecto

        File uploads  = new File(projDir, "dulce_gestion_uploads/productos");
        if (!uploads.exists()) uploads.mkdirs();
        return uploads;
    }

    /*
     * Directorio base de uploads (para que ImagenServlet resuelva rutas).
     */
    public static File directorioBase(jakarta.servlet.ServletContext ctx) {
        File webDir  = new File(ctx.getRealPath("/"));
        File projDir = webDir.getParentFile().getParentFile();
        return new File(projDir, "dulce_gestion_uploads");
    }

    /*
     * URL relativa para <img src="...">.
     * Ejemplo: "uploads/productos/producto_3.jpg"
     */
    public static String urlRelativa(String nombreArchivo) {
        return "uploads/productos/" + nombreArchivo;
    }
}
