package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.PerfilDAO;
import com.dulce_gestion.models.Usuario;
import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/perfil/editar")
public class EditarPerfilServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Obtener usuario de sesión
        HttpSession sesion = request.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
        String tipoEdicion = request.getParameter("tipo"); // "datos" o "contrasena"

        if ("datos".equals(tipoEdicion)) {
            editarDatos(request, response, usuarioSesion);
        } else if ("contrasena".equals(tipoEdicion)) {
            cambiarContrasena(request, response, usuarioSesion);
        } else {
            response.sendRedirect(request.getContextPath() + "/perfil");
        }
    }

    /*
     * Edita datos personales: nombre, teléfono, género, correo
     * Los Empleados SOLO pueden editar: teléfono, correo
     */
    private void editarDatos(HttpServletRequest request, HttpServletResponse response, 
                              Usuario usuarioSesion) throws ServletException, IOException {

        String nombreCompleto = request.getParameter("nombreCompleto");
        String telefono = request.getParameter("telefono");
        String idGeneroStr = request.getParameter("idGenero");
        String correo = request.getParameter("correo");

        // Validaciones
        if (nombreCompleto == null || nombreCompleto.isBlank() ||
            telefono == null || telefono.isBlank() ||
            correo == null || correo.isBlank() ||
            idGeneroStr == null || idGeneroStr.isBlank()) {

            request.setAttribute("error", "Todos los campos son obligatorios");
            try {
                PerfilDAO dao = new PerfilDAO();
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        // Solo Empleados tienen restricción
        if ("Empleado".equals(usuarioSesion.getNombreRol())) {
            // Los empleados NO pueden cambiar nombre ni género, solo teléfono y correo
            request.setAttribute("error", 
                "Los empleados solo pueden cambiar teléfono y correo");
            try {
                PerfilDAO dao = new PerfilDAO();
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            int idGenero = Integer.parseInt(idGeneroStr);
            PerfilDAO dao = new PerfilDAO();
            
            boolean actualizado = dao.actualizarPerfil(
                usuarioSesion.getId(),
                nombreCompleto.trim(),
                telefono.trim(),
                idGenero,
                correo.toLowerCase().trim()
            );

            if (actualizado) {
                // Actualizar sesión con nuevos datos
                Usuario perfilActualizado = dao.obtenerPerfil(usuarioSesion.getId());
                request.getSession().setAttribute("usuario", perfilActualizado);

                response.sendRedirect(request.getContextPath() + 
                    "/perfil?exito=actualizado");
            } else {
                request.setAttribute("error", "No se pudo actualizar el perfil");
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            }

        } catch (NumberFormatException | SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al actualizar perfil");
            try {
                PerfilDAO dao = new PerfilDAO();
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
     * Cambiar contraseña
     * Disponible para todos los roles
     */
    private void cambiarContrasena(HttpServletRequest request, HttpServletResponse response,
                                    Usuario usuarioSesion) throws ServletException, IOException {

        String contrasennaActual = request.getParameter("contrasennaActual");
        String contrasennaNueva = request.getParameter("contrasennaNueva");
        String contrasennaNuevaConfirm = request.getParameter("contrasennaNuevaConfirm");

        // Validaciones
        if (contrasennaActual == null || contrasennaActual.isBlank() ||
            contrasennaNueva == null || contrasennaNueva.isBlank() ||
            contrasennaNuevaConfirm == null || contrasennaNuevaConfirm.isBlank()) {

            request.setAttribute("error", "Todos los campos son obligatorios");
            try {
                PerfilDAO dao = new PerfilDAO();
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        if (!contrasennaNueva.equals(contrasennaNuevaConfirm)) {
            request.setAttribute("error", "Las contraseñas nuevas no coinciden");
            try {
                PerfilDAO dao = new PerfilDAO();
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        if (contrasennaNueva.length() < 8) {
            request.setAttribute("error", "La contraseña debe tener al menos 8 caracteres");
            try {
                PerfilDAO dao = new PerfilDAO();
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            PerfilDAO dao = new PerfilDAO();
            boolean cambiada = dao.cambiarContrasena(
                usuarioSesion.getId(),
                contrasennaActual.trim(),
                contrasennaNueva.trim()
            );

            if (cambiada) {
                response.sendRedirect(request.getContextPath() + 
                    "/perfil?exito=contrasenna_cambiada");
            } else {
                request.setAttribute("error", "La contraseña actual es incorrecta");
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Error al cambiar contraseña");
            try {
                PerfilDAO dao = new PerfilDAO();
                Usuario perfil = dao.obtenerPerfil(usuarioSesion.getId());
                request.setAttribute("perfil", perfil);
                request.setAttribute("generos", dao.listarGeneros());
                request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                        .forward(request, response);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
