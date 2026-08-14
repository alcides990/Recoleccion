-- Recalcula pago_hasta y periodo_pago para los comprobantes del lote de 2.000.
-- El acumulado considera todo el historial activo de cada cuenta.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_comprobantes_ultimos_2000;
CREATE TEMPORARY TABLE tmp_comprobantes_ultimos_2000 (
    nrorecibo VARCHAR(30), tiporecibo VARCHAR(10), cuenta VARCHAR(30),
    fechapago VARCHAR(30), codcob VARCHAR(30), cantidadperiodo VARCHAR(30),
    importe VARCHAR(30), nombre VARCHAR(255), inicio VARCHAR(30),
    catego VARCHAR(30), nromanzana VARCHAR(30), direccion VARCHAR(255)
);

source /home/alcides/migracion/cargar_comprobantes_ultimos_2000.sql

DROP TABLE IF EXISTS backup_periodos_ultimos_2000_20260813;
CREATE TABLE backup_periodos_ultimos_2000_20260813 AS
SELECT c.numero_comprobante, c.codigo_punto_expedicion, c.codigo_sucursal,
       c.codigo_serie, c.codigo_tipo_comprobante, c.pago_hasta, c.periodo_pago
FROM comprobantes c
JOIN tmp_comprobantes_ultimos_2000 l
  ON CAST(l.nrorecibo AS UNSIGNED) = c.numero_comprobante
 AND c.codigo_serie = CASE l.tiporecibo
     WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3
     WHEN 'D' THEN 4 WHEN 'G' THEN 7 ELSE 2 END
WHERE c.codigo_punto_expedicion = 1
  AND c.codigo_sucursal = 1
  AND c.codigo_tipo_comprobante = 1;

ALTER TABLE backup_periodos_ultimos_2000_20260813
ADD PRIMARY KEY (numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
                 codigo_serie, codigo_tipo_comprobante);

DROP TEMPORARY TABLE IF EXISTS tmp_periodos_calculados_2000;
CREATE TEMPORARY TABLE tmp_periodos_calculados_2000 AS
SELECT numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
       codigo_serie, codigo_tipo_comprobante, cantidad_pago,
       DATE_ADD(fecha_inicio, INTERVAL meses_previos MONTH) AS periodo_desde,
       DATE_ADD(fecha_inicio, INTERVAL (meses_previos + cantidad_pago) MONTH) AS nuevo_pago_hasta
FROM (
    SELECT c.numero_comprobante, c.codigo_punto_expedicion, c.codigo_sucursal,
           c.codigo_serie, c.codigo_tipo_comprobante,
           GREATEST(COALESCE(c.cantidad_pago, 0), 0) AS cantidad_pago,
           s.fecha_inicio,
           COALESCE(SUM(GREATEST(COALESCE(c.cantidad_pago, 0), 0)) OVER (
               PARTITION BY c.cuenta_corriente
               ORDER BY COALESCE(c.fecha_emision, TIMESTAMP(c.fecha_pago)),
                        c.codigo_sucursal, c.codigo_punto_expedicion,
                        c.codigo_serie, c.codigo_tipo_comprobante,
                        c.numero_comprobante
               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
           ), 0) AS meses_previos
    FROM comprobantes c
    JOIN servicios s ON TRIM(s.cuenta_corriente) = TRIM(c.cuenta_corriente)
    WHERE c.codigo_estado = 1
) historial;

ALTER TABLE tmp_periodos_calculados_2000
ADD PRIMARY KEY (numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
                 codigo_serie, codigo_tipo_comprobante);

START TRANSACTION;

UPDATE comprobantes c
JOIN tmp_periodos_calculados_2000 p
  ON p.numero_comprobante = c.numero_comprobante
 AND p.codigo_punto_expedicion = c.codigo_punto_expedicion
 AND p.codigo_sucursal = c.codigo_sucursal
 AND p.codigo_serie = c.codigo_serie
 AND p.codigo_tipo_comprobante = c.codigo_tipo_comprobante
JOIN backup_periodos_ultimos_2000_20260813 b
  ON b.numero_comprobante = c.numero_comprobante
 AND b.codigo_punto_expedicion = c.codigo_punto_expedicion
 AND b.codigo_sucursal = c.codigo_sucursal
 AND b.codigo_serie = c.codigo_serie
 AND b.codigo_tipo_comprobante = c.codigo_tipo_comprobante
SET c.pago_hasta = p.nuevo_pago_hasta,
    c.periodo_pago = CASE
        WHEN p.cantidad_pago <= 0 THEN NULL
        WHEN p.cantidad_pago = 1 THEN DATE_FORMAT(p.periodo_desde, '%m-%Y')
        ELSE CONCAT(DATE_FORMAT(p.periodo_desde, '%m-%Y'), ' / ',
                    DATE_FORMAT(DATE_SUB(p.nuevo_pago_hasta, INTERVAL 1 MONTH), '%m-%Y'))
    END
WHERE c.codigo_estado = 1;

SELECT ROW_COUNT() AS filas_actualizadas;
COMMIT;
