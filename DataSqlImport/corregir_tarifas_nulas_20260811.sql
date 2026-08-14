CREATE TABLE IF NOT EXISTS backup_tarifas_nulas_20260811 AS
SELECT numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
       codigo_serie, codigo_tipo_comprobante, tarifa
FROM comprobantes
WHERE tarifa IS NULL;

START TRANSACTION;

UPDATE comprobantes c
JOIN categorias cat ON cat.codigo_categoria = c.codigo_categoria
SET c.tarifa = COALESCE(cat.tarifa, 0)
WHERE c.tarifa IS NULL;

COMMIT;

ALTER TABLE comprobantes
MODIFY tarifa DOUBLE NOT NULL DEFAULT 0;
