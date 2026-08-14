-- Migra el lote extraido del sistema anterior el 2026-08-13.
-- Requiere ejecutar el cliente con --local-infile=1.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_comprobantes_ultimos_2000;
CREATE TEMPORARY TABLE tmp_comprobantes_ultimos_2000 (
    nrorecibo VARCHAR(30),
    tiporecibo VARCHAR(10),
    cuenta VARCHAR(30),
    fechapago VARCHAR(30),
    codcob VARCHAR(30),
    cantidadperiodo VARCHAR(30),
    importe VARCHAR(30),
    nombre VARCHAR(255),
    inicio VARCHAR(30),
    catego VARCHAR(30),
    nromanzana VARCHAR(30),
    direccion VARCHAR(255)
);

source /home/alcides/migracion/cargar_comprobantes_ultimos_2000.sql

-- Controles previos: estos resultados permiten detectar filas que no pueden migrarse.
SELECT COUNT(*) AS filas_cargadas FROM tmp_comprobantes_ultimos_2000;

SELECT COUNT(*) AS cuentas_inexistentes
FROM tmp_comprobantes_ultimos_2000 o
LEFT JOIN servicios s ON s.cuenta_corriente = o.cuenta
WHERE s.cuenta_corriente IS NULL;

SELECT COUNT(*) AS categorias_inexistentes
FROM tmp_comprobantes_ultimos_2000 o
LEFT JOIN categorias cat ON cat.codigo_categoria = CASE
    WHEN o.catego = 'A240' THEN 1
    WHEN o.catego = 'FRI' THEN 2
    WHEN o.catego = '055' THEN 3
    ELSE CAST(NULLIF(o.catego, '') AS UNSIGNED)
END
WHERE cat.codigo_categoria IS NULL;

START TRANSACTION;

INSERT INTO comprobantes (
    numero_comprobante, codigo_tipo_comprobante, codigo_serie,
    codigo_punto_expedicion, codigo_sucursal, codigo_timbrado,
    cuenta_corriente, razon_social, fecha_emision, fecha_pago,
    cantidad_deuda, tarifa, cantidad_pago, recargo, saldo, total_importe,
    pago_hasta, periodo_pago, codigo_condicion_venta, codigo_usuario,
    codigo_cobrador, codigo_usuario_sistema, codigo_estado, codigo_categoria,
    codigo_comision
)
SELECT DISTINCT
    CAST(o.nrorecibo AS UNSIGNED),
    1,
    CASE o.tiporecibo
        WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3
        WHEN 'D' THEN 4 WHEN 'G' THEN 7 ELSE 2
    END,
    1, 1, 1,
    o.cuenta,
    TRIM(o.nombre),
    STR_TO_DATE(NULLIF(o.fechapago, ''), '%Y-%m-%d'),
    STR_TO_DATE(NULLIF(o.fechapago, ''), '%Y-%m-%d'),
    0,
    CASE
        WHEN CAST(COALESCE(NULLIF(o.cantidadperiodo, ''), '0') AS UNSIGNED) > 0
        THEN CAST(o.importe AS DECIMAL(12,2)) /
             CAST(o.cantidadperiodo AS UNSIGNED)
        ELSE COALESCE(cat.tarifa, 0)
    END,
    CAST(COALESCE(NULLIF(o.cantidadperiodo, ''), '0') AS UNSIGNED),
    0, 0,
    CAST(COALESCE(NULLIF(o.importe, ''), '0') AS DECIMAL(12,2)),
    NULL, NULL, 1,
    s.codigo_usuario,
    CAST(COALESCE(NULLIF(o.codcob, ''), '0') AS UNSIGNED),
    1,
    CASE WHEN CAST(COALESCE(NULLIF(o.cantidadperiodo, ''), '0') AS UNSIGNED) = 0
         THEN 3 ELSE 1 END,
    cat.codigo_categoria,
    1
FROM tmp_comprobantes_ultimos_2000 o
JOIN servicios s ON s.cuenta_corriente = o.cuenta
JOIN categorias cat ON cat.codigo_categoria = CASE
    WHEN o.catego = 'A240' THEN 1
    WHEN o.catego = 'FRI' THEN 2
    WHEN o.catego = '055' THEN 3
    ELSE CAST(NULLIF(o.catego, '') AS UNSIGNED)
END
WHERE NULLIF(o.nrorecibo, '') IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM comprobantes c
      WHERE c.numero_comprobante = CAST(o.nrorecibo AS UNSIGNED)
        AND c.codigo_tipo_comprobante = 1
        AND c.codigo_serie = CASE o.tiporecibo
            WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3
            WHEN 'D' THEN 4 WHEN 'G' THEN 7 ELSE 2
        END
        AND c.codigo_punto_expedicion = 1
        AND c.codigo_sucursal = 1
  );

SELECT ROW_COUNT() AS comprobantes_insertados;
COMMIT;
