package com.dulce_gestion.utils;

import jakarta.servlet.ServletContext;
import java.io.File;

/*
 * Las imágenes se guardan DENTRO del proyecto en:
 *   web/assets/images/productos/
 *
 * Al estar dentro del deploy de Tomcat, son accesibles como
 * recursos estáticos normales:
 *   <img src="${pageContext.request.contextPath}/assets/images/productos/producto_3.jpg">
 *
 * Esto las hace completamente portables — al mover el proyecto
 * a otro equipo las imágenes viajan con él.
 *
 * NOTA: Al hacer Clean & Build en NetBeans, la carpeta build/web/
 * se recrea desde web/, así que las imágenes nuevas subidas en
 * tiempo de ejecución quedan en build/web/assets/images/productos/.
 * Para que persistan en el fuente, se guardan en AMBAS rutas.
 */
public class Uploads {

    public static final String RUTA_RELATIVA = "assets/images/productos";

    /*
     * Carpeta física donde guardar imágenes nuevas (en el deploy activo).
     * Esta es la carpeta que Tomcat está sirviendo en este momento.
     */
    public static File carpetaProductos(ServletContext ctx) {
        File webDir  = new File(ctx.getRealPath("/"));
        File carpeta = new File(webDir, RUTA_RELATIVA);
        if (!carpeta.exists()) carpeta.mkdirs();
        return carpeta;
    }

    /*
     * También guarda en el fuente del proyecto para que persista
     * después de un Clean & Build.
     * Si no se puede (ej: deploy en servidor externo), no falla.
     */
    public static File carpetaProductosFuente(ServletContext ctx) {
        try {
            // build/web/ -> build/ -> proyecto/ -> web/assets/images/productos
            File buildWeb = new File(ctx.getRealPath("/"));
            File proyecto = buildWeb.getParentFile().getParentFile();
            File fuente   = new File(proyecto, "web/" + RUTA_RELATIVA);
            if (!fuente.exists()) fuente.mkdirs();
            return fuente;
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * URL relativa para usar en <img src="...">.
     * Ejemplo: "assets/images/productos/producto_3.jpg"
     */
    public static String urlRelativa(String nombreArchivo) {
        return RUTA_RELATIVA + "/" + nombreArchivo;
    }
}
