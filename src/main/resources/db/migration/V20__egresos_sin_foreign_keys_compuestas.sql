DROP PROCEDURE IF EXISTS eliminar_foreign_keys_egresos;

DELIMITER $$

CREATE PROCEDURE eliminar_foreign_keys_egresos()
BEGIN
    DECLARE terminado BOOLEAN DEFAULT FALSE;
    DECLARE tabla_actual VARCHAR(64);
    DECLARE restriccion_actual VARCHAR(64);
    DECLARE relaciones CURSOR FOR
        SELECT DISTINCT kcu.TABLE_NAME, kcu.CONSTRAINT_NAME
          FROM information_schema.KEY_COLUMN_USAGE kcu
         WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
           AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
           AND kcu.TABLE_NAME IN ('egreso_catalogos', 'egresos', 'egreso_detalles');
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

CALL eliminar_foreign_keys_egresos();
DROP PROCEDURE eliminar_foreign_keys_egresos;

DROP PROCEDURE IF EXISTS eliminar_indice_egreso_si_existe;

DELIMITER $$

CREATE PROCEDURE eliminar_indice_egreso_si_existe(IN tabla VARCHAR(64), IN indice VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = tabla
           AND INDEX_NAME = indice
    ) THEN
        SET @ddl_idx = CONCAT('ALTER TABLE `', REPLACE(tabla, '`', '``'),
            '` DROP INDEX `', REPLACE(indice, '`', '``'), '`');
        PREPARE sentencia_idx FROM @ddl_idx;
        EXECUTE sentencia_idx;
        DEALLOCATE PREPARE sentencia_idx;
    END IF;
END$$

DELIMITER ;

CALL eliminar_indice_egreso_si_existe('egreso_catalogos', 'uq_egreso_catalogo_sucursal');
CALL eliminar_indice_egreso_si_existe('egreso_catalogos', 'idx_egreso_catalogos_categoria');
CALL eliminar_indice_egreso_si_existe('egreso_catalogos', 'fk_egreso_catalogos_categoria');
CALL eliminar_indice_egreso_si_existe('egresos', 'uq_egreso_sucursal');

DROP PROCEDURE eliminar_indice_egreso_si_existe;

ALTER TABLE egreso_catalogos
    ADD KEY idx_egreso_catalogos_categoria (categoria_id),
    ADD CONSTRAINT fk_egreso_catalogos_sucursal
        FOREIGN KEY (codigo_sucursal)
        REFERENCES sucursales (codigo_sucursal)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_egreso_catalogos_categoria
        FOREIGN KEY (categoria_id)
        REFERENCES egreso_catalogos (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;

ALTER TABLE egresos
    ADD CONSTRAINT fk_egresos_sucursal
        FOREIGN KEY (codigo_sucursal)
        REFERENCES sucursales (codigo_sucursal)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_egresos_tipo
        FOREIGN KEY (tipo_id)
        REFERENCES egreso_catalogos (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_egresos_proveedor
        FOREIGN KEY (proveedor_id)
        REFERENCES egreso_catalogos (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;

ALTER TABLE egreso_detalles
    ADD CONSTRAINT fk_egreso_detalles_sucursal
        FOREIGN KEY (codigo_sucursal)
        REFERENCES sucursales (codigo_sucursal)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_egreso_detalles_egreso
        FOREIGN KEY (egreso_id)
        REFERENCES egresos (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_egreso_detalles_producto
        FOREIGN KEY (producto_id)
        REFERENCES egreso_catalogos (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;
