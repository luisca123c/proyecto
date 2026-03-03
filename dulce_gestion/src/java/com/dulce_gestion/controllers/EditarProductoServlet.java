package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.EditarProductoDAO;
import com.dulce_gestion.dao.ImagenProductoDAO;
import com.dulce_gestion.dao.ProductoDAO;
import com.dulce_gestion.models.Producto;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
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

/*
 * Servlet para /productos/editar?id=X
 * Solo accesible para SuperAdministrador y Administrador.
 * Soporta subida de imagen via multipart.
 */
@MultipartConfig(maxFileSize = 5 * 1024 * 1024)
public class EditarProductoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/productos/editar.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;

        String idParam = request.getParameter("id");
        if (estaVacio(idParam)) { response.sendRedirect(request.getContextPath() + "/productos"); return; }

        try {
            Producto prod = new ProductoDAO().buscarPorId(Integer.parseInt(idParam));
            if (prod == null) { response.sendRedirect(request.getContextPath() + "/productos"); return; }
            request.setAttribute("producto", prod);
            cargarSelectores(request);
            request.getRequestDispatcher(VISTA).forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/productos");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;
        request.setCharacterEncoding("UTF-8");

        String idParam      = request.getParameter("id");
        String nombre       = request.getParameter("nombre");
        String descripcion  = request.getParameter("descripcion");
        String stockStr     = request.getParameter("stock");
        String precioStr    = request.getParameter("precio");
        String estado       = request.getParameter("estado");
        String fechaVenc    = request.getParameter("fechaVencimiento");
        String categoriaStr = request.getParameter("idCategoria");
        String unidadStr    = request.getParameter("idUnidad");

        if (estaVacio(nombre) || estaVacio(stockStr) || estaVacio(precioStr)
                || estaVacio(estado) || estaVacio(fechaVenc)
                || estaVacio(categoriaStr) || estaVacio(unidadStr)) {
            reenviarConError(request, response, "Todos los campos obligatorios deben completarse.", idParam);
            return;
        }

        int id; int stock; BigDecimal precio; int idCategoria; int idUnidad;
        try {
            id          = Integer.parseInt(idParam.trim());
            stock       = Integer.parseInt(stockStr.trim());
            precio      = new BigDecimal(precioStr.trim());
            idCategoria = Integer.parseInt(categoriaStr.trim());
            idUnidad    = Integer.parseInt(unidadStr.trim());
        } catch (NumberFormatException e) {
            reenviarConError(request, response, "Stock y precio deben ser numeros validos.", idParam);
            return;
        }

        if (stock < 0 || precio.compareTo(BigDecimal.ZERO) < 0) {
            reenviarConError(request, response, "Stock y precio no pueden ser negativos.", idParam);
            return;
        }

        try {
            new EditarProductoDAO().actualizar(id, nombre, descripcion, stock,
                                               precio, estado, fechaVenc, idCategoria, idUnidad);

            // Procesar imagen si fue enviada
            Part imagenPart = request.getPart("imagen");
            if (imagenPart != null && imagenPart.getSize() > 0) {
                guardarImagen(request, imagenPart, id, nombre);
            }

            // Eliminar imagen si se marco el checkbox
            String eliminarImg = request.getParameter("eliminarImagen");
            if ("1".equals(eliminarImg)) {
                eliminarImagenFisica(request, id);
                new ImagenProductoDAO().eliminar(id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            reenviarConError(request, response, "Error al guardar los cambios. Intenta de nuevo.", idParam);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/productos?exito=editado");
    }

    private void guardarImagen(HttpServletRequest request, Part part,
                                int idProducto, String nombreProducto)
            throws IOException, SQLException {

        // Borrar imagen anterior si existe
        eliminarImagenFisica(request, idProducto);

        String webRoot = request.getServletContext().getRealPath("/");
        File carpeta   = new File(webRoot, "assets/images/productos");
        if (!carpeta.exists()) carpeta.mkdirs();

        String ext           = obtenerExtension(part.getSubmittedFileName());
        String nombreArchivo = "producto_" + idProducto + ext;
        File   destino       = new File(carpeta, nombreArchivo);
        part.write(destino.getAbsolutePath());

        String pathRelativo = "assets/images/productos/" + nombreArchivo;
        new ImagenProductoDAO().guardarOActualizar(idProducto, pathRelativo, nombreProducto);
    }

    /*
     * Borra el archivo fisico de la imagen anterior en disco.
     */
    private void eliminarImagenFisica(HttpServletRequest request, int idProducto) {
        try {
            ImagenProductoDAO dao = new ImagenProductoDAO();
            String path = dao.obtenerPath(idProducto);
            if (path != null && !path.isBlank()) {
                String webRoot = request.getServletContext().getRealPath("/");
                File f = new File(webRoot, path);
                if (f.exists()) f.delete();
            }
        } catch (Exception ignored) {}
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private void reenviarConError(HttpServletRequest request, HttpServletResponse response,
                                   String error, String idParam)
            throws ServletException, IOException {
        try {
            Producto prod = new ProductoDAO().buscarPorId(Integer.parseInt(idParam));
            request.setAttribute("producto", prod);
        } catch (Exception ignored) {}
        cargarSelectores(request);
        request.setAttribute("error", error);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    private void cargarSelectores(HttpServletRequest request) {
        try {
            ProductoDAO dao = new ProductoDAO();
            request.setAttribute("categorias", dao.listarCategorias());
            request.setAttribute("unidades",   dao.listarUnidades());
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("categorias", List.of());
            request.setAttribute("unidades",   List.of());
        }
    }

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

    private boolean estaVacio(String v) { return v == null || v.isBlank(); }
}
