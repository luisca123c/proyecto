package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.CrearProductoDAO;
import com.dulce_gestion.dao.ImagenProductoDAO;
import com.dulce_gestion.dao.ProductoDAO;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/*
 * Servlet para /productos/nuevo.
 * Solo accesible para SuperAdministrador y Administrador.
 * Usa @MultipartConfig para recibir el archivo de imagen.
 */
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // 5 MB max
public class NuevoProductoServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/productos/nuevo.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!verificarAcceso(request, response)) return;
        cargarSelectores(request);
        request.getRequestDispatcher(VISTA).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!verificarAcceso(request, response)) return;
        request.setCharacterEncoding("UTF-8");

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
            cargarSelectores(request);
            request.setAttribute("error", "Todos los campos obligatorios deben completarse.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        int stock; BigDecimal precio; int idCategoria; int idUnidad;
        try {
            stock       = Integer.parseInt(stockStr.trim());
            precio      = new BigDecimal(precioStr.trim());
            idCategoria = Integer.parseInt(categoriaStr.trim());
            idUnidad    = Integer.parseInt(unidadStr.trim());
        } catch (NumberFormatException e) {
            cargarSelectores(request);
            request.setAttribute("error", "Stock y precio deben ser numeros validos.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        if (stock < 0 || precio.compareTo(BigDecimal.ZERO) < 0) {
            cargarSelectores(request);
            request.setAttribute("error", "Stock y precio no pueden ser negativos.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        try {
            // Crear el producto y obtener su nuevo ID
            int nuevoId = new CrearProductoDAO().crear(nombre, descripcion, stock,
                                                        precio, estado, fechaVenc,
                                                        idCategoria, idUnidad);

            // Procesar imagen si fue enviada
            Part imagenPart = request.getPart("imagen");
            if (imagenPart != null && imagenPart.getSize() > 0) {
                guardarImagen(request, imagenPart, nuevoId, nombre);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            cargarSelectores(request);
            request.setAttribute("error", "Error al guardar el producto. Intenta de nuevo.");
            request.getRequestDispatcher(VISTA).forward(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/productos?exito=creado");
    }

    /*
     * Guarda el archivo en disco y registra el path en la BD.
     * Los archivos van a: [webroot]/assets/images/productos/
     */
    private void guardarImagen(HttpServletRequest request, Part part,
                                int idProducto, String nombreProducto)
            throws IOException, SQLException {

        String webRoot   = request.getServletContext().getRealPath("/");
        File   carpeta   = new File(webRoot, "assets/images/productos");
        if (!carpeta.exists()) carpeta.mkdirs();

        // Generar nombre de archivo unico basado en el ID del producto
        String ext       = obtenerExtension(part.getSubmittedFileName());
        String nombreArchivo = "producto_" + idProducto + ext;
        File   destino   = new File(carpeta, nombreArchivo);

        part.write(destino.getAbsolutePath());

        String pathRelativo = "assets/images/productos/" + nombreArchivo;
        new ImagenProductoDAO().guardarOActualizar(idProducto, pathRelativo, nombreProducto);
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
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
