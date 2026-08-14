INSERT INTO recoleccion.comprobantes (
  numero_comprobante,
  codigo_tipo_comprobante,
  codigo_serie,
  codigo_punto_expedicion,
  codigo_sucursal,
  codigo_timbrado,
  cuenta_corriente,
  razon_social,
  fecha_emision,
  fecha_pago,
  cantidad_deuda,
  tarifa,
  cantidad_pago,
  recargo,
  saldo,
  total_importe,
  pago_hasta,
  periodo_pago,
  codigo_condicion_venta,
  codigo_usuario,
  codigo_cobrador,
  codigo_usuario_sistema,
  codigo_estado,
  codigo_categoria,
  codigo_comision
)
SELECT DISTINCT
  CAST(NULLIF(o.nrorecibo, 'None') AS UNSIGNED)                         AS numero_comprobante,
  1                                                                      AS codigo_tipo_comprobante,
CASE o.tiporecibo
    WHEN 'A' THEN 1
    WHEN 'B' THEN 2
    WHEN 'C' THEN 3
    WHEN 'D' THEN 4
    WHEN 'G'  THEN 7
     WHEN ''  THEN 2
END AS codigo_serie,

  1                                                                      AS codigo_punto_expedicion,
  1                                                                      AS codigo_sucursal,
  1                                                                      AS codigo_timbrado,
  o.cuenta                                                               AS cuenta_corriente,
  o.nombre                                                               AS razon_social,
  STR_TO_DATE(NULLIF(o.fechapago, 'None'), '%Y-%m-%d %H:%i')              AS fecha_emision,
 STR_TO_DATE(NULLIF(o.fechapago, 'None'), '%Y-%m-%d %H:%i')                 AS fecha_pago,
  0                                                                      AS cantida_deuda,
  CASE
    WHEN COALESCE(CAST(NULLIF(o.cantidadperiodo, 'None') AS UNSIGNED), 0) > 0
      THEN CAST(NULLIF(o.importe, 'None') AS DECIMAL(12,2))
           / CAST(o.cantidadperiodo AS UNSIGNED)
    ELSE COALESCE(cat.tarifa, 0)
  END                                                                    AS tarifa,
  COALESCE(CAST(NULLIF(o.cantidadperiodo, 'None') AS UNSIGNED), 0)       AS cantidad_pago,
  0                                                                      AS recargo,
  0                                                                      AS saldo,
  CAST(NULLIF(o.importe, 'None') AS DECIMAL(12,2))                       AS total_importe,
  NULL                                                                   AS pago_hasta,
  NULL                                                                   AS periodo_pago,
  1                                                                      AS codigo_condicion_venta,
  1                                                                      AS codigo_usuario,
  COALESCE(CAST(NULLIF(o.codcob, 'None') AS UNSIGNED), 0)                AS codigo_cobrador,
  1                                                                      AS codigo_usuario_sistema,
  CASE
    WHEN COALESCE(CAST(NULLIF(o.cantidadperiodo, 'None') AS UNSIGNED), 0) = 0 THEN 3
    ELSE 1
  END                                                                    AS codigo_estado,
  case when catego ='A240' then 1
  when catego='FRI' then 2
  WHEN catego = '055' THEN 3
  ELSE catego
  end  AS codigo_categoria,
  1 AS codigo_comision -- La migración histórica tiene porcomnormal = 10%, código 1.
FROM recoleccion_migracion.comprobantes o
INNER JOIN recoleccion.servicios s
  ON s.cuenta_corriente = o.cuenta
INNER JOIN recoleccion.categorias cat
  ON cat.codigo_categoria = CASE
      WHEN o.catego = 'A240' THEN 1
      WHEN o.catego = 'FRI' THEN 2
      WHEN o.catego = '055' THEN 3
      ELSE CAST(o.catego AS UNSIGNED)
  END
WHERE NOT EXISTS (
  SELECT 1
  FROM recoleccion.comprobantes c
  WHERE c.numero_comprobante = CAST(NULLIF(o.nrorecibo, 'None') AS UNSIGNED)
) AND o.cuenta != 'None'
GROUP BY o.nrorecibo, o.tiporecibo;
