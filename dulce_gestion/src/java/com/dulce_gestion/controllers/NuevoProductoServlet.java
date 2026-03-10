package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.CrearProductoDAO;
import com.dulce_gestion.dao.ImagenProductoDAO;
import com.dulce_gestion.dao.ProductoDAO;
import com.dulce_gestion.utils.Uploads;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * ============================================================
 * SERVLET: NuevoProductoServlet
 * URL:     /productos/nuevo
 * MÉTODOS: GET, POST
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Maneja la creación de nuevos productos con imagen opcional.
 *   GET  → muestra el formulario vacío con los <select> de categoría y unidad cargados
 *   POST → valida los datos, inserta el producto en BD y guarda la imagen si se subió
 *
 * ¿QUIÉN PUEDE ACCEDER?
 * ----------------------
 * Solo SuperAdministrador y Administrador.
 * Los Empleados son redirigidos a /productos (solo lectura).
 *
 * ¿QUÉ ES @MultipartConfig?
 * -------------------------
 * Normalmente los formularios HTML envían datos codificados como:
 *   application/x-www-form-urlencoded
 * y se leen con request.getParameter("campo").
 *
 * Cuando un formulario incluye un <input type="file">, debe usar:
 *   enctype="multipart/form-data"
 * Este formato no puede leerse con getParameter().
 *
 * @MultipartConfig activa el soporte de multipart en este servlet,
 * permitiendo usar request.getPart("imagen") para acceder al archivo.
 * maxFileSize = 5MB limita el tamaño máximo aceptado.
 *
 * NOTA IMPORTANTE: Como web.xml tiene metadata-complete="true",
 * la anotación @MultipartConfig es ignorada por Tomcat.
 * Para que funcione, el equivalente <multipart-config> también debe
 * estar declarado en web.xml para este servlet.
 *
 * ¿POR QUÉ SE NECESITA EL ID DEL PRODUCTO ANTES DE GUARDAR LA IMAGEN?
 * ---------------------------------------------------------------------
 * El archivo de imagen se nombra "producto_{ID}.jpg".
 * Ese ID solo se conoce DESPUÉS de insertar el producto en la BD
 * (MySQL lo genera con AUTO_INCREMENT).
 *
 * Por eso el orden es:
 *   1. Insertar producto → CrearProductoDAO retorna el ID generado
 *   2. Usar ese ID para nombrar y guardar el archivo de imagen
 *   3. Registrar el path en la tabla imagenes_producto
 *
 * ¿POR QUÉ SE GUARDA LA IMAGEN EN DOS CARPETAS?
 * -----------------------------------------------
 * Tomcat sirve los archivos desde build/web/ (la carpeta compilada).
 * La carpeta fuente del proyecto es web/.
 * Si solo se guarda en build/web/, al hacer "Clean and Build" en
 * NetBeans esa carpeta se borra y la imagen desaparece.
 *
 * Solución: guardar en AMBAS carpetas.
 * build/web/ → visible ahora mismo en el navegador
 * web/        → sobrevive al próximo Clean and Build
 */
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // Máximo 5 MB por archivo
public class NuevoProductoServlet extends HttpServlet {

    /** Ruta interna del JSP del formulario */
    private static final String VISTA = "/WEB-INF/jsp/productos/nuevo.jsp";

    // =========================================================
    // GET /productos/nuevo
    // =========================================================

    /**
     * Muestra el formulario vacío con los selectores de categoría y unidad cargados.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return; // Solo Admin y SuperAdmin

        cargarSelectores(request); // Cargar opciones de los <select> del formulario
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // POST /productos/nuevo
    // =========================================================

    /**
     * Valida los datos del formulario, crea el producto y guarda la imagen.
     *
     * FLUJO PASO A PASO:
     *
     * Paso 1 — Verificar acceso (Admin o SuperAdmin).
     *
     * Paso 2 — Setear charset UTF-8:
     *   Necesario para que los acentos y caracteres especiales en el
     *   nombre y descripción del producto lleguen correctamente.
     *
     * Paso 3 — Leer los campos del formulario.
     *   Los campos de texto se leen con getParameter().
     *   La imagen se lee con getPart() — disponible gracias a @MultipartConfig.
     *
     * Paso 4 — Validar que los campos obligatorios no estén vacíos.
     *
     * Paso 5 — Parsear tipos numéricos (stock, precio, categoría, unidad).
     *   Integer.parseInt() y new BigDecimal() pueden lanzar NumberFormatException.
     *
     * Paso 6 — Validar que stock y precio no sean negativos.
     *
     * Paso 7 — Insertar el producto en la BD.
     *   CrearProductoDAO.crear() retorna el ID generado por AUTO_INCREMENT.
     *
     * Paso 8 — Guardar la imagen si el usuario subió un archivo.
     *   part.getSize() > 0 verifica que el archivo no está vacío.
     *
     * Paso 9 — Redirigir a /productos?exito=creado.
     *
     * @param request  contiene los campos del form y el archivo subido
     * @param response para redirigir en éxito o reenviar al form en error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;

        // ── Paso 2: charset para soportar caracteres especiales ──────────
        request.setCharacterEncoding("UTF-8");

        // ── Paso 3: leer parámetros del formulario ───────────────────────
        String nombre       = request.getParameter("nombre");
        String descripcion  = request.getParameter("descripcion");   // Campo opcional
        String stockStr     = request.getParameter("stock");
        String precioStr    = request.getParameter("precio");
        String estado       = request.getParameter("estado");        // "Disponible", "Agotado", "Inactivo"
        String fechaVenc    = request.getParameter("fechaVencimiento"); // Formato yyyy-MM-dd
        String categoriaStr = request.getParameter("idCategoria");   // ID numérico del select
        String unidadStr    = request.getParameter("idUnidad");      // ID numérico del select

        // ── Paso 4: validar campos obligatorios ──────────────────────────
        if (estaVacio(nombre) || estaVacio(stockStr) || estaVacio(precioStr)
                || estaVacio(estado) || estaVacio(fechaVenc)
                || estaVacio(categoriaStr) || estaVacio(unidadStr)) {
            cargarSelectores(request); // Re-cargar los selectores para el formulario
            request.setAttribute("error", "Todos los campos obligatorios deben completarse.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // ── Paso 5: parsear tipos numéricos ──────────────────────────────
        int stock; BigDecimal precio; int idCategoria; int idUnidad;
        try {
            stock       = Integer.parseInt(stockStr.trim());
            precio      = new BigDecimal(precioStr.trim()); // BigDecimal para precisión monetaria exacta
            idCategoria = Integer.parseInt(categoriaStr.trim());
            idUnidad    = Integer.parseInt(unidadStr.trim());
        } catch (NumberFormatException e) {
            // El usuario escribió letras donde debían ir números
            cargarSelectores(request);
            request.setAttribute("error", "Stock y precio deben ser numeros validos.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // ── Paso 6: validar que no sean negativos ─────────────────────────
        if (stock < 0 || precio.compareTo(BigDecimal.ZERO) < 0) {
            // compareTo(ZERO) < 0 significa que precio es menor que 0
            cargarSelectores(request);
            request.setAttribute("error", "Stock y precio no pueden ser negativos.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        try {
            // ── Paso 7: insertar el producto y obtener el ID generado ─────
            // El DAO inserta en la tabla productos y retorna el ID AUTO_INCREMENT
            int nuevoId = new CrearProductoDAO().crear(nombre, descripcion, stock,
                                                        precio, estado, fechaVenc,
                                                        idCategoria, idUnidad);

            // ── Paso 8: guardar la imagen si se subió un archivo ──────────
            // getPart() requiere que @MultipartConfig esté activo (o <multipart-config> en web.xml)
            Part imagenPart = request.getPart("imagen");
            if (imagenPart != null && imagenPart.getSize() > 0) {
                // Se subió un archivo → guardar en disco y registrar en BD
                guardarImagen(request, imagenPart, nuevoId, nombre);
            }
            // Si no se subió imagen, el producto queda sin imagen (path_imagen = null en BD)

        } catch (SQLException e) {
            e.printStackTrace();
            cargarSelectores(request);
            request.setAttribute("error", "Error al guardar el producto. Intenta de nuevo.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // ── Paso 9: redirigir con mensaje de éxito ────────────────────────
        response.sendRedirect(request.getContextPath() + "/productos?exito=creado");
    }

    // =========================================================
    // MÉTODOS AUXILIARES (HELPERS)
    // =========================================================

    /**
     * Guarda el archivo de imagen en el disco y registra el path en la BD.
     *
     * FLUJO INTERNO:
     * 1. Extraer la extensión del nombre original del archivo subido.
     *    Ej: "foto.JPG" → ".jpg"
     * 2. Construir el nombre definitivo: "producto_{ID}.{ext}"
     *    Ej: "producto_7.jpg"
     *    El ID garantiza unicidad: no puede haber dos con el mismo nombre.
     * 3. Guardar en build/web/assets/images/productos/ → activo ahora en Tomcat.
     * 4. Copiar también a web/assets/images/productos/ → persiste tras Clean&Build.
     * 5. Registrar en la tabla imagenes_producto: path y alt (nombre del producto).
     *
     * @param request         para acceder al ServletContext (ruta real del servidor)
     * @param part            el archivo subido vía multipart/form-data
     * @param idProducto      ID del producto (usado en el nombre del archivo)
     * @param nombreProducto  nombre del producto (usado como texto alt de la imagen)
     */
    private void guardarImagen(HttpServletRequest request, Part part,
                                int idProducto, String nombreProducto)
            throws IOException, SQLException {

        // Extraer la extensión del archivo original subido por el usuario
        String ext           = obtenerExtension(part.getSubmittedFileName());
        // Nombre del archivo: único por producto, fácil de rastrear
        String nombreArchivo = "producto_" + idProducto + ext;

        // 1. Guardar en build/web/ → Tomcat puede servirlo inmediatamente
        File carpetaDeploy = Uploads.carpetaProductos(request.getServletContext());
        part.write(new File(carpetaDeploy, nombreArchivo).getAbsolutePath());

        // 2. Copiar también al fuente web/ → sobrevive el Clean & Build de NetBeans
        File carpetaFuente = Uploads.carpetaProductosFuente(request.getServletContext());
        if (carpetaFuente != null) {
            try {
                java.nio.file.Files.copy(
                    new File(carpetaDeploy, nombreArchivo).toPath(),
                    new File(carpetaFuente, nombreArchivo).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING // Sobreescribir si ya existe
                );
            } catch (Exception ignored) {
                // Si falla la copia al fuente, no es crítico — la imagen sí existe en deploy
            }
        }

        // 3. Registrar en BD: path relativo y texto alternativo
        // Uploads.urlRelativa() genera: "assets/images/productos/producto_7.jpg"
        new ImagenProductoDAO().guardarOActualizar(idProducto,
                Uploads.urlRelativa(nombreArchivo), nombreProducto);
    }

    /**
     * Extrae la extensión de un nombre de archivo, siempre en minúsculas.
     * Ejemplos: "foto.JPG" → ".jpg"  |  "imagen.PNG" → ".png"
     * Si no tiene punto (sin extensión) → retorna ".jpg" por defecto.
     */
    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        // Desde el último punto hasta el final, en minúsculas
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /**
     * Carga las listas de categorías y unidades de medida para los <select> del formulario.
     * Se llama tanto en el GET inicial como al reenviar el formulario con error,
     * porque los atributos del request se pierden entre peticiones.
     */
    private void cargarSelectores(HttpServletRequest request) {
        try {
            ProductoDAO dao = new ProductoDAO();
            request.setAttribute("categorias", dao.listarCategorias()); // Para el select de categoría
            request.setAttribute("unidades",   dao.listarUnidades());   // Para el select de unidad
        } catch (SQLException e) {
            // Si falla, los selectores quedarán vacíos — el JSP no explota
            request.setAttribute("categorias", List.of());
            request.setAttribute("unidades",   List.of());
        }
    }

    /**
     * Verifica que el usuario en sesión es Admin o SuperAdmin.
     * Los Empleados son redirigidos a /productos.
     * Retorna false si ya se hizo la redirección (el servlet debe retornar).
     */
    private boolean verificarAcceso(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Usuario u = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (u == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String rol = u.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            // Empleado → solo puede ver productos, no crear
            response.sendRedirect(request.getContextPath() + "/productos");
            return false;
        }
        return true;
    }

    /**
     * Retorna true si el string es null o está compuesto solo de espacios.
     */
    private boolean estaVacio(String v) { return v == null || v.isBlank(); }
}
