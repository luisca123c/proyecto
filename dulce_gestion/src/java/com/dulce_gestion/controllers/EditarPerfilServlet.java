package com.dulce_gestion.controllers;

import com.dulce_gestion.dao.PerfilDAO;
import com.dulce_gestion.models.Usuario;

import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

/**
 * ============================================================
 * SERVLET: EditarPerfilServlet
 * URL:     POST /perfil/editar
 * MÉTODOS: POST (solo POST — el formulario viene desde /perfil)
 * ============================================================
 *
 * ¿QUÉ HACE?
 * ----------
 * Procesa los cambios del perfil propio del usuario autenticado.
 * Hay DOS tipos de edición, distinguidos por el campo "tipo":
 *
 *   tipo = "datos"      → actualizar nombre, teléfono, género, correo
 *   tipo = "contrasena" → cambiar la contraseña (requiere la actual)
 *
 * ¿POR QUÉ DOS TIPOS EN UN SOLO SERVLET?
 * ----------------------------------------
 * La pantalla de perfil (/perfil) tiene DOS formularios separados:
 *   Formulario 1 → datos personales
 *   Formulario 2 → cambiar contraseña
 *
 * Ambos hacen POST a /perfil/editar pero con un campo oculto diferente:
 *   <input type="hidden" name="tipo" value="datos">
 *   <input type="hidden" name="tipo" value="contrasena">
 *
 * doPost() lee el "tipo" y delega a editarDatos() o cambiarContrasena().
 *
 * ¿QUÉ PUEDEN EDITAR LOS EMPLEADOS?
 * -----------------------------------
 * Los Empleados tienen restricción en qué datos pueden cambiar:
 *   Pueden cambiar:     teléfono, correo
 *   NO pueden cambiar:  nombre completo, género
 *
 * Esta lógica la aplica editarDatos():
 *   if "Empleado" → rechazar el cambio de datos con mensaje de error
 *
 * Admin y SuperAdmin pueden cambiar todos sus datos personales.
 *
 * ¿POR QUÉ SE ACTUALIZA LA SESIÓN DESPUÉS DE EDITAR DATOS?
 * ----------------------------------------------------------
 * Al cambiar el nombre o el correo, el objeto "usuario" en sesión
 * quedaría desactualizado. El sidebar y otras partes de la UI
 * muestran el nombre del usuario desde la sesión.
 *
 * Solución: después de actualizar la BD exitosamente, se recarga
 * el perfil completo y se sobreescribe el objeto de sesión:
 *   session.setAttribute("usuario", perfilActualizado)
 *
 * Así, el sidebar muestra el nombre nuevo inmediatamente.
 *
 * ¿CÓMO FUNCIONA cambiarContrasena()?
 * -------------------------------------
 * No se puede cambiar la contraseña sin saber la actual.
 * El flujo es:
 *   1. Verificar que la contraseña actual ingresada sea la correcta (BD).
 *   2. Verificar que las dos contraseñas nuevas coincidan.
 *   3. Verificar longitud mínima (8 caracteres).
 *   4. Si todo OK → hashear la nueva y hacer UPDATE.
 *
 * ¿POR QUÉ TANTO CÓDIGO REPETIDO?
 * ---------------------------------
 * Cada bloque catch necesita recargar el perfil y los géneros para
 * reenviar el JSP con el error. Esto genera código similar en varios lugares.
 * En un proyecto más grande se refactorizaría en un helper, pero aquí
 * se deja explícito para que sea más fácil de seguir en la exposición.
 */
@WebServlet("/perfil/editar")
public class EditarPerfilServlet extends HttpServlet {

    /**
     * POST /perfil/editar → delega al método correspondiente según el "tipo".
     *
     * @param request  contiene el formulario y el campo "tipo"
     * @param response para redirigir en éxito o reenviar al JSP en error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Verificar sesión activa ───────────────────────────────────────
        HttpSession sesion = request.getSession(false);
        if (sesion == null || sesion.getAttribute("usuario") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");

        // Leer el tipo de edición del campo oculto del formulario
        String tipoEdicion = request.getParameter("tipo"); // "datos" o "contrasena"

        if ("datos".equals(tipoEdicion)) {
            // El usuario quiere cambiar sus datos personales
            editarDatos(request, response, usuarioSesion);

        } else if ("contrasena".equals(tipoEdicion)) {
            // El usuario quiere cambiar su contraseña
            cambiarContrasena(request, response, usuarioSesion);

        } else {
            // Tipo no reconocido → redirigir al perfil
            response.sendRedirect(request.getContextPath() + "/perfil");
        }
    }

    // =========================================================
    // EDITAR DATOS PERSONALES
    // =========================================================

    /**
     * Actualiza los datos personales del usuario: nombre, teléfono, género, correo.
     *
     * RESTRICCIÓN: los Empleados SOLO pueden cambiar teléfono y correo.
     * Si un Empleado intenta cambiar nombre o género, se rechaza.
     *
     * FLUJO:
     * 1. Leer campos del formulario.
     * 2. Validar que no estén vacíos.
     * 3. Verificar restricción de rol para Empleados.
     * 4. Llamar a PerfilDAO.actualizarPerfil().
     * 5. Si éxito → actualizar el objeto de sesión + redirect con mensaje.
     *
     * @param request       request con los campos del formulario de datos
     * @param response      para redirigir o reenviar al JSP
     * @param usuarioSesion el usuario en sesión (quien edita su propio perfil)
     */
    private void editarDatos(HttpServletRequest request, HttpServletResponse response,
                              Usuario usuarioSesion) throws ServletException, IOException {

        // Leer los campos del formulario de datos personales
        String nombreCompleto = request.getParameter("nombreCompleto");
        String telefono       = request.getParameter("telefono");
        String idGeneroStr    = request.getParameter("idGenero");
        String correo         = request.getParameter("correo");

        // Validar que todos los campos tengan contenido
        if (nombreCompleto == null || nombreCompleto.isBlank() ||
            telefono       == null || telefono.isBlank()       ||
            correo         == null || correo.isBlank()         ||
            idGeneroStr    == null || idGeneroStr.isBlank()) {

            reenviarConError(request, response, usuarioSesion, "Todos los campos son obligatorios");
            return;
        }

        // ── Restricción de rol: Empleados no pueden cambiar nombre ni género ──
        if ("Empleado".equals(usuarioSesion.getNombreRol())) {
            // El formulario no debería enviar esto para empleados, pero verificamos en el servidor
            reenviarConError(request, response, usuarioSesion,
                "Los empleados solo pueden cambiar teléfono y correo");
            return;
        }

        try {
            int idGenero = Integer.parseInt(idGeneroStr);
            PerfilDAO dao = new PerfilDAO();

            // Actualizar los 3 campos en 3 tablas (correos, telefonos, perfil_usuario)
            boolean actualizado = dao.actualizarPerfil(
                usuarioSesion.getId(),
                nombreCompleto.trim(),
                telefono.trim(),
                idGenero,
                correo.toLowerCase().trim() // Normalizar correo a minúsculas
            );

            if (actualizado) {
                // Recargar el perfil completo desde la BD con los nuevos datos
                Usuario perfilActualizado = dao.obtenerPerfil(usuarioSesion.getId());

                // Actualizar el objeto de sesión para que el sidebar muestre el nombre nuevo
                request.getSession().setAttribute("usuario", perfilActualizado);

                // Redirigir con mensaje de éxito
                response.sendRedirect(request.getContextPath() + "/perfil?exito=actualizado");

            } else {
                reenviarConError(request, response, usuarioSesion, "No se pudo actualizar el perfil");
            }

        } catch (NumberFormatException | SQLException e) {
            e.printStackTrace();
            reenviarConError(request, response, usuarioSesion, "Error al actualizar perfil");
        }
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    /**
     * Cambia la contraseña del usuario, previa verificación de la actual.
     *
     * Disponible para TODOS los roles (Admin, SuperAdmin y Empleado).
     *
     * FLUJO:
     * 1. Leer los tres campos: contraseña actual, nueva, confirmación.
     * 2. Validar que ninguno esté vacío.
     * 3. Validar que la nueva y la confirmación coincidan.
     * 4. Validar longitud mínima de 8 caracteres.
     * 5. Llamar a PerfilDAO.cambiarContrasena() que verifica la actual en BD.
     *    Retorna false si la contraseña actual era incorrecta.
     * 6. Redirigir con mensaje de éxito.
     *
     * @param request       request con los campos del formulario de contraseña
     * @param response      para redirigir o reenviar al JSP
     * @param usuarioSesion el usuario en sesión (quien cambia su propia contraseña)
     */
    private void cambiarContrasena(HttpServletRequest request, HttpServletResponse response,
                                    Usuario usuarioSesion) throws ServletException, IOException {

        // Leer los tres campos del formulario de cambio de contraseña
        String contrasennaActual       = request.getParameter("contrasennaActual");
        String contrasennaNueva        = request.getParameter("contrasennaNueva");
        String contrasennaNuevaConfirm = request.getParameter("contrasennaNuevaConfirm");

        // ── Validación 1: todos los campos obligatorios ───────────────────
        if (contrasennaActual == null || contrasennaActual.isBlank() ||
            contrasennaNueva  == null || contrasennaNueva.isBlank()  ||
            contrasennaNuevaConfirm == null || contrasennaNuevaConfirm.isBlank()) {

            reenviarConError(request, response, usuarioSesion, "Todos los campos son obligatorios");
            return;
        }

        // ── Validación 2: las dos contraseñas nuevas deben ser iguales ────
        if (!contrasennaNueva.equals(contrasennaNuevaConfirm)) {
            reenviarConError(request, response, usuarioSesion, "Las contraseñas nuevas no coinciden");
            return;
        }

        // ── Validación 3: longitud mínima 8 caracteres ────────────────────
        if (contrasennaNueva.length() < 8) {
            reenviarConError(request, response, usuarioSesion, "La contraseña debe tener al menos 8 caracteres");
            return;
        }

        try {
            PerfilDAO dao = new PerfilDAO();

            // cambiarContrasena() verifica que la contraseña actual es correcta
            // (hashea y compara con la BD) antes de hacer el UPDATE.
            // Retorna false si la actual era incorrecta.
            boolean cambiada = dao.cambiarContrasena(
                usuarioSesion.getId(),
                contrasennaActual.trim(),
                contrasennaNueva.trim()
            );

            if (cambiada) {
                // Contraseña cambiada exitosamente → redirigir con éxito
                response.sendRedirect(request.getContextPath() + "/perfil?exito=contrasenna_cambiada");
            } else {
                // La contraseña actual ingresada era incorrecta
                reenviarConError(request, response, usuarioSesion, "La contraseña actual es incorrecta");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            reenviarConError(request, response, usuarioSesion, "Error al cambiar contraseña");
        }
    }

    // =========================================================
    // HELPER: reenviar al JSP con error
    // =========================================================

    /**
     * Recarga el perfil y los géneros desde la BD y reenvía al JSP mi_perfil.jsp
     * con el mensaje de error.
     *
     * Se llama cada vez que una validación falla o hay un error de BD.
     * Sin este método, tendría que copiar las mismas 5 líneas en cada catch.
     *
     * @param request       para poner los atributos
     * @param response      para el forward al JSP
     * @param usuarioSesion para saber el ID del usuario y cargar su perfil
     * @param error         mensaje de error a mostrar en el JSP
     */
    private void reenviarConError(HttpServletRequest request, HttpServletResponse response,
                                   Usuario usuarioSesion, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error); // El JSP leerá este atributo para mostrar el mensaje
        try {
            PerfilDAO dao = new PerfilDAO();
            // Recargar el perfil completo para que el formulario quede prellenado
            request.setAttribute("perfil",  dao.obtenerPerfil(usuarioSesion.getId()));
            // Recargar los géneros para que el <select> funcione correctamente
            request.setAttribute("generos", dao.listarGeneros());
        } catch (SQLException e) {
            e.printStackTrace();
            // Si incluso esto falla, el JSP mostrará el error sin datos del perfil
        }
        request.getRequestDispatcher("/WEB-INF/jsp/perfil/mi_perfil.jsp")
                .forward(request, response);
    }
}
