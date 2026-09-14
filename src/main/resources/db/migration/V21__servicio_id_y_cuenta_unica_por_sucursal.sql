-- Agrega una PK simple a servicios y permite repetir cuenta_corriente en
-- distintas sucursales mediante UNIQUE(cuenta_corriente, codigo_sucursal).

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DROP PROCEDURE IF EXISTS eliminar_fks_a_servicios;

DELIMITER $$

CREATE PROCEDURE eliminar_fks_a_servicios()
BEGIN
    DECLARE terminado BOOLEAN DEFAULT FALSE;
    DECLARE tabla_actual VARCHAR(64);
    DECLARE restriccion_actual VARCHAR(64);
    DECLARE relaciones CURSOR FOR
        SELECT DISTINCT kcu.TABLE_NAME, kcu.CONSTRAINT_NAME
          FROM information_schema.KEY_COLUMN_USAGE kcu
         WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
           AND kcu.REFERENCED_TABLE_NAME = 'servicios';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET terminado = TRUE;

    OPEN relaciones;
    ciclo: LOOP
        FETCH relaciones INTO tabla_actual, restriccion_actual;
        IF terminado THEN LEAVE ciclo; END IF;
        SET @ddl_fk = CONCAT('ALTER TABLE `', REPLACE(tabla_actual, '`', '``'),
            '` DROP FOREIGN KEY `', REPLACE(restriccion_actual, '`', '``'), '`');
        PREPARE sentencia_fk FROM @ddl_fk;
        EXECUTE sentencia_fk;
        DEALLOCATE PREPARE sentencia_fk;
    END LOOP;
    CLOSE relaciones;
END$$

DELIMITER ;

CALL eliminar_fks_a_servicios();
DROP PROCEDURE eliminar_fks_a_servicios;

ALTER TABLE servicios
    ADD COLUMN codigo_servicio INT NULL FIRST;

SET @codigo_servicio := 0;
UPDATE servicios
   SET codigo_servicio = (@codigo_servicio := @codigo_servicio + 1)
 ORDER BY codigo_sucursal, cuenta_corriente;

ALTER TABLE servicios
    DROP PRIMARY KEY,
    MODIFY codigo_servicio INT NOT NULL AUTO_INCREMENT,
    ADD PRIMARY KEY (codigo_servicio),
    ADD UNIQUE INDEX uq_servicios_cuenta_sucursal (cuenta_corriente, codigo_sucursal);

ALTER TABLE ubicaciones_servicio
    ADD COLUMN codigo_sucursal INT NULL AFTER cuenta_corriente;

UPDATE ubicaciones_servicio u
JOIN servicios s ON s.cuenta_corriente = u.cuenta_corriente
   SET u.codigo_sucursal = s.codigo_sucursal
 WHERE u.codigo_sucursal IS NULL;

ALTER TABLE ubicaciones_servicio
    MODIFY codigo_sucursal INT NOT NULL,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (cuenta_corriente, codigo_sucursal),
    ADD INDEX idx_ubicacion_servicio_sucursal (codigo_sucursal);

ALTER TABLE historial_ubicaciones_servicio
    ADD COLUMN codigo_sucursal INT NULL AFTER cuenta_corriente;

UPDATE historial_ubicaciones_servicio h
JOIN servicios s ON s.cuenta_corriente = h.cuenta_corriente
   SET h.codigo_sucursal = s.codigo_sucursal
 WHERE h.codigo_sucursal IS NULL;

ALTER TABLE historial_ubicaciones_servicio
    MODIFY codigo_sucursal INT NOT NULL,
    ADD INDEX idx_historial_ubicacion_cuenta_sucursal
        (cuenta_corriente, codigo_sucursal, fecha_modificacion DESC, codigo_historial DESC);

ALTER TABLE historial_suspensiones_servicio
    ADD COLUMN codigo_sucursal INT NULL AFTER cuenta_corriente;

UPDATE historial_suspensiones_servicio h
JOIN servicios s ON s.cuenta_corriente = h.cuenta_corriente
   SET h.codigo_sucursal = s.codigo_sucursal
 WHERE h.codigo_sucursal IS NULL;

ALTER TABLE historial_suspensiones_servicio
    MODIFY codigo_sucursal INT NOT NULL,
    ADD INDEX idx_suspension_cuenta_sucursal
        (cuenta_corriente, codigo_sucursal, fecha_desde DESC, codigo_suspension DESC);

ALTER TABLE historial_exoneraciones_servicio
    ADD COLUMN codigo_sucursal INT NULL AFTER cuenta_corriente;

UPDATE historial_exoneraciones_servicio h
JOIN servicios s ON s.cuenta_corriente = h.cuenta_corriente
   SET h.codigo_sucursal = s.codigo_sucursal
 WHERE h.codigo_sucursal IS NULL;

ALTER TABLE historial_exoneraciones_servicio
    MODIFY codigo_sucursal INT NOT NULL,
    ADD INDEX idx_exoneracion_cuenta_sucursal
        (cuenta_corriente, codigo_sucursal, fecha_registro DESC, codigo_exoneracion DESC);

ALTER TABLE comprobantes
    ADD INDEX idx_comprobantes_cuenta_sucursal (cuenta_corriente, codigo_sucursal),
    ADD CONSTRAINT fk_comprobantes_servicio
        FOREIGN KEY (cuenta_corriente, codigo_sucursal)
        REFERENCES servicios (cuenta_corriente, codigo_sucursal)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE ubicaciones_servicio
    ADD CONSTRAINT fk_ubicacion_servicio
        FOREIGN KEY (cuenta_corriente, codigo_sucursal)
        REFERENCES servicios (cuenta_corriente, codigo_sucursal)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE historial_ubicaciones_servicio
    ADD CONSTRAINT fk_historial_ubicacion_servicio
        FOREIGN KEY (cuenta_corriente, codigo_sucursal)
        REFERENCES servicios (cuenta_corriente, codigo_sucursal)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE historial_suspensiones_servicio
    ADD CONSTRAINT fk_suspension_servicio
        FOREIGN KEY (cuenta_corriente, codigo_sucursal)
        REFERENCES servicios (cuenta_corriente, codigo_sucursal)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE historial_exoneraciones_servicio
    ADD CONSTRAINT fk_exoneracion_servicio
        FOREIGN KEY (cuenta_corriente, codigo_sucursal)
        REFERENCES servicios (cuenta_corriente, codigo_sucursal)
        ON UPDATE CASCADE ON DELETE RESTRICT;
