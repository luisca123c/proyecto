package com.dulce_gestion.utls;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilidad para obtener conexiones a la base de datos.
 * Usa JDBC con DriverManager (sin pool de conexiones).
 */
public class DB {

    private static final String URL     = "jdbc:mysql://localhost:3306/dulce_gestion?useSSL=false&serverTimezone=America/Bogota";
    private static final String USUARIO = "root";
    private static final String CLAVE   = "#Aprendiz2024";
    private static final String DRIVER  = "com.mysql.cj.jdbc.Driver";

    // Evitar instanciación
    private DB() {}

    /**
     * Abre y retorna una conexión a la BD.
     * El llamador es responsable de cerrarla (try-with-resources).
     */
    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado: " + e.getMessage(), e);
        }
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
