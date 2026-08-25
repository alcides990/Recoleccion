-- Migra los comprobantes faltantes obtenidos del sistema anterior el 2026-08-22.
-- Requiere la tabla de auditoria:
--   recoleccion_migracion.comprobantes_origen_20260822
--
-- Criterios de seguridad:
--   * una sola fila por numero/serie del origen;
--   * no duplica claves existentes;
--   * reconoce como existente una factura corregida a otra serie;
--   * excluye filas sin cuenta y numeros de comprobante invalidos;
--   * crea las tres cuentas nuevas encontradas en el origen.

SET NAMES utf8mb4;
USE recoleccion;

START TRANSACTION;

SET @siguiente_usuario := (SELECT COALESCE(MAX(codigo_usuario), 0) FROM usuarios);

INSERT INTO usuarios (
    codigo_usuario, codigo_tipo_documento, numero_documento, nombre, apellido,
    celular, telefono, barrio, direccion, observacion, codigo_sucursal,
    codigo_estado, cuenta_temporal
)
SELECT
    (@siguiente_usuario := @siguiente_usuario + 1), 1, x.numero_documento,
    x.nombre, '', x.celular, NULL, '', x.direccion, '', 1, 1,
    x.cuenta_corriente
FROM (
    SELECT '31-0218-54' cuenta_corriente, '0' numero_documento,
           'LUZ DELGADILLO' nombre, '0' celular,
           'PUERTAS DE KATUETE' direccion
    UNION ALL
    SELECT '31-0859-12', '0', 'LUIS ARCE. EMPLEADO MINICIPAL',
           '0985982979', 'KAATY'
    UNION ALL
    SELECT '31-0886-02', '0', 'SONIA SOSA', '0', 'CRECER'
) x
LEFT JOIN usuarios u ON u.cuenta_temporal = x.cuenta_corriente
LEFT JOIN servicios s ON s.cuenta_corriente = x.cuenta_corriente
WHERE u.codigo_usuario IS NULL
  AND s.cuenta_corriente IS NULL
ORDER BY x.cuenta_corriente;

INSERT INTO servicios (
    cuenta_corriente, direccion, fecha_inicio, codigo_usuario,
    codigo_categoria, codigo_sucursal, codigo_manzana, codigo_estado,
    observacion
)
SELECT x.cuenta_corriente, x.direccion, x.fecha_inicio, u.codigo_usuario,
       x.codigo_categoria, 1, x.codigo_manzana, 1, ''
FROM (
    SELECT '31-0218-54' cuenta_corriente, 'PUERTAS DE KATUETE' direccion,
           DATE('2026-06-01') fecha_inicio, 66 codigo_categoria,
           218 codigo_manzana
    UNION ALL
    SELECT '31-0859-12', 'KAATY', DATE('2026-07-01'), 69, 859
    UNION ALL
    SELECT '31-0886-02', 'CRECER', DATE('2026-07-01'), 69, 886
) x
JOIN usuarios u ON u.cuenta_temporal = x.cuenta_corriente
LEFT JOIN servicios s ON s.cuenta_corriente = x.cuenta_corriente
WHERE s.cuenta_corriente IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_comprobantes_migrar_20260822;
CREATE TEMPORARY TABLE tmp_comprobantes_migrar_20260822 AS
WITH origen_unico AS (
    SELECT o.*,
           ROW_NUMBER() OVER (
               PARTITION BY o.numero_comprobante, o.codigo_serie
               ORDER BY o.orden_origen
           ) AS rn,
           CASE
               WHEN o.categoria_origen = 'A240' THEN 1
               WHEN o.categoria_origen = 'FRI' THEN 2
               WHEN o.categoria_origen = '055' THEN 3
               WHEN o.categoria_origen REGEXP '^[0-9]+$'
                   THEN CAST(o.categoria_origen AS UNSIGNED)
               ELSE NULL
           END AS codigo_categoria_mapeada
    FROM recoleccion_migracion.comprobantes_origen_20260822 o
), candidatos AS (
    SELECT o.*, s.codigo_usuario, cat.tarifa AS tarifa_categoria,
           COALESCE(prev.ultimo_pago_hasta, s.fecha_inicio) AS periodo_base
    FROM origen_unico o
    JOIN servicios s ON s.cuenta_corriente = o.cuenta_corriente
    JOIN categorias cat
      ON cat.codigo_categoria = o.codigo_categoria_mapeada
    LEFT JOIN (
        SELECT cuenta_corriente, MAX(pago_hasta) AS ultimo_pago_hasta
        FROM comprobantes
        WHERE pago_hasta IS NOT NULL
        GROUP BY cuenta_corriente
    ) prev ON prev.cuenta_corriente = o.cuenta_corriente
    WHERE o.rn = 1
      AND o.cuenta_corriente <> ''
      AND o.numero_comprobante > 0
      -- Fila corrupta del origen: el numero contiene la cuenta 31-0338-19.
      AND o.numero_comprobante <> 31033819
      AND NOT EXISTS (
          SELECT 1
          FROM comprobantes c
          WHERE c.numero_comprobante = o.numero_comprobante
            AND c.codigo_punto_expedicion = 1
            AND c.codigo_sucursal = 1
            AND c.codigo_serie = o.codigo_serie
            AND c.codigo_tipo_comprobante = 1
      )
      -- Evita duplicar la factura 42238, corregida de serie B a E.
      AND NOT EXISTS (
          SELECT 1
          FROM comprobantes c
          WHERE c.numero_comprobante = o.numero_comprobante
            AND c.codigo_punto_expedicion = 1
            AND c.codigo_sucursal = 1
            AND c.codigo_tipo_comprobante = 1
            AND c.cuenta_corriente = o.cuenta_corriente
            AND c.fecha_pago = o.fecha_pago
            AND ABS(COALESCE(c.total_importe, 0) - COALESCE(o.importe, 0)) < 0.01
      )
), periodos AS (
    SELECT c.*,
           SUM(c.cantidad_pago) OVER (
               PARTITION BY c.cuenta_corriente
               ORDER BY c.fecha_pago, c.numero_comprobante, c.codigo_serie
               ROWS UNBOUNDED PRECEDING
           ) - c.cantidad_pago AS meses_previos
    FROM candidatos c
)
SELECT * FROM periodos;

INSERT INTO comprobantes (
    numero_comprobante, codigo_tipo_comprobante, codigo_serie,
    codigo_punto_expedicion, codigo_sucursal, codigo_timbrado,
    cuenta_corriente, razon_social, fecha_emision, fecha_pago,
    cantidad_deuda, tarifa, cantidad_pago, recargo, saldo, total_importe,
    pago_hasta, periodo_pago, codigo_condicion_venta, codigo_usuario,
    codigo_cobrador, codigo_usuario_sistema, codigo_estado,
    codigo_categoria, codigo_comision
)
SELECT
    p.numero_comprobante, 1, p.codigo_serie, 1, 1, 1,
    p.cuenta_corriente, LEFT(TRIM(p.razon_social), 100),
    p.fecha_pago, p.fecha_pago, 0,
    CASE WHEN p.cantidad_pago > 0
         THEN p.importe / p.cantidad_pago
         ELSE COALESCE(p.tarifa_categoria, 0) END,
    p.cantidad_pago, 0, 0, p.importe,
    CASE WHEN p.cantidad_pago > 0
         THEN DATE_ADD(p.periodo_base,
                       INTERVAL (p.meses_previos + p.cantidad_pago) MONTH)
         ELSE NULL END,
    CASE
        WHEN p.cantidad_pago <= 0 THEN NULL
        WHEN p.cantidad_pago = 1 THEN
            DATE_FORMAT(
                DATE_ADD(p.periodo_base, INTERVAL p.meses_previos MONTH),
                '%m-%Y'
            )
        ELSE CONCAT(
            DATE_FORMAT(
                DATE_ADD(p.periodo_base, INTERVAL p.meses_previos MONTH),
                '%m-%Y'
            ),
            ' / ',
            DATE_FORMAT(
                DATE_ADD(
                    p.periodo_base,
                    INTERVAL (p.meses_previos + p.cantidad_pago - 1) MONTH
                ),
                '%m-%Y'
            )
        )
    END,
    1, p.codigo_usuario, p.codigo_cobrador, 1,
    CASE WHEN p.cantidad_pago = 0 THEN 3 ELSE 1 END,
    p.codigo_categoria_mapeada, 1
FROM tmp_comprobantes_migrar_20260822 p;

SELECT COUNT(*) AS comprobantes_insertados
FROM tmp_comprobantes_migrar_20260822;

COMMIT;

