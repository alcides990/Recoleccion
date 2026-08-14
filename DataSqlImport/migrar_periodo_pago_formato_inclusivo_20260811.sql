-- Convierte periodos históricos al formato inclusivo MM-yyyy / MM-yyyy.
-- Solo modifica comprobantes activos con cantidad de pago positiva y formato antiguo.

CREATE TABLE backup_periodo_pago_formato_20260811 AS
SELECT numero_comprobante,
       codigo_punto_expedicion,
       codigo_sucursal,
       codigo_serie,
       codigo_tipo_comprobante,
       periodo_pago
FROM comprobantes
WHERE codigo_estado = 1
  AND cantidad_pago > 0
  AND periodo_pago REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}/[0-9]{4}-[0-9]{2}-[0-9]{2}$';

ALTER TABLE backup_periodo_pago_formato_20260811
ADD PRIMARY KEY (
    numero_comprobante,
    codigo_punto_expedicion,
    codigo_sucursal,
    codigo_serie,
    codigo_tipo_comprobante
);

START TRANSACTION;

UPDATE comprobantes
SET periodo_pago = CASE
    WHEN cantidad_pago = 1 THEN
        DATE_FORMAT(
            STR_TO_DATE(TRIM(SUBSTRING_INDEX(periodo_pago, '/', 1)), '%Y-%m-%d'),
            '%m-%Y'
        )
    ELSE CONCAT(
        DATE_FORMAT(
            STR_TO_DATE(TRIM(SUBSTRING_INDEX(periodo_pago, '/', 1)), '%Y-%m-%d'),
            '%m-%Y'
        ),
        ' / ',
        DATE_FORMAT(
            DATE_ADD(
                STR_TO_DATE(TRIM(SUBSTRING_INDEX(periodo_pago, '/', 1)), '%Y-%m-%d'),
                INTERVAL (cantidad_pago - 1) MONTH
            ),
            '%m-%Y'
        )
    )
END
WHERE codigo_estado = 1
  AND cantidad_pago > 0
  AND periodo_pago REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}/[0-9]{4}-[0-9]{2}-[0-9]{2}$';

SET @filas_actualizadas = ROW_COUNT();

COMMIT;

SELECT @filas_actualizadas AS filas_actualizadas;

-- Restauración, si fuera necesaria:
-- UPDATE comprobantes c
-- JOIN backup_periodo_pago_formato_20260811 b
--   ON b.numero_comprobante = c.numero_comprobante
--  AND b.codigo_punto_expedicion = c.codigo_punto_expedicion
--  AND b.codigo_sucursal = c.codigo_sucursal
--  AND b.codigo_serie = c.codigo_serie
--  AND b.codigo_tipo_comprobante = c.codigo_tipo_comprobante
-- SET c.periodo_pago = b.periodo_pago;
