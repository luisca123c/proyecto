drop database if exists dulce_gestion;
create database dulce_gestion;
use dulce_gestion;
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
    descripcion VARCHAR(150)
);
CREATE TABLE estado_carrito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);
CREATE TABLE unidad_medida (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);
CREATE TABLE metodo_pago (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);
create table correos(
	id INT AUTO_INCREMENT PRIMARY KEY,
	correo VARCHAR(100) UNIQUE NOT NULL
);
create table telefonos(
	id INT AUTO_INCREMENT PRIMARY KEY,
	telefono VARCHAR(20) UNIQUE NOT NULL
);
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_correo INT NOT NULL UNIQUE,
    estado VARCHAR(50) NOT NULL,
    contrasena VARCHAR(100) NOT NULL,
    id_rol INT NOT NULL,
    FOREIGN KEY (id_rol) REFERENCES roles(id)
    ON DELETE RESTRICT
	ON UPDATE CASCADE,
    foreign key (id_correo) references correos(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE
);
CREATE TABLE perfil_usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
	nombre_completo VARCHAR(100) NOT NULL,
    id_usuario INT NOT NULL UNIQUE,
    id_telefono INT NOT NULL UNIQUE,
    id_genero INT NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE,
    FOREIGN KEY (id_genero) REFERENCES generos(id)
    ON DELETE RESTRICT
	ON UPDATE CASCADE,
	foreign key (id_telefono) references telefonos(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE
);
CREATE TABLE rol_permiso (
    id_permiso INT NOT NULL,
    id_rol INT NOT NULL,
    primary key (id_rol, id_permiso),
    FOREIGN KEY (id_permiso) REFERENCES permisos(id)
	ON DELETE CASCADE
	ON UPDATE CASCADE,
	FOREIGN KEY (id_rol) REFERENCES roles(id)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);
CREATE TABLE productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    descripcion VARCHAR(200),
    stock_actual INT NOT NULL,
    id_unidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    estado VARCHAR(100) NOT NULL,
    id_categoria INT NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    FOREIGN KEY (id_unidad) REFERENCES unidad_medida(id)
    ON DELETE RESTRICT 
    ON UPDATE CASCADE,
    FOREIGN KEY (id_categoria) REFERENCES categorias(id)
    ON DELETE RESTRICT 
    ON UPDATE CASCADE
);
CREATE TABLE carrito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    fecha_creacion DATETIME,
    fecha_actualizacion DATETIME,
    id_estado_carro INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE,
    FOREIGN KEY (id_estado_carro) REFERENCES estado_carrito(id)
    ON DELETE RESTRICT
	ON UPDATE CASCADE
);
CREATE TABLE compras (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fecha_compra DATETIME NOT NULL,
  total_compra DECIMAL(10,2) NOT NULL
);
CREATE TABLE detalle_compra (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    descripcion VARCHAR(150),
    id_compra INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
    ON DELETE RESTRICT
	ON UPDATE CASCADE,
    FOREIGN KEY (id_compra) REFERENCES compras(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE
);
CREATE TABLE ventas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha_venta DATETIME NOT NULL,
    id_carrito INT NOT NULL,
    id_metodo_pago INT NOT NULL,
    total_venta DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_carrito) REFERENCES carrito(id)
	ON DELETE RESTRICT
	ON UPDATE CASCADE,
	FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago(id)
	ON DELETE RESTRICT
	ON UPDATE CASCADE
);
CREATE TABLE detalle_carrito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_carrito INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    FOREIGN KEY (id_carrito) REFERENCES carrito(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id)
    ON DELETE RESTRICT
	ON UPDATE CASCADE
);
CREATE TABLE ingresos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha_ingreso DATETIME NOT NULL,
    id_venta INT NOT NULL,
    descripcion VARCHAR(150),
    FOREIGN KEY (id_venta) REFERENCES ventas(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE
);
CREATE TABLE gastos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_detalle_compra INT NOT NULL,
    id_metodo_pago INT NOT NULL,
    fecha_gasto DATETIME NOT NULL,
    total_gasto DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compra(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE,
    FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago(id)
    ON DELETE RESTRICT
	ON UPDATE CASCADE
);
CREATE TABLE imagenes_producto (
	id INT AUTO_INCREMENT PRIMARY KEY,
	id_producto INT NOT NULL,
	path_imagen VARCHAR(200),
	alt_imagen VARCHAR(100),
	FOREIGN KEY (id_producto) REFERENCES productos(id)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);
CREATE TABLE ganancias (
    id_ingreso INT NOT NULL,
    id_gasto INT NOT NULL,
    total_ganancia DECIMAL(10,2),
    PRIMARY KEY (id_ingreso, id_gasto),
    FOREIGN KEY (id_ingreso) REFERENCES ingresos(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE,
    FOREIGN KEY (id_gasto) REFERENCES gastos(id)
    ON DELETE CASCADE
	ON UPDATE CASCADE
);

alter table detalle_carrito
ADD CONSTRAINT chk_cantidad CHECK (cantidad > 0);

ALTER TABLE detalle_carrito
ADD UNIQUE KEY uq_carrito_producto (id_carrito, id_producto);

ALTER TABLE carrito
  MODIFY fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY fecha_actualizacion DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE perfil_usuario
  MODIFY fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  MODIFY fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
  
INSERT INTO generos (nombre) VALUES
('Masculino'),
('Femenino'),
('Otro');

INSERT INTO roles (nombre) VALUES
('SuperAdministrador'),
('Administrador'),
('Empleado');

INSERT INTO permisos (descripcion) VALUES
('VER_PRODUCTOS'),
('CREAR_PRODUCTO'),
('EDITAR_PRODUCTO'),
('ELIMINAR_PRODUCTO'),
('VER_VENTAS'),
('CREAR_VENTA'),
('ANULAR_VENTA'),
('VER_EMPLEADOS'),
('CREAR_EMPLEADO'),
('EDITAR_EMPLEADO'),
('ELIMINAR_EMPLEADO'),
('VER_GASTOS'),
('CREAR_GASTO'),
('ELIMINAR_GASTO'),
('VER_REPORTES'),
('VER_GANANCIAS'),
('GESTIONAR_ROLES'),
('GESTIONAR_PERMISOS');

INSERT INTO categorias (nombre, descripcion) VALUES
('Helados', 'Helados caseros de diferentes sabores');

INSERT INTO estado_carrito (nombre) VALUES
('Activo'),
('Inactivo'),
('Cancelado');

INSERT INTO unidad_medida (nombre) VALUES
('Unidad'),
('Gramos'),
('Litros');

INSERT INTO metodo_pago (nombre) VALUES
('Efectivo'),
('Nequi');

INSERT INTO correos (correo) VALUES
('Luisca123c@gmail.com');

INSERT INTO telefonos (telefono) VALUES
('3154746303');

INSERT INTO usuarios (id_correo, estado, contrasena, id_rol) VALUES
(1, 'Activo', '123456789hola', 1);

INSERT INTO perfil_usuario (nombre_completo, id_usuario, id_telefono, id_genero)VALUES 
('Luis Carlos Villamizar', 1, 1, 1);

insert into rol_permiso (id_rol, id_permiso) 
select r.id, p.id
from roles r, permisos p 
where r.nombre = 'SuperAdministrador';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id
FROM roles r, permisos p
WHERE r.nombre = 'Administrador'
AND p.descripcion NOT IN ('GESTIONAR_ROLES', 'GESTIONAR_PERMISOS');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id
FROM roles r, permisos p
WHERE r.nombre = 'Empleado'
AND p.descripcion IN (
    'VER_PRODUCTOS',
    'CREAR_VENTA',
    'VER_VENTAS'
);

INSERT INTO productos (nombre, descripcion, stock_actual, id_unidad, precio_unitario, estado, id_categoria, fecha_vencimiento) VALUES
('Helado de chocolate','Helado artesanal de chocolate intenso elaborado con cacao premium.',10,1,1000,'Activo',1,'2026-09-30'),
('Helado de maracuyá','Helado refrescante de maracuyá natural con sabor tropical.',10,1,1000,'Activo',1,'2026-09-30'),
('Helado de coco','Helado cremoso de coco rallado con textura suave y tropical.',10,1,1000,'Activo',1,'2026-09-30'),
('Helado de oreo','Helado cremoso con trozos de galleta Oreo y base de vainilla.',10,1,1000,'Activo',1,'2026-09-30'),
('Helado de arequipe','Helado dulce de arequipe con textura cremosa y sabor tradicional.',10,1,1000,'Activo',1,'2026-09-30');

