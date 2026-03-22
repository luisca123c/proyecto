package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.CrearProductoDAO;
import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.utils.EmpresaUtil;
import com.dulce_gestion.dao.ImagenProductoDAO;
import com.dulce_gestion.dao.ProductoDAO;
import com.dulce_gestion.utils.Uploads;
import com.dulce_gestion.models.Emprendimiento;
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
 */
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // Máximo 5 MB por archivo
@WebServlet("/productos/nuevo")
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

        if (!verificarAcceso(request, response)) return;

        cargarSelectores(request);
        cargarEmprendimientos(request);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    // =========================================================
    // POST /productos/nuevo
    // =========================================================

    /**
     * Valida los datos del formulario, crea el producto y guarda la imagen.
     *
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;

        HttpSession session  = request.getSession(false);
        Usuario solicitante  = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

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
        String empParam     = request.getParameter("idEmprendimiento");

        // Determinar idEmprendimiento: SuperAdmin elige, Admin usa el suyo
        int idEmprendimiento;
        if (solicitante != null && "SuperAdministrador".equals(solicitante.getNombreRol())) {
            if (empParam == null || empParam.isBlank()) {
                cargarSelectores(request);
                cargarEmprendimientos(request);
                request.setAttribute("error", "Debes seleccionar un emprendimiento.");
                request.getRequestDispatcher(VISTA).forward(request, response);
                return;
            }
            try { idEmprendimiento = Integer.parseInt(empParam); }
            catch (NumberFormatException e) { idEmprendimiento = 0; }
        } else {
            idEmprendimiento = solicitante != null ? EmpresaUtil.resolverEmprendimiento(solicitante, request) : 0;
        }

        // ── Paso 4: validar campos obligatorios ──────────────────────────
        if (estaVacio(nombre) || estaVacio(stockStr) || estaVacio(precioStr)
                || estaVacio(estado) || estaVacio(fechaVenc)
                || estaVacio(categoriaStr) || estaVacio(unidadStr)) {
            cargarSelectores(request);
            cargarEmprendimientos(request);
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
            cargarEmprendimientos(request);
            request.setAttribute("error", "Stock y precio deben ser numeros validos.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        // ── Paso 6: validar que no sean negativos ─────────────────────────
        // Validar que fechaVencimiento no sea en el pasado
        try {
            java.time.LocalDate fv = java.time.LocalDate.parse(fechaVenc);
            if (fv.isBefore(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a hoy.");
            }
        } catch (java.time.format.DateTimeParseException ex) {
            cargarSelectores(request);
            cargarEmprendimientos(request);
            request.setAttribute("error", "El formato de la fecha de vencimiento no es válido.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        } catch (IllegalArgumentException ex) {
            cargarSelectores(request);
            cargarEmprendimientos(request);
            request.setAttribute("error", ex.getMessage());
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        if (stock < 0 || precio.compareTo(BigDecimal.ZERO) < 0) {
            // compareTo(ZERO) < 0 significa que precio es menor que 0
            cargarSelectores(request);
            cargarEmprendimientos(request);
            request.setAttribute("error", "Stock y precio no pueden ser negativos.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        try {
            // ── Paso 7: insertar el producto y obtener el ID generado ─────
            // El DAO inserta en la tabla productos y retorna el ID AUTO_INCREMENT
            int nuevoId = new CrearProductoDAO().crear(nombre, descripcion, stock,
                                                        precio, estado, fechaVenc,
                                                        idCategoria, idUnidad, idEmprendimiento);

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
            cargarEmprendimientos(request);
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

    private void cargarEmprendimientos(HttpServletRequest req) {
        try {
            req.setAttribute("emprendimientos", new EmprendimientoDAO().listarActivos());
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            req.setAttribute("emprendimientos", java.util.List.of());
        }
    }
}
