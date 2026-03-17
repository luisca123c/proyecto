drop database if exists dulce_gestion;
create database dulce_gestion;
use dulce_gestion;

-- ── CATÁLOGOS ─────────────────────────────────────────────────────────────

CREATE TABLE generos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE permisos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(150) NOT NULL
);

CREATE TABLE categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) UNIQUE NOT NULL,
    descripcion VARCHAR(150),
    activo TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE estado_carrito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE unidad_medida (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE metodo_pago (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1
);

-- ── EMPRENDIMIENTOS ───────────────────────────────────────────────────────
-- El SuperAdministrador gestiona todos los emprendimientos.
-- Admins y Empleados pertenecen a uno solo (definido en usuarios.id_emprendimiento).

CREATE TABLE emprendimientos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    nit VARCHAR(30) UNIQUE,
    direccion VARCHAR(150),
    ciudad VARCHAR(100),
    telefono VARCHAR(20),
    correo VARCHAR(100),
    estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── USUARIOS ──────────────────────────────────────────────────────────────

CREATE TABLE correos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    correo VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE telefonos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    telefono VARCHAR(20) UNIQUE NOT NULL
);

CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_correo INT NOT NULL UNIQUE,
    estado VARCHAR(50) NOT NULL,
    contrasena VARCHAR(100) NOT NULL,
    id_rol INT NOT NULL,
    id_emprendimiento INT NULL,        -- NULL solo para SuperAdministrador
    FOREIGN KEY (id_rol) REFERENCES roles(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (id_correo) REFERENCES correos(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_emprendimiento) REFERENCES emprendimientos(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE perfil_usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    id_usuario INT NOT NULL UNIQUE,
    id_telefono INT NOT NULL UNIQUE,
    id_genero INT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_genero) REFERENCES generos(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (id_telefono) REFERENCES telefonos(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE rol_permiso (
    id_permiso INT NOT NULL,
    id_rol INT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    FOREIGN KEY (id_permiso) REFERENCES permisos(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_rol) REFERENCES roles(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- ── PRODUCTOS ────────────────────────────────────────────────────────────
-- Cada producto pertenece a un emprendimiento directamente
-- porque un producto no tiene un "usuario responsable" fijo.

CREATE TABLE productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(100),
    stock_actual INT NOT NULL,
    id_unidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    estado VARCHAR(100) NOT NULL,
    id_categoria INT NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    id_emprendimiento INT NOT NULL,    -- FK directa: un producto pertenece a un emprendimiento
    FOREIGN KEY (id_unidad) REFERENCES unidad_medida(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (id_categoria) REFERENCES categorias(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (id_emprendimiento) REFERENCES emprendimientos(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ── CARRITO Y VENTAS ──────────────────────────────────────────────────────
-- El emprendimiento se deriva por: ventas → carrito → usuarios → id_emprendimiento
-- No hay redundancia: cada usuario ya sabe a qué emprendimiento pertenece.

CREATE TABLE carrito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    id_estado_carro INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_estado_carro) REFERENCES estado_carrito(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE detalle_carrito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_carrito INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    FOREIGN KEY (id_carrito) REFERENCES carrito(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_cantidad CHECK (cantidad > 0),
    UNIQUE KEY uq_carrito_producto (id_carrito, id_producto)
);

CREATE TABLE ventas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha_venta DATETIME NOT NULL,
    id_carrito INT NOT NULL,
    id_metodo_pago INT NOT NULL,
    total_venta DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_carrito) REFERENCES carrito(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ── GASTOS ────────────────────────────────────────────────────────────────
-- El emprendimiento se deriva por: gastos → detalle_compra → usuarios → id_emprendimiento
-- No hay redundancia.

CREATE TABLE compras (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha_compra DATETIME NOT NULL,
    total_compra DECIMAL(10,2) NOT NULL
);

CREATE TABLE detalle_compra (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,           -- de aquí se saca el emprendimiento
    descripcion VARCHAR(150),
    id_compra INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (id_compra) REFERENCES compras(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE gastos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_detalle_compra INT NOT NULL,
    id_metodo_pago INT NOT NULL,
    fecha_gasto DATETIME NOT NULL,
    total_gasto DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compra(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ── COMPRAS DE INSUMOS ────────────────────────────────────────────────────
-- El emprendimiento se deriva por: compras_insumos → usuarios → id_emprendimiento
-- No hay redundancia.

CREATE TABLE compras_insumos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,           -- de aquí se saca el emprendimiento
    descripcion VARCHAR(150) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    id_metodo_pago INT NOT NULL,
    fecha_compra DATETIME NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ── AUXILIARES ────────────────────────────────────────────────────────────

CREATE TABLE ingresos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha_ingreso DATETIME NOT NULL,
    id_venta INT NOT NULL,
    descripcion VARCHAR(150),
    FOREIGN KEY (id_venta) REFERENCES ventas(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE imagenes_producto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    path_imagen VARCHAR(200),
    alt_imagen VARCHAR(100),
    FOREIGN KEY (id_producto) REFERENCES productos(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE ganancias (
    id_ingreso INT NOT NULL,
    id_gasto INT NOT NULL,
    total_ganancia DECIMAL(10,2),
    PRIMARY KEY (id_ingreso, id_gasto),
    FOREIGN KEY (id_ingreso) REFERENCES ingresos(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_gasto) REFERENCES gastos(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- ════════════════════════════════════════════════════════════════
-- DATOS INICIALES
-- ════════════════════════════════════════════════════════════════

INSERT INTO generos (nombre) VALUES ('Masculino'), ('Femenino'), ('Otro');

INSERT INTO roles (nombre) VALUES ('SuperAdministrador'), ('Administrador'), ('Empleado');

INSERT INTO permisos (descripcion) VALUES
('VER_PRODUCTOS'), ('CREAR_PRODUCTO'), ('EDITAR_PRODUCTO'), ('ELIMINAR_PRODUCTO'),
('VER_VENTAS'), ('CREAR_VENTA'), ('ANULAR_VENTA'),
('VER_EMPLEADOS'), ('CREAR_EMPLEADO'), ('EDITAR_EMPLEADO'), ('ELIMINAR_EMPLEADO'),
('VER_GASTOS'), ('CREAR_GASTO'), ('ELIMINAR_GASTO'),
('VER_REPORTES'), ('VER_GANANCIAS'),
('GESTIONAR_ROLES'), ('GESTIONAR_PERMISOS');

INSERT INTO categorias (nombre, descripcion) VALUES
('Helados',         'Helados artesanales de diferentes sabores'),
('Postres Caseros', 'Tortas, brownies y dulces elaborados en casa'),
('Bebidas',         'Jugos, batidos y bebidas frías');

INSERT INTO estado_carrito (nombre) VALUES ('Activo'), ('Inactivo'), ('Cancelado');

INSERT INTO unidad_medida (nombre) VALUES ('Unidad'), ('Gramos'), ('Litros');

INSERT INTO metodo_pago (nombre) VALUES ('Efectivo'), ('Nequi');

-- ID 1: Dulce Gestión   ID 2: Postres del Valle
INSERT INTO emprendimientos (nombre, nit, direccion, ciudad, telefono, correo) VALUES
('Dulce Gestión',     '900123456-1', 'Calle 10 # 5-20',   'Bucaramanga', '3001234567', 'dulcegestion@gmail.com'),
('Postres del Valle', '900654321-2', 'Carrera 8 # 12-45', 'Medellín',    '3109876543', 'postresdelvalle@gmail.com');

INSERT INTO correos (correo) VALUES
('Luisca123c@gmail.com'),  -- 1 superadmin
('carlos@gmail.com'),      -- 2 admin1 emp1
('ana@gmail.com'),         -- 3 admin2 emp1
('pedro@gmail.com'),       -- 4 empleado1 emp1
('lucia@gmail.com'),       -- 5 empleado2 emp1
('sofia@gmail.com'),       -- 6 admin1 emp2
('miguel@gmail.com'),      -- 7 admin2 emp2
('valentina@gmail.com'),   -- 8 empleado1 emp2
('juan@gmail.com');        -- 9 empleado2 emp2

INSERT INTO telefonos (telefono) VALUES
('3154746303'),  -- 1 superadmin
('3101112233'),  -- 2 carlos
('3202223344'),  -- 3 ana
('3153334455'),  -- 4 pedro
('3004445566'),  -- 5 lucia
('3175556677'),  -- 6 sofia
('3006667788'),  -- 7 miguel
('3187778899'),  -- 8 valentina
('3118889900');  -- 9 juan

-- SuperAdmin contraseña existente | Demás: 123456
INSERT INTO usuarios (id_correo, estado, contrasena, id_rol, id_emprendimiento) VALUES
(1, 'Activo', '6243000b81daf96bbdf63efc4436fc07cb453df98a27196f7f549a9c8002635c', 1, NULL),
(2, 'Activo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, 1),
(3, 'Activo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, 1),
(4, 'Activo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 3, 1),
(5, 'Activo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 3, 1),
(6, 'Activo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, 2),
(7, 'Activo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 2, 2),
(8, 'Activo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 3, 2),
(9, 'Activo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 3, 2);

INSERT INTO perfil_usuario (nombre_completo, id_usuario, id_telefono, id_genero) VALUES
('Luis Carlos Villamizar', 1, 1, 1),
('Carlos Mendoza',         2, 2, 1),
('Ana Torres',             3, 3, 2),
('Pedro Gómez',            4, 4, 1),
('Lucía Ramírez',          5, 5, 2),
('Sofía Herrera',          6, 6, 2),
('Miguel Castillo',        7, 7, 1),
('Valentina Ruiz',         8, 8, 2),
('Juan Morales',           9, 9, 1);

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'SuperAdministrador';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Administrador'
AND p.descripcion NOT IN ('GESTIONAR_ROLES', 'GESTIONAR_PERMISOS');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Empleado'
AND p.descripcion IN ('VER_PRODUCTOS', 'CREAR_VENTA', 'VER_VENTAS');

-- IDs 1-5: Dulce Gestión  |  IDs 6-10: Postres del Valle
INSERT INTO productos (nombre, descripcion, stock_actual, id_unidad, precio_unitario, estado, id_categoria, fecha_vencimiento, id_emprendimiento) VALUES
('Helado de chocolate',  'Artesanal de chocolate intenso con cacao premium.',        50, 1, 2500, 'Activo', 1, '2026-09-30', 1),
('Helado de maracuyá',   'Sabor tropical con maracuyá natural.',                     50, 1, 2500, 'Activo', 1, '2026-09-30', 1),
('Helado de coco',       'Cremoso de coco rallado, textura suave.',                  40, 1, 2500, 'Activo', 1, '2026-09-30', 1),
('Helado de oreo',       'Con trozos de galleta Oreo y base de vainilla.',           40, 1, 3000, 'Activo', 1, '2026-09-30', 1),
('Helado de arequipe',   'Dulce de arequipe, textura cremosa y sabor tradicional.',  45, 1, 3000, 'Activo', 1, '2026-09-30', 1),
('Brownie de chocolate', 'Brownie húmedo con chispas de chocolate y nueces.',        30, 1, 4500, 'Activo', 2, '2026-06-30', 2),
('Cheesecake de fresas', 'Base de galleta, crema suave y cobertura de fresas.',      25, 1, 6000, 'Activo', 2, '2026-06-30', 2),
('Torta de vainilla',    'Torta esponjosa de vainilla con buttercream de fresa.',    20, 1, 8000, 'Activo', 2, '2026-06-30', 2),
('Helado de vainilla',   'Clásico helado de vainilla natural con semillas.',         35, 1, 2500, 'Activo', 1, '2026-09-30', 2),
('Paleta de mango',      'Paleta artesanal de mango con tajadas naturales.',         40, 1, 2000, 'Activo', 1, '2026-09-30', 2);

-- ════════════════════════════════════════════════════════════════
-- DATOS HISTÓRICOS (Enero–Marzo 2026)
-- Carrito, detalle_carrito y ventas: vacíos (los genera la app)
-- Enero: GANANCIA | Febrero: GANANCIA | Marzo: PÉRDIDA
-- El emprendimiento se deriva siempre del id_usuario registrado
-- ════════════════════════════════════════════════════════════════

-- ── COMPRAS DE INSUMOS ── 3 por mes, derivadas del usuario (emp1 o emp2)
INSERT INTO compras_insumos (id_usuario, descripcion, total, id_metodo_pago, fecha_compra) VALUES
-- Enero (total: 18,000)
(2, 'Leche entera para helados (20 L)',    5000.00, 1, '2026-01-05 09:00:00'),  -- carlos emp1
(6, 'Frutas tropicales: maracuyá y coco', 7000.00, 2, '2026-01-12 10:00:00'),  -- sofia  emp2
(3, 'Azúcar, crema de leche y vainilla',  6000.00, 1, '2026-01-19 11:00:00'),  -- ana    emp1
-- Febrero (total: 20,000)
(2, 'Cacao en polvo y chocolate bitter',  8000.00, 1, '2026-02-03 09:30:00'),  -- carlos emp1
(6, 'Galletas Oreo y arequipe x 2 kg',   6000.00, 2, '2026-02-10 10:30:00'),  -- sofia  emp2
(3, 'Harina, huevos y mantequilla',       6000.00, 1, '2026-02-17 11:30:00'),  -- ana    emp1
-- Marzo (total: 22,000)
(2, 'Frutas frescas: fresas y mangos',    7000.00, 1, '2026-03-02 09:00:00'),  -- carlos emp1
(6, 'Crema de leche y queso crema',       8000.00, 2, '2026-03-09 10:00:00'),  -- sofia  emp2
(3, 'Azúcar, colorantes y esencias',      7000.00, 1, '2026-03-16 11:00:00');  -- ana    emp1

-- ── GASTOS ── derivados del usuario registrado en detalle_compra
-- Enero gastos: 16,000 | Febrero: 20,000 | Marzo: 45,000

INSERT INTO compras (fecha_compra, total_compra) VALUES
('2026-01-07 08:00:00',  5500.00),   -- 1
('2026-01-14 08:00:00',  4500.00),   -- 2
('2026-01-21 08:00:00',  6000.00),   -- 3
('2026-02-04 08:00:00',  7000.00),   -- 4
('2026-02-11 08:00:00',  6500.00),   -- 5
('2026-02-18 08:00:00',  6500.00),   -- 6
('2026-03-03 08:00:00', 15000.00),   -- 7
('2026-03-10 08:00:00', 15000.00),   -- 8
('2026-03-17 08:00:00', 15000.00);   -- 9

INSERT INTO detalle_compra (id_usuario, descripcion, id_compra) VALUES
(2, 'Alquiler local Dulce Gestión enero',       1),   -- carlos emp1
(2, 'Servicios públicos enero',                 2),   -- carlos emp1
(6, 'Alquiler Postres del Valle enero',         3),   -- sofia  emp2
(2, 'Alquiler local Dulce Gestión febrero',     4),   -- carlos emp1
(2, 'Servicios públicos febrero',               5),   -- carlos emp1
(6, 'Alquiler Postres del Valle febrero',       6),   -- sofia  emp2
(2, 'Alquiler local Dulce Gestión marzo',       7),   -- carlos emp1
(2, 'Mantenimiento equipos refrigeración',      8),   -- carlos emp1
(6, 'Alquiler Postres del Valle + mora marzo',  9);   -- sofia  emp2

INSERT INTO gastos (id_detalle_compra, id_metodo_pago, fecha_gasto, total_gasto) VALUES
(1, 1, '2026-01-07 08:30:00',  5500.00),
(2, 1, '2026-01-14 08:30:00',  4500.00),
(3, 2, '2026-01-21 08:30:00',  6000.00),
(4, 1, '2026-02-04 08:30:00',  7000.00),
(5, 1, '2026-02-11 08:30:00',  6500.00),
(6, 2, '2026-02-18 08:30:00',  6500.00),
(7, 1, '2026-03-03 08:30:00', 15000.00),
(8, 1, '2026-03-10 08:30:00', 15000.00),
(9, 2, '2026-03-17 08:30:00', 15000.00);
