package com.dulce_gestion.models;

import java.math.BigDecimal;

/**
 * ============================================================
 * MODELO: Producto
 * Tabla principal: productos
 * Tablas del JOIN: categorias, unidad_medida, imagenes_producto (LEFT JOIN)
 * Usado por: ProductoDAO, CarritoDAO, y los Servlets de productos y ventas
 * ============================================================
 *
 * ¿QUÉ REPRESENTA?
 * -----------------
 * Un producto del catálogo del negocio. Incluye tanto los campos propios
 * de la tabla productos como los campos denormalizados que vienen del JOIN:
 * el nombre de la categoría (en vez de solo su ID) y el nombre de la unidad.
 *
 * ¿POR QUÉ SE INCLUYEN nombreCategoria Y nombreUnidad SIENDO DE OTRAS TABLAS?
 * ----------------------------------------------------------------------------
 * ProductoDAO hace un JOIN con categorias y unidad_medida para traer el
 * nombre legible junto al ID. Si el modelo solo guardara idCategoria,
 * el JSP no podría mostrar "Lácteos" — solo vería "2".
 * Al incluir ambos en el modelo, el JSP puede mostrar el nombre
 * sin consultas adicionales:
 *   ${producto.nombreCategoria}  → "Lácteos"
 *   ${producto.idCategoria}      → 2  (para comparar en el <select> del form)
 *
 * ¿POR QUÉ pathImagen PUEDE SER null?
 * -------------------------------------
 * La imagen es opcional. ProductoDAO usa LEFT JOIN con imagenes_producto,
 * lo que significa que los productos sin imagen devuelven null en path_imagen.
 * El JSP verifica antes de mostrar el <img>:
 *   <c:if test="${not empty producto.pathImagen}">
 *     <img src="${ctx}/${producto.pathImagen}">
 *   </c:if>
 *
 * ¿POR QUÉ BigDecimal PARA precioUnitario?
 * -----------------------------------------
 * float y double no representan decimales exactamente. 2.10 en float
 * puede almacenarse como 2.09999999... lo que introduce errores en
 * cálculos financieros. BigDecimal representa el valor exacto tal como
 * lo devuelve MySQL (DECIMAL en la BD), sin pérdida de precisión.
 */
public class Producto {

    // ── Campos de la tabla productos ──────────────────────────────────────
    private int        id;
    private String     nombre;
    private String     descripcion;
    private int        stockActual;
    private BigDecimal precioUnitario;  // BigDecimal para exactitud monetaria
    private String     estado;          // "Disponible", "Agotado" o "Inactivo"
    private String     fechaVencimiento;
    private int        idCategoria;     // FK numérico (para pre-seleccionar en <select>)
    private int        idUnidad;        // FK numérico (para pre-seleccionar en <select>)

    // ── Campos denormalizados del JOIN ────────────────────────────────────
    private String     nombreCategoria; // Viene de categorias.nombre (no existe en productos)
    private String     nombreUnidad;    // Viene de unidad_medida.nombre

    // ── Campos de imagenes_producto (LEFT JOIN, pueden ser null) ──────────
    private String     pathImagen;      // Ruta relativa: "assets/images/productos/producto_3.jpg"
    private String     altImagen;       // Texto alternativo para <img alt="...">

    /** Constructor vacío requerido para que los DAOs puedan crear instancias
     *  con new Producto() y poblar los campos con setters. */
    public Producto() {}

    // =========================================================
    // GETTERS
    // =========================================================

    /** ID del producto (PK en la tabla productos, generado por AUTO_INCREMENT). */
    public int        getId()               { return id; }

    public String     getNombre()           { return nombre; }
    public String     getDescripcion()      { return descripcion; }

    /** Stock disponible actualmente. CarritoDAO lo usa para validar cantidades. */
    public int        getStockActual()      { return stockActual; }

    /**
     * Precio unitario como BigDecimal.
     * CarritoItem.getSubtotal() multiplica este valor por la cantidad
     * para calcular el subtotal sin pérdida de precisión.
     */
    public BigDecimal getPrecioUnitario()   { return precioUnitario; }

    /** Estado del producto: "Disponible", "Agotado" o "Inactivo". */
    public String     getEstado()           { return estado; }

    public String     getFechaVencimiento() { return fechaVencimiento; }

    /** ID numérico de la categoría. Se usa para pre-seleccionar la opción
     *  correcta en el <select> del formulario de edición. */
    public int        getIdCategoria()      { return idCategoria; }

    /** Nombre legible de la categoría (del JOIN con categorias). */
    public String     getNombreCategoria()  { return nombreCategoria; }

    /** ID numérico de la unidad de medida. */
    public int        getIdUnidad()         { return idUnidad; }

    /** Nombre legible de la unidad de medida (del JOIN con unidad_medida). */
    public String     getNombreUnidad()     { return nombreUnidad; }

    /**
     * Ruta relativa de la imagen del producto, o null si no tiene.
     * Ejemplo: "assets/images/productos/producto_3.jpg"
     * El JSP la usa como: <img src="${ctx}/${producto.pathImagen}">
     */
    public String     getPathImagen()       { return pathImagen; }

    /** Texto alternativo para la imagen (accesibilidad y SEO). */
    public String     getAltImagen()        { return altImagen; }

    // =========================================================
    // SETTERS
    // =========================================================

    public void setId(int id)                          { this.id = id; }
    public void setNombre(String nombre)               { this.nombre = nombre; }
    public void setDescripcion(String descripcion)     { this.descripcion = descripcion; }
    public void setStockActual(int stockActual)        { this.stockActual = stockActual; }
    public void setPrecioUnitario(BigDecimal precio)   { this.precioUnitario = precio; }
    public void setEstado(String estado)               { this.estado = estado; }
    public void setFechaVencimiento(String fecha)      { this.fechaVencimiento = fecha; }
    public void setIdCategoria(int idCategoria)        { this.idCategoria = idCategoria; }
    public void setNombreCategoria(String nombre)      { this.nombreCategoria = nombre; }
    public void setIdUnidad(int idUnidad)              { this.idUnidad = idUnidad; }
    public void setNombreUnidad(String nombre)         { this.nombreUnidad = nombre; }
    public void setPathImagen(String pathImagen)       { this.pathImagen = pathImagen; }
    public void setAltImagen(String altImagen)         { this.altImagen = altImagen; }
}
