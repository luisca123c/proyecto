package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.ConfiguracionDAO;
import com.dulce_gestion.models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /configuracion          → lista las 3 tablas con tabs
 * GET  /configuracion?tab=X    → abre un tab específico
 * POST /configuracion          → crear / editar / eliminar según ?accion=
 * Solo SuperAdministrador puede acceder.
 */
@WebServlet("/configuracion")
public class ConfiguracionServlet extends HttpServlet {

    private static final String VISTA = "/WEB-INF/jsp/configuracion/configuracion.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!soloSuperAdmin(req, res)) return;
        cargar(req);
        req.getRequestDispatcher(VISTA).forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!soloSuperAdmin(req, res)) return;
        req.setCharacterEncoding("UTF-8");

        String accion = req.getParameter("accion");
        String tab    = req.getParameter("tab");
        if (tab == null || tab.isBlank()) tab = "categorias";

        ConfiguracionDAO dao = new ConfiguracionDAO();

        try {
            switch (accion == null ? "" : accion) {

                // ── CATEGORÍAS ────────────────────────────────────────
                case "crearCategoria": {
                    String nombre = requerir(req, "nombre");
                    String desc   = req.getParameter("descripcion");
                    dao.crearCategoria(nombre, desc);
                    res.sendRedirect(ctx(req) + "/configuracion?tab=categorias&exito=creado");
                    return;
                }
                case "editarCategoria": {
                    int id = Integer.parseInt(req.getParameter("id"));
                    boolean activo = "1".equals(req.getParameter("activo"));
                    dao.editarCategoria(id, requerir(req, "nombre"), req.getParameter("descripcion"), activo);
                    res.sendRedirect(ctx(req) + "/configuracion?tab=categorias&exito=editado");
                    return;
                }
                case "eliminarCategoria": {
                    dao.eliminarCategoria(Integer.parseInt(req.getParameter("id")));
                    res.sendRedirect(ctx(req) + "/configuracion?tab=categorias&exito=eliminado");
                    return;
                }

                // ── UNIDADES DE MEDIDA ────────────────────────────────
                case "crearUnidad": {
                    dao.crearUnidad(requerir(req, "nombre"));
                    res.sendRedirect(ctx(req) + "/configuracion?tab=unidades&exito=creado");
                    return;
                }
                case "editarUnidad": {
                    boolean activo = "1".equals(req.getParameter("activo"));
                    dao.editarUnidad(Integer.parseInt(req.getParameter("id")), requerir(req, "nombre"), activo);
                    res.sendRedirect(ctx(req) + "/configuracion?tab=unidades&exito=editado");
                    return;
                }
                case "eliminarUnidad": {
                    dao.eliminarUnidad(Integer.parseInt(req.getParameter("id")));
                    res.sendRedirect(ctx(req) + "/configuracion?tab=unidades&exito=eliminado");
                    return;
                }

                // ── MÉTODOS DE PAGO ───────────────────────────────────
                case "crearMetodo": {
                    dao.crearMetodo(requerir(req, "nombre"));
                    res.sendRedirect(ctx(req) + "/configuracion?tab=metodos&exito=creado");
                    return;
                }
                case "editarMetodo": {
                    boolean activo = "1".equals(req.getParameter("activo"));
                    dao.editarMetodo(Integer.parseInt(req.getParameter("id")), requerir(req, "nombre"), activo);
                    res.sendRedirect(ctx(req) + "/configuracion?tab=metodos&exito=editado");
                    return;
                }
                case "eliminarMetodo": {
                    dao.eliminarMetodo(Integer.parseInt(req.getParameter("id")));
                    res.sendRedirect(ctx(req) + "/configuracion?tab=metodos&exito=eliminado");
                    return;
                }

                default:
                    req.setAttribute("error", "Acción desconocida.");
            }
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("Duplicate")) {
                req.setAttribute("error", "Ya existe un registro con ese nombre.");
            } else if (msg != null && msg.contains("foreign key")) {
                req.setAttribute("error", "No se puede eliminar: está siendo usado en el sistema.");
            } else {
                req.setAttribute("error", "Error de base de datos: " + msg);
            }
        }

        req.setAttribute("tab", tab);
        cargar(req);
        req.getRequestDispatcher(VISTA).forward(req, res);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void cargar(HttpServletRequest req) {
        ConfiguracionDAO dao = new ConfiguracionDAO();
        String tab = req.getParameter("tab");
        req.setAttribute("tab", tab != null ? tab : "categorias");
        try { req.setAttribute("categorias", dao.listarCategorias()); } catch (SQLException e) { req.setAttribute("categorias", java.util.List.of()); }
        try { req.setAttribute("unidades",   dao.listarUnidades());   } catch (SQLException e) { req.setAttribute("unidades",   java.util.List.of()); }
        try { req.setAttribute("metodos",    dao.listarMetodos());    } catch (SQLException e) { req.setAttribute("metodos",    java.util.List.of()); }
    }

    private String requerir(HttpServletRequest req, String campo) {
        String v = req.getParameter(campo);
        if (v == null || v.isBlank())
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        String trimmed = v.trim();
        if (campo.equals("nombre")) {
            if (trimmed.matches(".*\\d.*"))
                throw new IllegalArgumentException("El nombre no puede contener números.");
            if (trimmed.length() < 2)
                throw new IllegalArgumentException("El nombre debe tener al menos 2 caracteres.");
            if (trimmed.length() > 25)
                throw new IllegalArgumentException("El nombre no puede superar los 25 caracteres.");
        }
        return trimmed;
    }

    private String ctx(HttpServletRequest req) { return req.getContextPath(); }

    private boolean soloSuperAdmin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession s = req.getSession(false);
        Usuario u = s != null ? (Usuario) s.getAttribute("usuario") : null;
        if (u == null) { res.sendRedirect(req.getContextPath() + "/login"); return false; }
        if (!"SuperAdministrador".equals(u.getNombreRol())) {
            res.sendRedirect(req.getContextPath() + "/dashboard");
            return false;
        }
        return true;
    }
}
