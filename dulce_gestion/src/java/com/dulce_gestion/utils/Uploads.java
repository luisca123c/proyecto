package com.dulce_gestion.utils;

import jakarta.servlet.ServletContext;
import java.io.File;

/**
 * ============================================================
 * UTILIDAD: Uploads
 * Usada por: NuevoProductoServlet, EditarProductoServlet,
 *            EliminarProductoServlet
 * ============================================================
 *
 */
public class Uploads {

    /**
     * Ruta relativa al contexto web donde se almacenan las imágenes de productos.
     * Se usa para construir la URL que se guarda en la BD y la ruta de las carpetas.
     */
    public static final String RUTA_RELATIVA = "assets/images/productos";

    /** Constructor privado: clase utilitaria, no debe instanciarse. */
    private Uploads() {}

    /**
     * Retorna la carpeta física activa de Tomcat donde se guardan las imágenes.
     *
     * Esta es la carpeta que Tomcat sirve en tiempo real. Un archivo copiado
     * aquí es inmediatamente accesible por el navegador sin reiniciar el servidor.
     *
     * Ruta resultante (ejemplo en Windows):
     *   C:\...\dulce_gestion\build\web\assets\images\productos\
     *
     * Si la carpeta no existe (primera vez), se crea automáticamente con mkdirs().
     * mkdirs() crea todos los directorios intermedios necesarios, no solo el último.
     *
     * @param ctx  contexto del Servlet (se obtiene con getServletContext() en el Servlet)
     * @return     carpeta donde guardar la imagen para que sea servida inmediatamente
     */
    public static File carpetaProductos(ServletContext ctx) {
        File webDir  = new File(ctx.getRealPath("/"));   // Raíz del deploy activo: build/web/
        File carpeta = new File(webDir, RUTA_RELATIVA);  // build/web/assets/images/productos/
        if (!carpeta.exists()) carpeta.mkdirs();         // Crear si no existe (primera imagen)
        return carpeta;
    }

    /**
     * Retorna la carpeta fuente del proyecto para persistir imágenes entre builds.
     *
     * Problema que resuelve:
     * Al hacer "Clean & Build" en NetBeans, build/web/ se borra y recrea
     * copiando todo desde web/. Si la imagen solo está en build/web/,
     * desaparece después del build. Guardándola también en web/, sobrevive.
     *
     * ¿Cómo se llega a web/ desde build/web/?
     *   ctx.getRealPath("/")     →  .../dulce_gestion/build/web/
     *   getParentFile()          →  .../dulce_gestion/build/
     *   getParentFile()          →  .../dulce_gestion/          (raíz del proyecto)
     *   new File(..., "web/...") →  .../dulce_gestion/web/assets/images/productos/
     *
     * Este método puede retornar null si:
     *   - El proyecto está desplegado en un servidor externo donde build/web/
     *     no tiene la estructura de directorios esperada.
     *   - No hay permisos de escritura en la carpeta fuente.
     * En ese caso el catch retorna null y el Servlet lo ignora sin fallar.
     *
     * @param ctx  contexto del Servlet
     * @return     carpeta fuente web/assets/images/productos/, o null si no es accesible
     */
    public static File carpetaProductosFuente(ServletContext ctx) {
        try {
            File buildWeb = new File(ctx.getRealPath("/"));           // build/web/
            File proyecto = buildWeb.getParentFile().getParentFile(); // raíz del proyecto
            File fuente   = new File(proyecto, "web/" + RUTA_RELATIVA); // web/assets/images/productos/
            if (!fuente.exists()) fuente.mkdirs();
            return fuente;
        } catch (Exception e) {
            // En deploys externos la estructura de directorios no existe → retornar null
            // Los Servlets verifican if (fuente != null) antes de usar esta carpeta
            return null;
        }
    }

    /**
     * Construye la URL relativa de una imagen para guardar en la BD y usar en <img src>.
     *
     * Ejemplo:
     *   urlRelativa("producto_3.jpg") → "assets/images/productos/producto_3.jpg"
     *
     * El JSP la combina con el contextPath para la URL completa:
     *   <img src="${pageContext.request.contextPath}/assets/images/productos/producto_3.jpg">
     *
     */
    public static String urlRelativa(String nombreArchivo) {
        return RUTA_RELATIVA + "/" + nombreArchivo;
    }
}
