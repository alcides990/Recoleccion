-- Ejecutar una sola vez, despues de verificar con EXPLAIN la carga real.
-- Los nombres pueden ajustarse si ya existen indices equivalentes.
CREATE INDEX ix_comprobantes_sucursal_fecha_numero
    ON comprobantes (codigo_sucursal, fecha_pago, numero_comprobante);

CREATE INDEX ix_comprobantes_sucursal_punto_serie_numero
    ON comprobantes (codigo_sucursal, codigo_punto_expedicion, codigo_serie, numero_comprobante);

CREATE INDEX ix_comprobantes_cuenta_fecha
    ON comprobantes (cuenta_corriente, fecha_pago);
