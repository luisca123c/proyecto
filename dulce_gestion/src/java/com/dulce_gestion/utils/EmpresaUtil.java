package com.dulce_gestion.utils;

import com.dulce_gestion.dao.UsuarioDAO;
import com.dulce_gestion.models.Usuario;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilidad para resolver el idEmprendimiento efectivo de un usuario.
 *
 * El problema: si un Admin inició sesión ANTES de que el login cargara
 * id_emprendimiento, su sesión tiene idEmprendimiento=0. Los DAOs
 * interpretan 0 como "sin filtro" (SuperAdmin viendo todo), así que
 * el Admin vería datos de todos los emprendimientos.
 *
 * Solución: para roles distintos de SuperAdmin, si la sesión tiene 0,
 * se consulta la BD y se corrige la sesión en el momento.
 */
public class EmpresaUtil {

    private EmpresaUtil() {}

    /**
     * Devuelve el idEmprendimiento correcto para el usuario en sesión.
     *
     * - SuperAdmin → siempre 0 (sin filtro forzado, puede elegir en el desplegable)
     * - Admin / Empleado con idEmprendimiento > 0 en sesión → lo usa directamente
     * - Admin / Empleado con idEmprendimiento = 0 en sesión (sesión vieja) →
     *   consulta la BD, actualiza la sesión y devuelve el valor real
     *
     * @param usuario  objeto Usuario de la sesión
     * @param request  para actualizar el objeto en sesión si fue necesario
     * @return idEmprendimiento real; 0 solo si es SuperAdmin
     */
    public static int resolverEmprendimiento(Usuario usuario,
                                             HttpServletRequest request) {
        if ("SuperAdministrador".equals(usuario.getNombreRol())) {
            return 0; // SuperAdmin no tiene emprendimiento fijo
        }

        int id = usuario.getIdEmprendimiento();
        if (id > 0) return id;

        // Sesión desactualizada: consultar BD y corregir
        try {
            int idReal = new UsuarioDAO().resolverIdEmprendimiento(usuario.getId());
            if (idReal > 0) {
                usuario.setIdEmprendimiento(idReal);
                // Actualizar sesión para que futuras peticiones no repitan la consulta
                request.getSession(false).setAttribute("usuario", usuario);
            }
            return idReal;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
