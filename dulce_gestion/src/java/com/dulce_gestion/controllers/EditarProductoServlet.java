package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EditarProductoDAO;
import com.dulce_gestion.dao.EmprendimientoDAO;
import com.dulce_gestion.dao.ImagenProductoDAO;
import com.dulce_gestion.dao.ProductoDAO;
import com.dulce_gestion.utils.Uploads;
import com.dulce_gestion.models.Emprendimiento;
import com.dulce_gestion.models.Producto;
import com.dulce_gestion.models.Usuario;
import java.util.List;

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
 * SERVLET: EditarProductoServlet
 * URL:     /productos/editar?id=X
 * MÉTODOS: GET, POST
 * ============================================================
 *
 */
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // Máximo 5 MB por archivo
@WebServlet("/productos/editar")
public class EditarProductoServlet extends HttpServlet {

    /** Ruta interna del JSP del formulario de edición */
    private static final String VISTA = "/WEB-INF/jsp/productos/editar.jsp";

    // =========================================================
    // GET /productos/editar?id=X
    // =========================================================

    /**
     * Carga el producto por ID y muestra el formulario prellenado.
     *
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;

        jakarta.servlet.http.HttpSession _sess = request.getSession(false);
        com.dulce_gestion.models.Usuario _u = (_sess != null) ? (com.dulce_gestion.models.Usuario) _sess.getAttribute("usuario") : null;
        boolean _esSuper = _u != null && "SuperAdministrador".equals(_u.getNombreRol());

        String idParam = request.getParameter("id"); // ID del producto desde la URL
        if (estaVacio(idParam)) {
            response.sendRedirect(request.getContextPath() + "/productos");
            return;
        }

        try {
            // Buscar el producto completo (con imagen, categoría y unidad)
            Producto prod = new ProductoDAO().buscarPorId(Integer.parseInt(idParam));
            if (prod == null) {
                // El producto no existe (¿fue eliminado?) → volver a la lista
                response.sendRedirect(request.getContextPath() + "/productos");
                return;
            }
            request.setAttribute("producto", prod);
            request.setAttribute("esSuperAdmin", _esSuper);
            cargarSelectores(request);
            if (_esSuper) {
                request.setAttribute("emprendimientos", new EmprendimientoDAO().listarActivos());
            }
            request.getRequestDispatcher(VISTA).forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/productos");
        }
    }

    // =========================================================
    // POST /productos/editar
    // =========================================================

    /**
     * Aplica los cambios al producto en la BD y gestiona la imagen.
     *
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;

        // ── Paso 2: charset para caracteres especiales ───────────────────
        request.setCharacterEncoding("UTF-8");

        // ── Paso 3: leer parámetros del formulario ───────────────────────
        String idParam      = request.getParameter("id");
        String nombre       = request.getParameter("nombre");
        String descripcion  = request.getParameter("descripcion");
        String stockStr     = request.getParameter("stock");
        String precioStr    = request.getParameter("precio");
        String estado       = request.getParameter("estado");
        String fechaVenc    = request.getParameter("fechaVencimiento");
        String categoriaStr = request.getParameter("idCategoria");
        String unidadStr    = request.getParameter("idUnidad");
        String empParamProd = request.getParameter("idEmprendimiento");
        jakarta.servlet.http.HttpSession _sessP = request.getSession(false);
        com.dulce_gestion.models.Usuario _uP = (_sessP != null) ? (com.dulce_gestion.models.Usuario) _sessP.getAttribute("usuario") : null;
        boolean _esSuperP = _uP != null && "SuperAdministrador".equals(_uP.getNombreRol());

        // ── Paso 4: validar campos obligatorios ──────────────────────────
        if (estaVacio(nombre) || estaVacio(stockStr) || estaVacio(precioStr)
                || estaVacio(estado) || estaVacio(fechaVenc)
                || estaVacio(categoriaStr) || estaVacio(unidadStr)) {
            reenviarConError(request, response, "Todos los campos obligatorios deben completarse.", idParam);
            return;
        }

        // ── Paso 5: parsear tipos numéricos ──────────────────────────────
        int id; int stock; BigDecimal precio; int idCategoria; int idUnidad;
        try {
            id          = Integer.parseInt(idParam.trim());
            stock       = Integer.parseInt(stockStr.trim());
            precio      = new BigDecimal(precioStr.trim()); // BigDecimal para precisión monetaria
            idCategoria = Integer.parseInt(categoriaStr.trim());
            idUnidad    = Integer.parseInt(unidadStr.trim());
        } catch (NumberFormatException e) {
            reenviarConError(request, response, "Stock y precio deben ser numeros validos.", idParam);
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
            request.setAttribute("error", "El formato de la fecha de vencimiento no es válido.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        } catch (IllegalArgumentException ex) {
            cargarSelectores(request);
            request.setAttribute("error", ex.getMessage());
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        if (stock < 0 || precio.compareTo(BigDecimal.ZERO) < 0) {
            reenviarConError(request, response, "Stock y precio no pueden ser negativos.", idParam);
            return;
        }

        try {
            // ── Paso 7: actualizar los datos del producto en la BD ────────
            int idEmpProd = 0;
            if (_esSuperP && empParamProd != null && !empParamProd.isBlank()) {
                try { idEmpProd = Integer.parseInt(empParamProd); } catch (NumberFormatException ignored) {}
            }
            new EditarProductoDAO().actualizar(id, nombre, descripcion, stock,
                                               precio, estado, fechaVenc, idCategoria, idUnidad,
                                               _esSuperP, idEmpProd);

            // ── Paso 8: gestionar la imagen ───────────────────────────────

            String eliminarImg = request.getParameter("eliminarImagen"); // Checkbox del form

            if ("1".equals(eliminarImg)) {
                // ── Escenario A: el usuario quiere eliminar la imagen actual ─
                eliminarImagenFisica(request, id); // Borrar archivo del disco
                new ImagenProductoDAO().eliminar(id); // Borrar registro en BD

            } else {
                // ── Escenario B o C: verificar si se subió una nueva imagen ─
                Part imagenPart = request.getPart("imagen");
                boolean hayNuevaImagen = imagenPart != null
                        && imagenPart.getSize() > 0
                        && imagenPart.getSubmittedFileName() != null
                        && !imagenPart.getSubmittedFileName().isBlank();

                if (hayNuevaImagen) {
                    // ── Escenario B: subir nueva imagen ──────────────────
                    guardarImagen(request, imagenPart, id, nombre);
                }
                // ── Escenario C: sin cambios en la imagen → no hacer nada ─
            }

        } catch (SQLException e) {
            e.printStackTrace();
            reenviarConError(request, response, "Error al guardar los cambios. Intenta de nuevo.", idParam);
            return;
        }

        // ── Paso 9: redirigir con mensaje de éxito ────────────────────────
        response.sendRedirect(request.getContextPath() + "/productos?exito=editado");
    }

    // =========================================================
    // MÉTODOS AUXILIARES (HELPERS)
    // =========================================================

    /**
     * Guarda un archivo de imagen en disco (build/web/ y web/) y actualiza el registro en BD.
     *
     * Primero borra la imagen anterior (si la hay) para evitar archivos huérfanos.
     * Luego guarda el nuevo archivo y actualiza imagenes_producto.
     */
    private void guardarImagen(HttpServletRequest request, Part part,
                                int idProducto, String nombreProducto)
            throws IOException, SQLException {

        // Borrar imagen anterior antes de guardar la nueva (evita archivos con diferente extensión)
        eliminarImagenFisica(request, idProducto);

        String ext           = obtenerExtension(part.getSubmittedFileName());
        String nombreArchivo = "producto_" + idProducto + ext;

        // Guardar en build/web/ → activo en Tomcat inmediatamente
        File carpetaDeploy = Uploads.carpetaProductos(request.getServletContext());
        File archivoDeploy = new File(carpetaDeploy, nombreArchivo);
        part.write(archivoDeploy.getAbsolutePath());

        // Copiar al fuente web/ → persiste tras Clean & Build de NetBeans
        File carpetaFuente = Uploads.carpetaProductosFuente(request.getServletContext());
        if (carpetaFuente != null) {
            try {
                java.nio.file.Files.copy(
                    archivoDeploy.toPath(),
                    new File(carpetaFuente, nombreArchivo).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            } catch (Exception ignored) {
                // No es crítico si falla la copia al fuente
            }
        }

        // Actualizar el registro en imagenes_producto (UPSERT: UPDATE si existe, INSERT si no)
        new ImagenProductoDAO().guardarOActualizar(idProducto,
                Uploads.urlRelativa(nombreArchivo), nombreProducto);
    }

    /**
     * Borra el archivo físico de imagen de ambas carpetas (build/web/ y web/).
     * Consulta primero el path actual en la BD para saber el nombre del archivo.
     * Si no hay imagen registrada, no hace nada.
     */
    private void eliminarImagenFisica(HttpServletRequest request, int idProducto) {
        try {
            // Obtener el path registrado en BD para este producto
            String urlRelativa = new ImagenProductoDAO().obtenerPath(idProducto);
            if (urlRelativa == null || urlRelativa.isBlank()) return; // Sin imagen → nada que borrar

            // Extraer solo el nombre del archivo del path (ej: "producto_5.jpg")
            String nombreArchivo = new File(urlRelativa).getName();

            // Borrar de build/web/assets/images/productos/
            File archivoDeploy = new File(Uploads.carpetaProductos(request.getServletContext()), nombreArchivo);
            if (archivoDeploy.exists()) archivoDeploy.delete();

            // Borrar de web/assets/images/productos/ (el fuente del proyecto)
            File carpetaFuente = Uploads.carpetaProductosFuente(request.getServletContext());
            if (carpetaFuente != null) {
                File archivoFuente = new File(carpetaFuente, nombreArchivo);
                if (archivoFuente.exists()) archivoFuente.delete();
            }
        } catch (Exception ignored) {
            // Si falla la eliminación del disco, el proceso continúa
            // (la imagen huérfana no es crítica)
        }
    }

    /**
     * Extrae la extensión del nombre de archivo en minúsculas.
     * "foto.JPG" → ".jpg" | si no tiene extensión → ".jpg" por defecto
     */
    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    /**
     * Recarga el producto desde la BD y reenvía al formulario con el mensaje de error.
     * Asegura que el formulario muestre los datos actuales del producto aunque haya un error.
     */
    private void reenviarConError(HttpServletRequest request, HttpServletResponse response,
                                   String error, String idParam)
            throws ServletException, IOException {
        try {
            // Recargar el producto para prellenar el formulario
            Producto prod = new ProductoDAO().buscarPorId(Integer.parseInt(idParam));
            request.setAttribute("producto", prod);
        } catch (Exception ignored) {}

        cargarSelectores(request);
        jakarta.servlet.http.HttpSession _sessR = request.getSession(false);
        com.dulce_gestion.models.Usuario _uR = (_sessR != null) ? (com.dulce_gestion.models.Usuario) _sessR.getAttribute("usuario") : null;
        boolean _esSuperR = _uR != null && "SuperAdministrador".equals(_uR.getNombreRol());
        request.setAttribute("esSuperAdmin", _esSuperR);
        if (_esSuperR) {
            try { request.setAttribute("emprendimientos", new EmprendimientoDAO().listarActivos()); } catch (Exception ignored) {}
        }
        request.setAttribute("error", error);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    /**
     * Carga las listas de categorías y unidades para los <select> del formulario.
     */
    private void cargarSelectores(HttpServletRequest request) {
        try {
            ProductoDAO dao = new ProductoDAO();
            request.setAttribute("categorias", dao.listarCategorias());
            request.setAttribute("unidades",   dao.listarUnidades());
        } catch (SQLException e) {
            request.setAttribute("categorias", List.of());
            request.setAttribute("unidades",   List.of());
        }
    }

    /**
     * Verifica que el usuario en sesión es Admin o SuperAdmin.
     * Redirige y retorna false si no tiene permiso.
     */
    private boolean verificarAcceso(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Usuario u = (session != null) ? (Usuario) session.getAttribute("usuario") : null;
        if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return false; }
        String rol = u.getNombreRol();
        if (!"SuperAdministrador".equals(rol) && !"Administrador".equals(rol)) {
            response.sendRedirect(request.getContextPath() + "/productos");
            return false;
        }
        return true;
    }

    /** Retorna true si el string es null o solo espacios. */
    private boolean estaVacio(String v) { return v == null || v.isBlank(); }
}
