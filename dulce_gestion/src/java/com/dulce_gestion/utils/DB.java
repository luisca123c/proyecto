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
 * ¿QUÉ HACE?
 * ----------
 * Centraliza la configuración de conexión a MySQL y expone un único
 * método estático para obtener una conexión: DB.obtenerConexion().
 *
 * ¿POR QUÉ UNA CLASE UTILITARIA EN VEZ DE PONER EL URL EN CADA DAO?
 * ------------------------------------------------------------------
 * Si el URL, usuario o contraseña estuvieran copiados en los 14 DAOs,
 * cambiar el servidor o la clave significaría editar 14 archivos.
 * Al tenerlo aquí, se cambia en un solo lugar.
 *
 * ¿POR QUÉ EL CONSTRUCTOR ES PRIVADO?
 * -------------------------------------
 * Esta clase no necesita ser instanciada: no tiene estado de instancia,
 * solo campos y métodos estáticos. El constructor privado lo hace explícito:
 * nadie puede escribir new DB(). Es el patrón estándar para clases utilitarias.
 *
 * ¿POR QUÉ NO SE USA UN CONNECTION POOL?
 * ----------------------------------------
 * Un pool (como HikariCP o el DataSource de Tomcat) reutiliza conexiones
 * abiertas para evitar el costo de crear y destruir una conexión en cada
 * petición. Para un sistema de gestión de uso interno con pocos usuarios
 * concurrentes, DriverManager.getConnection() es suficiente y más simple.
 * Si el sistema creciera a muchos usuarios simultáneos, migrar a un pool
 * implicaría solo cambiar este método sin tocar ningún DAO.
 *
 * ¿QUÉ HACE useSSL=false EN EL URL?
 * ------------------------------------
 * En entornos de desarrollo local, MySQL suele no tener un certificado SSL
 * configurado. Sin este parámetro, el driver lanzaría una advertencia de
 * conexión no segura en cada petición. En producción debería habilitarse SSL.
 *
 * ¿QUÉ HACE allowPublicKeyRetrieval=true?
 * ----------------------------------------
 * Con ciertos métodos de autenticación de MySQL 8+ (caching_sha2_password),
 * el driver necesita recuperar la clave pública del servidor para el handshake.
 * Sin este parámetro, la conexión fallaría con "Public Key Retrieval is not allowed".
 * En producción con SSL activo, esto no es necesario.
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
     * ¿QUÉ ES Class.forName()?
     * -------------------------
     * Class.forName(nombreClase) carga la clase del driver en la JVM.
     * Al cargarse, el driver se registra automáticamente en DriverManager
     * (via un bloque static dentro del driver).
     * Sin este paso, DriverManager.getConnection() no sabría qué driver usar
     * para las URLs que empiezan con "jdbc:mysql://".
     *
     * En versiones modernas del driver (JDBC 4.0+, Java 6+), la carga
     * automática del driver via ServiceLoader hace que Class.forName() sea
     * técnicamente innecesario. Se mantiene por claridad y compatibilidad.
     *
     * ¿POR QUÉ SE RE-LANZA ClassNotFoundException COMO SQLException?
     * ----------------------------------------------------------------
     * Los DAOs solo declaran "throws SQLException" en sus métodos. Si
     * Class.forName fallara y lanzara ClassNotFoundException (excepción
     * checked distinta), los DAOs tendrían que declarar esa excepción
     * también o capturarla individualmente. Envolviéndola en SQLException
     * aquí, el contrato de todos los DAOs se mantiene limpio.
     *
     * ¿QUIÉN CIERRA ESTA CONEXIÓN?
     * -----------------------------
     * El que llama a obtenerConexion() es responsable de cerrarla.
     * Todos los DAOs usan try-with-resources:
     *   try (Connection con = DB.obtenerConexion()) { ... }
     * Al salir del bloque try (con éxito o con excepción), Java llama
     * automáticamente a con.close(), devolviendo la conexión al sistema.
     *
     * @return  conexión activa a la base de datos dulce_gestion
     * @throws SQLException si el driver no está disponible o si MySQL
     *                      rechaza la conexión (credenciales incorrectas,
     *                      servidor apagado, base de datos inexistente)
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
