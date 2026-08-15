-- Completa los periodos del lote reciente importado y avanza fecha_desde.
-- Es idempotente: solo procesa comprobantes activos sin periodo/pago_hasta.
SET NAMES utf8mb4;

DROP TABLE IF EXISTS backup_comprobantes_periodos_recientes_20260814;
CREATE TABLE backup_comprobantes_periodos_recientes_20260814 AS
SELECT c.*
FROM comprobantes c
WHERE c.codigo_estado = 1
  AND c.fecha_pago >= '2026-08-13'
  AND c.periodo_pago IS NULL
  AND c.pago_hasta IS NULL;

ALTER TABLE backup_comprobantes_periodos_recientes_20260814
  ADD PRIMARY KEY (numero_comprobante, codigo_punto_expedicion,
                   codigo_sucursal, codigo_serie, codigo_tipo_comprobante);

DROP TABLE IF EXISTS backup_servicios_periodos_recientes_20260814;
CREATE TABLE backup_servicios_periodos_recientes_20260814 AS
SELECT DISTINCT s.*
FROM servicios s
JOIN backup_comprobantes_periodos_recientes_20260814 c
  ON c.cuenta_corriente = s.cuenta_corriente;

ALTER TABLE backup_servicios_periodos_recientes_20260814
  ADD PRIMARY KEY (cuenta_corriente);

DROP TEMPORARY TABLE IF EXISTS tmp_periodos_recientes;
CREATE TEMPORARY TABLE tmp_periodos_recientes AS
SELECT p.numero_comprobante,
       p.codigo_punto_expedicion,
       p.codigo_sucursal,
       p.codigo_serie,
       p.codigo_tipo_comprobante,
       p.cuenta_corriente,
       p.cantidad_pago,
       DATE_ADD(p.periodo_base, INTERVAL p.meses_previos MONTH) AS periodo_desde,
       DATE_ADD(p.periodo_base,
                INTERVAL (p.meses_previos + p.cantidad_pago) MONTH) AS nuevo_pago_hasta
FROM (
    SELECT b.numero_comprobante,
           b.codigo_punto_expedicion,
           b.codigo_sucursal,
           b.codigo_serie,
           b.codigo_tipo_comprobante,
           b.cuenta_corriente,
           GREATEST(COALESCE(b.cantidad_pago, 0), 0) AS cantidad_pago,
           COALESCE(prev.ultimo_pago_hasta, s.fecha_desde, s.fecha_inicio) AS periodo_base,
           COALESCE(SUM(GREATEST(COALESCE(b.cantidad_pago, 0), 0)) OVER (
               PARTITION BY b.cuenta_corriente
               ORDER BY b.fecha_pago, b.numero_comprobante,
                        b.codigo_sucursal, b.codigo_punto_expedicion,
                        b.codigo_serie, b.codigo_tipo_comprobante
               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
           ), 0) AS meses_previos
    FROM backup_comprobantes_periodos_recientes_20260814 b
    JOIN servicios s ON s.cuenta_corriente = b.cuenta_corriente
    LEFT JOIN (
        SELECT cuenta_corriente, MAX(pago_hasta) AS ultimo_pago_hasta
        FROM comprobantes
        WHERE codigo_estado = 1
          AND pago_hasta IS NOT NULL
        GROUP BY cuenta_corriente
    ) prev ON prev.cuenta_corriente = b.cuenta_corriente
) p;

ALTER TABLE tmp_periodos_recientes
  ADD PRIMARY KEY (numero_comprobante, codigo_punto_expedicion,
                   codigo_sucursal, codigo_serie, codigo_tipo_comprobante);

START TRANSACTION;

UPDATE comprobantes c
JOIN tmp_periodos_recientes p
  ON p.numero_comprobante = c.numero_comprobante
 AND p.codigo_punto_expedicion = c.codigo_punto_expedicion
 AND p.codigo_sucursal = c.codigo_sucursal
 AND p.codigo_serie = c.codigo_serie
 AND p.codigo_tipo_comprobante = c.codigo_tipo_comprobante
SET c.pago_hasta = p.nuevo_pago_hasta,
    c.periodo_pago = CASE
        WHEN p.cantidad_pago <= 0 THEN NULL
        WHEN p.cantidad_pago = 1 THEN DATE_FORMAT(p.periodo_desde, '%m-%Y')
        ELSE CONCAT(DATE_FORMAT(p.periodo_desde, '%m-%Y'), ' / ',
                    DATE_FORMAT(DATE_SUB(p.nuevo_pago_hasta, INTERVAL 1 MONTH), '%m-%Y'))
    END;

UPDATE servicios s
JOIN (
    SELECT cuenta_corriente, MAX(nuevo_pago_hasta) AS nueva_fecha_desde
    FROM tmp_periodos_recientes
    GROUP BY cuenta_corriente
) p ON p.cuenta_corriente = s.cuenta_corriente
SET s.fecha_desde = GREATEST(s.fecha_desde, p.nueva_fecha_desde);

COMMIT;

SELECT COUNT(*) AS comprobantes_respaldados
FROM backup_comprobantes_periodos_recientes_20260814;

SELECT COUNT(*) AS servicios_respaldados
FROM backup_servicios_periodos_recientes_20260814;
