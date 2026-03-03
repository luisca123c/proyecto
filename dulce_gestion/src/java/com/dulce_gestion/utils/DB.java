package com.dulce_gestion.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/*
 * Clase utilitaria para abrir la conexión a MySQL.
 * No se instancia, se llama directamente: DB.obtenerConexion()
 */
public class DB {

    // Dirección del servidor, puerto y nombre de la base de datos
    private static final String URL =
            "jdbc:mysql://localhost:3306/dulce_gestion?useSSL=false&allowPublicKeyRetrieval=true";

    private static final String USUARIO = "root";
    private static final String CLAVE   = "123456789Hola";

    // Clase del driver de MySQL — requiere el .jar en Libraries del proyecto
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // Constructor privado para que nadie pueda instanciar esta clase
    private DB() {}

    /*
     * Registra el driver de MySQL y abre una conexión a la base de datos.
     * El que llame este método debe cerrar la conexión al terminar,
     * usando try-with-resources para que se cierre automáticamente.
     */
    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName(DRIVER); // Carga el driver en la JVM
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado: " + e.getMessage(), e);
        }
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
