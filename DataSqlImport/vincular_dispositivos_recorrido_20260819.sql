CREATE TABLE IF NOT EXISTS dispositivos_cobrador (
    id_dispositivo VARCHAR(64) NOT NULL,
    codigo_sucursal INT NOT NULL,
    codigo_cobrador INT NULL,
    nombre_dispositivo VARCHAR(120) NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_conexion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id_dispositivo),
    KEY idx_dispositivo_cobrador (codigo_cobrador),
    CONSTRAINT fk_dispositivo_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales(codigo_sucursal),
    CONSTRAINT fk_dispositivo_cobrador FOREIGN KEY (codigo_cobrador) REFERENCES cobradores(codigo_cobrador)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE puntos_recorrido_cobrador
    ADD COLUMN id_dispositivo VARCHAR(64) NULL AFTER codigo_recorrido,
    ADD KEY idx_punto_dispositivo (id_dispositivo);
