package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.ProductoDAO;
import com.dulce_gestion.models.Producto;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/*
 * Servlet para GET /productos.
 * Todos los roles autenticados pueden acceder (empleado solo lectura).
 * Carga la lista de productos y pasa el rol al JSP para mostrar
 * u ocultar los botones de acción.
 */
public class ProductosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuario") : null;

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        List<Producto> productos;
        try {
            productos = new ProductoDAO().listarTodos();
        } catch (SQLException e) {
            e.printStackTrace();
            productos = List.of();
            request.setAttribute("errorProductos", "Error al cargar los productos.");
        }

        request.setAttribute("productos", productos);
        request.setAttribute("rolSolicitante", usuario.getNombreRol());
        request.getRequestDispatcher("/WEB-INF/jsp/productos/lista.jsp")
               .forward(request, response);
    }
}
