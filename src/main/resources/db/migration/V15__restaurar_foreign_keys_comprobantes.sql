-- Restaura las relaciones de comprobantes y las referencias hacia cobradores.
-- La migracion no corrige ni elimina datos: se detiene si la restauracion dejo
-- referencias huerfanas.

DROP PROCEDURE IF EXISTS validar_referencias_comprobantes;

DELIMITER $$

CREATE PROCEDURE validar_referencias_comprobantes()
BEGIN
    DECLARE cantidad_huerfanos BIGINT DEFAULT 0;

    SELECT
        (SELECT COUNT(*) FROM comprobantes c LEFT JOIN timbrados t
            ON t.codigo_timbrado = c.codigo_timbrado WHERE t.codigo_timbrado IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN condiciones_venta t
            ON t.codigo_condicion_venta = c.codigo_condicion_venta WHERE t.codigo_condicion_venta IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN usuarios_sistema t
            ON t.codigo_usuario_sistema = c.codigo_usuario_sistema WHERE t.codigo_usuario_sistema IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN estados t
            ON t.codigo_estado = c.codigo_estado WHERE t.codigo_estado IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN servicios t
            ON t.cuenta_corriente = c.cuenta_corriente WHERE t.cuenta_corriente IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN usuarios t
            ON t.codigo_usuario = c.codigo_usuario WHERE t.codigo_usuario IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN puntos_expedicion t
            ON t.codigo_punto_expedicion = c.codigo_punto_expedicion
           AND t.codigo_sucursal = c.codigo_sucursal
          WHERE t.codigo_punto_expedicion IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN categorias t
            ON t.codigo_categoria = c.codigo_categoria WHERE t.codigo_categoria IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN cobradores t
            ON t.codigo_cobrador = c.codigo_cobrador WHERE t.codigo_cobrador IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN series t
            ON t.codigo_serie = c.codigo_serie WHERE t.codigo_serie IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN tipos_comprobante t
            ON t.codigo_tipo_comprobante = c.codigo_tipo_comprobante WHERE t.codigo_tipo_comprobante IS NULL)
      + (SELECT COUNT(*) FROM comprobantes c LEFT JOIN comisiones t
            ON t.codigo_comision = c.codigo_comision WHERE t.codigo_comision IS NULL)
      + (SELECT COUNT(*) FROM zonas z LEFT JOIN cobradores c
            ON c.codigo_cobrador = z.codigo_cobrador WHERE c.codigo_cobrador IS NULL)
      + (SELECT COUNT(*) FROM recorridos_cobrador r LEFT JOIN cobradores c
            ON c.codigo_cobrador = r.codigo_cobrador WHERE c.codigo_cobrador IS NULL)
      + (SELECT COUNT(*) FROM dispositivos_cobrador d LEFT JOIN cobradores c
            ON c.codigo_cobrador = d.codigo_cobrador
          WHERE d.codigo_cobrador IS NOT NULL AND c.codigo_cobrador IS NULL)
      INTO cantidad_huerfanos;

    IF cantidad_huerfanos > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se pueden crear las FK: existen referencias huerfanas. Complete primero la restauracion.';
    END IF;
END$$

DELIMITER ;

CALL validar_referencias_comprobantes();
DROP PROCEDURE validar_referencias_comprobantes;

-- Elimina las FK salientes actuales de comprobantes y las FK existentes hacia
-- cobradores para reconstruirlas con reglas uniformes.
DROP PROCEDURE IF EXISTS eliminar_foreign_keys_a_restaurar;

DELIMITER $$

CREATE PROCEDURE eliminar_foreign_keys_a_restaurar()
BEGIN
    DECLARE terminado BOOLEAN DEFAULT FALSE;
    DECLARE tabla_actual VARCHAR(64);
    DECLARE restriccion_actual VARCHAR(64);
    DECLARE relaciones CURSOR FOR
        SELECT DISTINCT kcu.TABLE_NAME, kcu.CONSTRAINT_NAME
          FROM information_schema.KEY_COLUMN_USAGE kcu
         WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
           AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
           AND (kcu.TABLE_NAME = 'comprobantes'
                OR (kcu.REFERENCED_TABLE_NAME = 'cobradores'
                    AND kcu.TABLE_NAME IN ('zonas', 'recorridos_cobrador', 'dispositivos_cobrador')));
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET terminado = TRUE;

    OPEN relaciones;
    ciclo: LOOP
        FETCH relaciones INTO tabla_actual, restriccion_actual;
        IF terminado THEN
            LEAVE ciclo;
        END IF;
        SET @ddl_fk = CONCAT('ALTER TABLE `', REPLACE(tabla_actual, '`', '``'),
            '` DROP FOREIGN KEY `', REPLACE(restriccion_actual, '`', '``'), '`');
        PREPARE sentencia_fk FROM @ddl_fk;
        EXECUTE sentencia_fk;
        DEALLOCATE PREPARE sentencia_fk;
    END LOOP;
    CLOSE relaciones;
END$$

DELIMITER ;

CALL eliminar_foreign_keys_a_restaurar();
DROP PROCEDURE eliminar_foreign_keys_a_restaurar;

ALTER TABLE comprobantes
    ADD CONSTRAINT fk_comprobantes_timbrado
        FOREIGN KEY (codigo_timbrado) REFERENCES timbrados (codigo_timbrado)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_condicion_venta
        FOREIGN KEY (codigo_condicion_venta) REFERENCES condiciones_venta (codigo_condicion_venta)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_usuario_sistema
        FOREIGN KEY (codigo_usuario_sistema) REFERENCES usuarios_sistema (codigo_usuario_sistema)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_estado
        FOREIGN KEY (codigo_estado) REFERENCES estados (codigo_estado)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_servicio
        FOREIGN KEY (cuenta_corriente) REFERENCES servicios (cuenta_corriente)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_usuario
        FOREIGN KEY (codigo_usuario) REFERENCES usuarios (codigo_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_punto_expedicion
        FOREIGN KEY (codigo_punto_expedicion, codigo_sucursal)
        REFERENCES puntos_expedicion (codigo_punto_expedicion, codigo_sucursal)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_categoria
        FOREIGN KEY (codigo_categoria) REFERENCES categorias (codigo_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_cobrador
        FOREIGN KEY (codigo_cobrador) REFERENCES cobradores (codigo_cobrador)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_serie
        FOREIGN KEY (codigo_serie) REFERENCES series (codigo_serie)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_tipo
        FOREIGN KEY (codigo_tipo_comprobante) REFERENCES tipos_comprobante (codigo_tipo_comprobante)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_comprobantes_comision
        FOREIGN KEY (codigo_comision) REFERENCES comisiones (codigo_comision)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE zonas
    ADD CONSTRAINT fk_zonas_cobrador
        FOREIGN KEY (codigo_cobrador) REFERENCES cobradores (codigo_cobrador)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE recorridos_cobrador
    ADD CONSTRAINT fk_recorridos_cobrador
        FOREIGN KEY (codigo_cobrador) REFERENCES cobradores (codigo_cobrador)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE dispositivos_cobrador
    ADD CONSTRAINT fk_dispositivos_cobrador
        FOREIGN KEY (codigo_cobrador) REFERENCES cobradores (codigo_cobrador)
        ON UPDATE CASCADE ON DELETE RESTRICT;
