-- El cambio de número de una factura manual debe propagarse a sus tablas dependientes.
-- La eliminación de un comprobante se restringe para conservar su integridad e historial.

SET @fk_detalle = (
    SELECT kcu.CONSTRAINT_NAME
      FROM information_schema.KEY_COLUMN_USAGE kcu
     WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
       AND kcu.TABLE_NAME = 'detalle_pago'
       AND kcu.REFERENCED_TABLE_NAME = 'comprobantes'
     LIMIT 1
);
SET @sql = IF(@fk_detalle IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE detalle_pago DROP FOREIGN KEY `', REPLACE(@fk_detalle, '`', '``'), '`'));
PREPARE sentencia FROM @sql;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;

ALTER TABLE detalle_pago
    ADD CONSTRAINT fk_detalle_pago_comprobantes
    FOREIGN KEY (numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
                 codigo_serie, codigo_tipo_comprobante)
    REFERENCES comprobantes (numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
                             codigo_serie, codigo_tipo_comprobante)
    ON UPDATE CASCADE
    ON DELETE RESTRICT;
