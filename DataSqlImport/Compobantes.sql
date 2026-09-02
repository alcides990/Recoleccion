-- Importacion idempotente desde recoleccion_migracion.comprobantes.
-- Configuracion fiscal del lote historico:
--   sucursal 1, punto 1, timbrado 1 y tipo de comprobante 1.
-- La serie se obtiene de tiporecibo.

SET NAMES utf8mb4;
USE recoleccion;

SET @codigo_sucursal := 1;
SET @codigo_punto_expedicion := 1;
SET @codigo_timbrado := 1;
SET @codigo_tipo_comprobante := 1;
SET @codigo_condicion_venta := 1;
SET @codigo_usuario_sistema := 1;
SET @codigo_comision := 1;

DROP TEMPORARY TABLE IF EXISTS tmp_comprobantes_importar;
CREATE TEMPORARY TABLE tmp_comprobantes_importar AS
WITH origen_normalizado AS (
    SELECT
        o.*,
        CAST(TRIM(o.nrorecibo) AS UNSIGNED) AS numero_normalizado,
        CASE TRIM(o.tiporecibo)
            WHEN 'A' THEN 1
            WHEN 'B' THEN 2
            WHEN 'C' THEN 3
            WHEN 'D' THEN 4
            WHEN 'G' THEN 7
            WHEN ''  THEN 2
            ELSE NULL
        END AS serie_normalizada,
        CASE
            WHEN TRIM(o.catego) = 'A240' THEN 1
            WHEN TRIM(o.catego) = 'FRI' THEN 2
            WHEN TRIM(o.catego) = '055' THEN 3
            WHEN TRIM(o.catego) REGEXP '^[0-9]+$'
                THEN CAST(TRIM(o.catego) AS UNSIGNED)
            ELSE NULL
        END AS categoria_normalizada,
        COALESCE(CAST(NULLIF(TRIM(o.cantidadperiodo), 'None') AS UNSIGNED), 0)
            AS cantidad_pago_normalizada,
        COALESCE(CAST(NULLIF(TRIM(o.importe), 'None') AS DECIMAL(12,2)), 0)
            AS importe_normalizado,
        ROW_NUMBER() OVER (
            PARTITION BY TRIM(o.nrorecibo), TRIM(o.tiporecibo)
            ORDER BY STR_TO_DATE(NULLIF(TRIM(o.fechapago), 'None'), '%Y-%m-%d %H:%i') DESC
        ) AS rn
    FROM recoleccion_migracion.comprobantes o
    WHERE o.nrorecibo IS NOT NULL
      AND TRIM(o.nrorecibo) REGEXP '^[0-9]+$'
      AND CAST(TRIM(o.nrorecibo) AS UNSIGNED) BETWEEN 1 AND 9999999
      AND o.cuenta IS NOT NULL
      AND TRIM(o.cuenta) NOT IN ('', 'None')
), candidatos AS (
    SELECT
        o.numero_normalizado AS numero_comprobante,
        @codigo_punto_expedicion AS codigo_punto_expedicion,
        NULLIF(TRIM(pe.punto_expedicion), '') AS punto_expedicion_fiscal,
        @codigo_sucursal AS codigo_sucursal,
        NULLIF(TRIM(su.sucursal), '') AS establecimiento_fiscal,
        o.serie_normalizada AS codigo_serie,
        NULLIF(TRIM(se.serie), '') AS serie_fiscal,
        @codigo_tipo_comprobante AS codigo_tipo_comprobante,
        @codigo_timbrado AS codigo_timbrado,
        CAST(t.numero_timbrado AS CHAR) AS numero_timbrado_fiscal,
        t.fecha_inicio AS inicio_vigencia_fiscal,
        t.fecha_fin AS fin_vigencia_fiscal,
        @codigo_condicion_venta AS codigo_condicion_venta,
        s.cuenta_corriente,
        s.codigo_usuario,
        cat.codigo_categoria,
        LEFT(NULLIF(TRIM(o.nombre), 'None'), 100) AS razon_social,
        STR_TO_DATE(NULLIF(TRIM(o.fechapago), 'None'), '%Y-%m-%d %H:%i') AS fecha_emision,
        DATE(STR_TO_DATE(NULLIF(TRIM(o.fechapago), 'None'), '%Y-%m-%d %H:%i')) AS fecha_pago,
        0 AS cantidad_deuda,
        CASE
            WHEN o.cantidad_pago_normalizada > 0
                THEN o.importe_normalizado / o.cantidad_pago_normalizada
            ELSE COALESCE(cat.tarifa, 0)
        END AS tarifa,
        o.cantidad_pago_normalizada AS cantidad_pago,
        0 AS recargo,
        0 AS saldo,
        o.importe_normalizado AS total_importe,
        NULL AS pago_hasta,
        NULL AS periodo_pago,
        @codigo_usuario_sistema AS codigo_usuario_sistema,
        CASE WHEN o.cantidad_pago_normalizada = 0 THEN 3 ELSE 1 END AS codigo_estado,
        NULLIF(TRIM(COALESCE(NULLIF(o.obs, 'None'), NULLIF(o.observacion, 'None'))), '') AS obs,
        cob.codigo_cobrador,
        @codigo_comision AS codigo_comision
    FROM origen_normalizado o
    JOIN recoleccion.servicios s
      ON s.cuenta_corriente = TRIM(o.cuenta) COLLATE utf8mb4_0900_ai_ci
    JOIN recoleccion.categorias cat
      ON cat.codigo_categoria = o.categoria_normalizada
    JOIN recoleccion.cobradores cob
      ON cob.codigo_cobrador = CAST(NULLIF(TRIM(o.codcob), 'None') AS UNSIGNED)
    JOIN recoleccion.series se
      ON se.codigo_serie = o.serie_normalizada
    JOIN recoleccion.sucursales su
      ON su.codigo_sucursal = @codigo_sucursal
    JOIN recoleccion.puntos_expedicion pe
      ON pe.codigo_sucursal = su.codigo_sucursal
     AND pe.codigo_punto_expedicion = @codigo_punto_expedicion
    JOIN recoleccion.timbrados t
      ON t.codigo_timbrado = @codigo_timbrado
     AND t.codigo_empresa = su.codigo_empresa
    JOIN recoleccion.tipos_comprobante tc
      ON tc.codigo_tipo_comprobante = @codigo_tipo_comprobante
    JOIN recoleccion.condiciones_venta cv
      ON cv.codigo_condicion_venta = @codigo_condicion_venta
    JOIN recoleccion.usuarios_sistema us
      ON us.codigo_usuario_sistema = @codigo_usuario_sistema
    JOIN recoleccion.comisiones com
      ON com.codigo_comision = @codigo_comision
    WHERE o.rn = 1
      AND o.serie_normalizada IS NOT NULL
      AND o.categoria_normalizada IS NOT NULL
      AND STR_TO_DATE(NULLIF(TRIM(o.fechapago), 'None'), '%Y-%m-%d %H:%i') IS NOT NULL
)
SELECT * FROM candidatos;

-- Control previo. Las diferencias deben revisarse antes de confirmar la transaccion.
SELECT
    (SELECT COUNT(*) FROM recoleccion_migracion.comprobantes) AS filas_origen,
    COUNT(*) AS filas_validas,
    SUM(EXISTS (
        SELECT 1 FROM recoleccion.comprobantes c
         WHERE c.numero_comprobante = x.numero_comprobante
           AND c.codigo_punto_expedicion = x.codigo_punto_expedicion
           AND c.codigo_sucursal = x.codigo_sucursal
           AND c.codigo_serie = x.codigo_serie
           AND c.codigo_tipo_comprobante = x.codigo_tipo_comprobante
    )) AS filas_ya_existentes,
    COUNT(*) - SUM(EXISTS (
        SELECT 1 FROM recoleccion.comprobantes c
         WHERE c.numero_comprobante = x.numero_comprobante
           AND c.codigo_punto_expedicion = x.codigo_punto_expedicion
           AND c.codigo_sucursal = x.codigo_sucursal
           AND c.codigo_serie = x.codigo_serie
           AND c.codigo_tipo_comprobante = x.codigo_tipo_comprobante
    )) AS filas_por_insertar
FROM tmp_comprobantes_importar x;

START TRANSACTION;

INSERT INTO recoleccion.comprobantes (
    numero_comprobante, codigo_punto_expedicion, punto_expedicion_fiscal,
    codigo_sucursal, establecimiento_fiscal, codigo_serie, serie_fiscal,
    codigo_tipo_comprobante, codigo_timbrado, numero_timbrado_fiscal,
    inicio_vigencia_fiscal, fin_vigencia_fiscal, codigo_condicion_venta,
    cuenta_corriente, codigo_usuario, codigo_categoria, razon_social,
    fecha_emision, fecha_pago, cantidad_deuda, tarifa, cantidad_pago,
    recargo, saldo, total_importe, pago_hasta, periodo_pago,
    codigo_usuario_sistema, codigo_estado, obs, codigo_cobrador,
    codigo_comision
)
SELECT
    x.numero_comprobante, x.codigo_punto_expedicion, x.punto_expedicion_fiscal,
    x.codigo_sucursal, x.establecimiento_fiscal, x.codigo_serie, x.serie_fiscal,
    x.codigo_tipo_comprobante, x.codigo_timbrado, x.numero_timbrado_fiscal,
    x.inicio_vigencia_fiscal, x.fin_vigencia_fiscal, x.codigo_condicion_venta,
    x.cuenta_corriente, x.codigo_usuario, x.codigo_categoria, x.razon_social,
    x.fecha_emision, x.fecha_pago, x.cantidad_deuda, x.tarifa, x.cantidad_pago,
    x.recargo, x.saldo, x.total_importe, x.pago_hasta, x.periodo_pago,
    x.codigo_usuario_sistema, x.codigo_estado, x.obs, x.codigo_cobrador,
    x.codigo_comision
FROM tmp_comprobantes_importar x
WHERE NOT EXISTS (
    SELECT 1 FROM recoleccion.comprobantes c
     WHERE c.numero_comprobante = x.numero_comprobante
       AND c.codigo_punto_expedicion = x.codigo_punto_expedicion
       AND c.codigo_sucursal = x.codigo_sucursal
       AND c.codigo_serie = x.codigo_serie
       AND c.codigo_tipo_comprobante = x.codigo_tipo_comprobante
);

SELECT ROW_COUNT() AS comprobantes_insertados;

COMMIT;
