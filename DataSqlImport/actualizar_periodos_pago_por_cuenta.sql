-- Recalcula pago_hasta y periodo_pago acumulando los meses pagados por cuenta.
-- Los pagos realizados en el mismo instante se ordenan por la clave completa
-- del comprobante para que cada uno reciba un periodo diferente y reproducible.

CREATE TABLE backup_periodo_pago_20260809 AS
SELECT numero_comprobante,
       codigo_punto_expedicion,
       codigo_sucursal,
       codigo_serie,
       codigo_tipo_comprobante,
       pago_hasta,
       periodo_pago
FROM comprobantes;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_periodos_pago;

CREATE TEMPORARY TABLE tmp_periodos_pago AS
SELECT numero_comprobante,
       codigo_punto_expedicion,
       codigo_sucursal,
       codigo_serie,
       codigo_tipo_comprobante,
       DATE_ADD(fecha_inicio, INTERVAL meses_previos MONTH) AS periodo_desde,
       DATE_ADD(
           fecha_inicio,
           INTERVAL (meses_previos + GREATEST(COALESCE(cantidad_pago, 0), 0)) MONTH
       ) AS nuevo_pago_hasta
FROM (
    SELECT c.numero_comprobante,
           c.codigo_punto_expedicion,
           c.codigo_sucursal,
           c.codigo_serie,
           c.codigo_tipo_comprobante,
           c.cantidad_pago,
           s.fecha_inicio,
           COALESCE(
               SUM(GREATEST(COALESCE(c.cantidad_pago, 0), 0)) OVER (
                   PARTITION BY c.cuenta_corriente
                   ORDER BY COALESCE(c.fecha_emision, TIMESTAMP(c.fecha_pago)),
                            c.codigo_sucursal,
                            c.codigo_punto_expedicion,
                            c.codigo_serie,
                            c.codigo_tipo_comprobante,
                            c.numero_comprobante
                   ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
               ),
               0
           ) AS meses_previos
    FROM comprobantes c
    JOIN servicios s
      ON TRIM(s.cuenta_corriente) = TRIM(c.cuenta_corriente)
    WHERE c.codigo_estado = 1
) pagos_ordenados;

ALTER TABLE tmp_periodos_pago
ADD PRIMARY KEY (
    numero_comprobante,
    codigo_punto_expedicion,
    codigo_sucursal,
    codigo_serie,
    codigo_tipo_comprobante
);

UPDATE comprobantes c
JOIN tmp_periodos_pago t
  ON t.numero_comprobante = c.numero_comprobante
 AND t.codigo_punto_expedicion = c.codigo_punto_expedicion
 AND t.codigo_sucursal = c.codigo_sucursal
 AND t.codigo_serie = c.codigo_serie
 AND t.codigo_tipo_comprobante = c.codigo_tipo_comprobante
SET c.pago_hasta = t.nuevo_pago_hasta,
    c.periodo_pago = CONCAT(
        DATE_FORMAT(t.periodo_desde, '%Y-%m-%d'),
        '/',
        DATE_FORMAT(t.nuevo_pago_hasta, '%Y-%m-%d')
    )
WHERE c.codigo_estado = 1;

SET @filas_actualizadas = ROW_COUNT();

COMMIT;

SELECT @filas_actualizadas AS filas_actualizadas;

-- Para restaurar los valores anteriores si fuera necesario:
-- UPDATE comprobantes c
-- JOIN backup_periodo_pago_20260809 b
--   ON b.numero_comprobante = c.numero_comprobante
--  AND b.codigo_punto_expedicion = c.codigo_punto_expedicion
--  AND b.codigo_sucursal = c.codigo_sucursal
--  AND b.codigo_serie = c.codigo_serie
--  AND b.codigo_tipo_comprobante = c.codigo_tipo_comprobante
-- SET c.pago_hasta = b.pago_hasta,
--     c.periodo_pago = b.periodo_pago;
