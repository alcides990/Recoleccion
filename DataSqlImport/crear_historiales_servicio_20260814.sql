SET NAMES utf8mb4;
START TRANSACTION;

ALTER TABLE servicios
    ADD COLUMN ocupado VARCHAR(15) NOT NULL DEFAULT 'OCUPADO' AFTER fecha_inicio,
    ADD COLUMN fecha_desde DATE NULL AFTER fecha_inicio;

UPDATE servicios
SET fecha_desde = fecha_inicio
WHERE fecha_desde IS NULL;

ALTER TABLE servicios
    MODIFY fecha_desde DATE NOT NULL;

CREATE TABLE historial_suspensiones_servicio (
    codigo_suspension BIGINT NOT NULL AUTO_INCREMENT,
    cuenta_corriente VARCHAR(30) NOT NULL,
    fecha_desde DATE NOT NULL,
    fecha_hasta DATE NULL,
    cantidad_periodos_pendientes INT NOT NULL DEFAULT 0,
    motivo VARCHAR(500) NOT NULL,
    codigo_usuario_sistema INT NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (codigo_suspension),
    INDEX idx_suspension_cuenta_fecha (cuenta_corriente, fecha_desde DESC),
    CONSTRAINT fk_suspension_servicio FOREIGN KEY (cuenta_corriente)
        REFERENCES servicios (cuenta_corriente),
    CONSTRAINT fk_suspension_usuario_sistema FOREIGN KEY (codigo_usuario_sistema)
        REFERENCES usuarios_sistema (codigo_usuario_sistema)
);

CREATE TABLE historial_exoneraciones_servicio (
    codigo_exoneracion BIGINT NOT NULL AUTO_INCREMENT,
    cuenta_corriente VARCHAR(30) NOT NULL,
    fecha_desde_anterior DATE NOT NULL,
    fecha_desde_nueva DATE NOT NULL,
    cantidad_periodos INT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    codigo_usuario_sistema INT NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (codigo_exoneracion),
    INDEX idx_exoneracion_cuenta_fecha (cuenta_corriente, fecha_registro DESC),
    CONSTRAINT fk_exoneracion_servicio FOREIGN KEY (cuenta_corriente)
        REFERENCES servicios (cuenta_corriente),
    CONSTRAINT fk_exoneracion_usuario_sistema FOREIGN KEY (codigo_usuario_sistema)
        REFERENCES usuarios_sistema (codigo_usuario_sistema)
);

COMMIT;
