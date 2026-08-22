-- Completa pago_hasta y periodo_pago de comprobantes importados pendientes.
--
-- Se consideran pendientes solamente los comprobantes:
--   * activos (codigo_estado = 1),
--   * con una cantidad_pago positiva, y
--   * con pago_hasta o periodo_pago sin completar.
--
-- Requiere MySQL 8 o superior (usa funciones de ventana).
-- El script es idempotente: al volver a ejecutarlo no toma filas ya completas.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_comprobantes_importados_pendientes;
CREATE TEMPORARY TABLE tmp_comprobantes_importados_pendientes AS
SELECT c.*
FROM comprobantes c
WHERE c.codigo_estado = 1
  AND COALESCE(c.cantidad_pago, 0) > 0
  AND (c.pago_hasta IS NULL
       OR c.periodo_pago IS NULL
       OR TRIM(c.periodo_pago) = '');

ALTER TABLE tmp_comprobantes_importados_pendientes
  ADD PRIMARY KEY (numero_comprobante, codigo_punto_expedicion,
                   codigo_sucursal, codigo_serie, codigo_tipo_comprobante);

-- Control previo: revisar esta lista antes de confirmar la actualización.
SELECT numero_comprobante,
       codigo_serie,
       cuenta_corriente,
       fecha_pago,
       cantidad_pago,
       pago_hasta,
       periodo_pago
FROM tmp_comprobantes_importados_pendientes
ORDER BY cuenta_corriente, fecha_pago, numero_comprobante;

-- Conserva los valores originales para una eventual restauración.
CREATE TABLE IF NOT EXISTS backup_periodos_importados_20260818 LIKE comprobantes;

INSERT IGNORE INTO backup_periodos_importados_20260818
SELECT c.*
FROM comprobantes c
JOIN tmp_comprobantes_importados_pendientes p
  ON p.numero_comprobante = c.numero_comprobante
 AND p.codigo_punto_expedicion = c.codigo_punto_expedicion
 AND p.codigo_sucursal = c.codigo_sucursal
 AND p.codigo_serie = c.codigo_serie
 AND p.codigo_tipo_comprobante = c.codigo_tipo_comprobante;

DROP TEMPORARY TABLE IF EXISTS tmp_periodos_importados_calculados;
CREATE TEMPORARY TABLE tmp_periodos_importados_calculados AS
SELECT ordenados.numero_comprobante,
       ordenados.codigo_punto_expedicion,
       ordenados.codigo_sucursal,
       ordenados.codigo_serie,
       ordenados.codigo_tipo_comprobante,
       ordenados.cuenta_corriente,
       ordenados.cantidad_pago,
       DATE_ADD(ordenados.periodo_base,
                INTERVAL ordenados.meses_previos MONTH) AS periodo_desde,
       DATE_ADD(ordenados.periodo_base,
                INTERVAL (ordenados.meses_previos + ordenados.cantidad_pago) MONTH)
           AS nuevo_pago_hasta
FROM (
    SELECT p.numero_comprobante,
           p.codigo_punto_expedicion,
           p.codigo_sucursal,
           p.codigo_serie,
           p.codigo_tipo_comprobante,
           p.cuenta_corriente,
           p.cantidad_pago,
           GREATEST(
               s.fecha_inicio,
               COALESCE(anteriores.ultimo_pago_hasta, s.fecha_inicio),
               COALESCE(exoneraciones.ultimo_periodo_exonerado, s.fecha_inicio)
           ) AS periodo_base,
           COALESCE(
               SUM(p.cantidad_pago) OVER (
                   PARTITION BY p.cuenta_corriente
                   ORDER BY COALESCE(p.fecha_emision, p.fecha_pago),
                            p.codigo_sucursal,
                            p.codigo_punto_expedicion,
                            p.codigo_serie,
                            p.codigo_tipo_comprobante,
                            p.numero_comprobante
                   ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
               ),
               0
           ) AS meses_previos
    FROM tmp_comprobantes_importados_pendientes p
    JOIN servicios s
      ON s.cuenta_corriente = p.cuenta_corriente
    LEFT JOIN (
        SELECT c.cuenta_corriente, MAX(c.pago_hasta) AS ultimo_pago_hasta
        FROM comprobantes c
        WHERE c.codigo_estado = 1
          AND c.pago_hasta IS NOT NULL
        GROUP BY c.cuenta_corriente
    ) anteriores
      ON anteriores.cuenta_corriente = p.cuenta_corriente
    LEFT JOIN (
        SELECT he.cuenta_corriente,
               MAX(he.fecha_desde_nueva) AS ultimo_periodo_exonerado
        FROM historial_exoneraciones_servicio he
        GROUP BY he.cuenta_corriente
    ) exoneraciones
      ON exoneraciones.cuenta_corriente = p.cuenta_corriente
) ordenados;

ALTER TABLE tmp_periodos_importados_calculados
  ADD PRIMARY KEY (numero_comprobante, codigo_punto_expedicion,
                   codigo_sucursal, codigo_serie, codigo_tipo_comprobante);

START TRANSACTION;

UPDATE comprobantes c
JOIN tmp_periodos_importados_calculados p
  ON p.numero_comprobante = c.numero_comprobante
 AND p.codigo_punto_expedicion = c.codigo_punto_expedicion
 AND p.codigo_sucursal = c.codigo_sucursal
 AND p.codigo_serie = c.codigo_serie
 AND p.codigo_tipo_comprobante = c.codigo_tipo_comprobante
SET c.pago_hasta = p.nuevo_pago_hasta,
    c.periodo_pago = CASE
        WHEN p.cantidad_pago = 1
            THEN DATE_FORMAT(p.periodo_desde, '%m-%Y')
        ELSE CONCAT(
            DATE_FORMAT(p.periodo_desde, '%m-%Y'),
            ' / ',
            DATE_FORMAT(DATE_SUB(p.nuevo_pago_hasta, INTERVAL 1 MONTH), '%m-%Y')
        )
    END;

SET @comprobantes_actualizados = ROW_COUNT();

COMMIT;

SELECT @comprobantes_actualizados AS comprobantes_actualizados;

-- Verificación: este resultado debe quedar vacío para los comprobantes
-- activos con cantidad_pago positiva.
SELECT numero_comprobante,
       codigo_serie,
       cuenta_corriente,
       pago_hasta,
       periodo_pago
FROM comprobantes
WHERE codigo_estado = 1
  AND COALESCE(cantidad_pago, 0) > 0
  AND (pago_hasta IS NULL
       OR periodo_pago IS NULL
       OR TRIM(periodo_pago) = '');

-- Restauración opcional (NO ejecutar salvo que sea necesario revertir):
-- UPDATE comprobantes c
-- JOIN backup_periodos_importados_20260818 b
--   ON b.numero_comprobante = c.numero_comprobante
--  AND b.codigo_punto_expedicion = c.codigo_punto_expedicion
--  AND b.codigo_sucursal = c.codigo_sucursal
--  AND b.codigo_serie = c.codigo_serie
--  AND b.codigo_tipo_comprobante = c.codigo_tipo_comprobante
-- SET c.pago_hasta = b.pago_hasta,
--     c.periodo_pago = b.periodo_pago;
