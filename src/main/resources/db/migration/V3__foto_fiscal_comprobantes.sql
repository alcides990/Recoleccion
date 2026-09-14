-- Conserva en cada comprobante los datos fiscales tal como estaban al emitirlo.
-- Las claves foraneas se mantienen para navegacion e integridad referencial, pero
-- las impresiones deben utilizar primero esta copia historica.

ALTER TABLE comprobantes
    ADD COLUMN establecimiento_fiscal VARCHAR(20) NULL AFTER codigo_sucursal,
    ADD COLUMN punto_expedicion_fiscal VARCHAR(20) NULL AFTER codigo_punto_expedicion,
    ADD COLUMN numero_timbrado_fiscal VARCHAR(30) NULL AFTER codigo_timbrado,
    ADD COLUMN inicio_vigencia_fiscal DATE NULL AFTER numero_timbrado_fiscal,
    ADD COLUMN fin_vigencia_fiscal DATE NULL AFTER inicio_vigencia_fiscal,
    ADD COLUMN serie_fiscal VARCHAR(20) NULL AFTER codigo_serie;

-- Foto inicial para documentos historicos. Se dejan NULL solamente los valores
-- que no puedan recuperarse de los maestros actuales, evitando inventar datos fiscales.
UPDATE comprobantes c
LEFT JOIN sucursales s
       ON s.codigo_sucursal = c.codigo_sucursal
LEFT JOIN puntos_expedicion pe
       ON pe.codigo_sucursal = c.codigo_sucursal
      AND pe.codigo_punto_expedicion = c.codigo_punto_expedicion
LEFT JOIN timbrados t
       ON t.codigo_timbrado = c.codigo_timbrado
LEFT JOIN series se
       ON se.codigo_serie = c.codigo_serie
SET c.establecimiento_fiscal = NULLIF(TRIM(s.sucursal), ''),
    c.punto_expedicion_fiscal = NULLIF(TRIM(pe.punto_expedicion), ''),
    c.numero_timbrado_fiscal = CAST(t.numero_timbrado AS CHAR),
    c.inicio_vigencia_fiscal = t.fecha_inicio,
    c.fin_vigencia_fiscal = t.fecha_fin,
    c.serie_fiscal = NULLIF(TRIM(se.serie), '');
