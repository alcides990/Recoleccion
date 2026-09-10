CREATE TABLE egreso_catalogos (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 codigo_sucursal INT NOT NULL,
 clase VARCHAR(15) NOT NULL,
 nombre VARCHAR(180) NOT NULL,
 documento VARCHAR(40) NOT NULL DEFAULT '',
 telefono VARCHAR(60) NOT NULL DEFAULT '',
 direccion VARCHAR(250) NOT NULL DEFAULT '',
 UNIQUE KEY uq_egreso_catalogo (codigo_sucursal, clase, nombre),
 UNIQUE KEY uq_egreso_catalogo_sucursal (id, codigo_sucursal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE egresos (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 codigo_sucursal INT NOT NULL,
 codigo_usuario_sistema INT NOT NULL,
 fecha DATE NOT NULL,
 fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 tipo_id BIGINT NOT NULL,
 proveedor_id BIGINT NOT NULL,
 comprobante VARCHAR(100) NOT NULL DEFAULT '',
 observacion VARCHAR(500) NOT NULL DEFAULT '',
 total DECIMAL(18,2) NOT NULL,
 anulado BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uq_egreso_sucursal (id, codigo_sucursal),
 KEY idx_egreso_fecha (codigo_sucursal, fecha),
 FOREIGN KEY (tipo_id, codigo_sucursal) REFERENCES egreso_catalogos (id, codigo_sucursal),
 FOREIGN KEY (proveedor_id, codigo_sucursal) REFERENCES egreso_catalogos (id, codigo_sucursal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE egreso_detalles (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 egreso_id BIGINT NOT NULL,
 codigo_sucursal INT NOT NULL,
 producto_id BIGINT NOT NULL,
 descripcion VARCHAR(180) NOT NULL,
 cantidad DECIMAL(12,3) NOT NULL,
 precio DECIMAL(18,2) NOT NULL,
 subtotal DECIMAL(18,2) NOT NULL,
 FOREIGN KEY (egreso_id, codigo_sucursal) REFERENCES egresos (id, codigo_sucursal),
 FOREIGN KEY (producto_id, codigo_sucursal) REFERENCES egreso_catalogos (id, codigo_sucursal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
