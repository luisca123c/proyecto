package com.dulce_gestion.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * ============================================================
 * UTILIDAD: DB
 * Usada por: todos los DAOs del proyecto
 * ============================================================
 *
 */
public class DB {

    // URL de conexión JDBC. Formato: jdbc:mysql://host:puerto/basededatos?params
    // localhost:3306 → servidor MySQL local en el puerto estándar
    // dulce_gestion  → nombre de la base de datos
    private static final String URL =
            "jdbc:mysql://localhost:3306/dulce_gestion?useSSL=false&allowPublicKeyRetrieval=true";

    private static final String USUARIO = "root";
    private static final String CLAVE   = "123456789Hola";

    // Nombre completo de la clase del driver JDBC de MySQL.
    // Requiere que el archivo mysql-connector-j-X.X.X.jar esté en Libraries del proyecto.
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    /** Constructor privado: esta clase no debe instanciarse, solo usarse con DB.obtenerConexion(). */
    private DB() {}

    /**
     * Registra el driver de MySQL en la JVM y abre una nueva conexión a la BD.
     *
     */
    public static Connection obtenerConexion() throws SQLException {
        try {
            // Cargar el driver de MySQL en la JVM para que DriverManager lo reconozca
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            // El .jar del connector no está en Libraries del proyecto
            throw new SQLException("Driver MySQL no encontrado. Verifique que mysql-connector-j esté en Libraries: " + e.getMessage(), e);
        }
        // Abrir y retornar la conexión usando las credenciales configuradas
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
