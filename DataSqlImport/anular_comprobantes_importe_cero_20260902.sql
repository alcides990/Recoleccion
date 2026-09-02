-- Corrige tres comprobantes históricos activos con importe cero.
-- Estado 3 = ANULADO.

START TRANSACTION;

-- Verificación previa: deben aparecer exactamente los tres registros esperados.
SELECT numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
       codigo_serie, codigo_tipo_comprobante, cuenta_corriente,
       cantidad_pago, total_importe, codigo_estado
  FROM comprobantes
 WHERE (numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
        codigo_serie, codigo_tipo_comprobante) IN (
           (81072, 1, 1, 2, 1),
           (36500, 1, 1, 2, 1),
           (24321, 1, 1, 2, 1)
       )
 FOR UPDATE;

UPDATE comprobantes
   SET codigo_estado = 3,
       cantidad_pago = 0
 WHERE codigo_estado = 1
   AND total_importe = 0
   AND (
       (numero_comprobante = 81072 AND cuenta_corriente = '31-0216-10')
       OR
       (numero_comprobante IN (36500, 24321) AND cuenta_corriente = '31-0520-14')
   )
   AND codigo_punto_expedicion = 1
   AND codigo_sucursal = 1
   AND codigo_serie = 2
   AND codigo_tipo_comprobante = 1;

SELECT ROW_COUNT() AS comprobantes_actualizados;

-- Verificación posterior: los tres deben quedar ANULADOS y con cantidad_pago 0.
SELECT numero_comprobante, cuenta_corriente, cantidad_pago,
       total_importe, codigo_estado
  FROM comprobantes
 WHERE (numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
        codigo_serie, codigo_tipo_comprobante) IN (
           (81072, 1, 1, 2, 1),
           (36500, 1, 1, 2, 1),
           (24321, 1, 1, 2, 1)
       )
 ORDER BY numero_comprobante;

COMMIT;
