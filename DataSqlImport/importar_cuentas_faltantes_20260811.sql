SET NAMES utf8mb4;
START TRANSACTION;

SET @codigo_laura = (SELECT COALESCE(MAX(codigo_usuario), 0) + 1 FROM usuarios);
SET @codigo_paola = @codigo_laura + 1;

INSERT INTO usuarios (
    codigo_usuario, codigo_tipo_documento, numero_documento, nombre, apellido,
    celular, telefono, barrio, direccion, observacion, codigo_sucursal,
    codigo_estado, cuenta_temporal
) VALUES
(@codigo_laura, 1, '0', 'LAURA GIMÉNEZ', '', '0', NULL, '', 'CENTRO', '', 1, 1, '31-0062-02'),
(@codigo_paola, 1, '3202492', 'PAOLA ANDREA SIFUENTES', '', '0981172043', NULL, '', 'BUEN VISTA', '', 1, 1, '31-0520-07');

INSERT INTO servicios (
    cuenta_corriente, direccion, fecha_inicio, codigo_usuario, codigo_categoria,
    codigo_sucursal, codigo_manzana, codigo_estado, observacion
) VALUES
('31-0062-02', 'CENTRO', '2026-07-01', @codigo_laura, 58, 1, 62, 1, ''),
('31-0520-07', 'BUEN VISTA', '2026-08-01', @codigo_paola, 71, 1, 520, 1, '');

INSERT INTO comprobantes (
    numero_comprobante, codigo_tipo_comprobante, codigo_serie,
    codigo_punto_expedicion, codigo_sucursal, codigo_timbrado,
    cuenta_corriente, razon_social, fecha_emision, fecha_pago,
    cantidad_deuda, tarifa, cantidad_pago, recargo, saldo, total_importe,
    pago_hasta, periodo_pago, codigo_condicion_venta, codigo_usuario,
    codigo_cobrador, codigo_usuario_sistema, codigo_estado, codigo_categoria,
    codigo_comision
)
SELECT
    CAST(o.nrorecibo AS UNSIGNED),
    1,
    CASE o.tiporecibo
        WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3
        WHEN 'D' THEN 4 WHEN 'G' THEN 7 ELSE 2
    END,
    1, 1, 1,
    o.cuenta,
    TRIM(o.nombre),
    STR_TO_DATE(o.fechapago, '%Y-%m-%d'),
    STR_TO_DATE(o.fechapago, '%Y-%m-%d'),
    0,
    CAST(o.importe AS DECIMAL(12,2)) / CAST(o.cantidadperiodo AS UNSIGNED),
    CAST(o.cantidadperiodo AS UNSIGNED),
    0, 0,
    CAST(o.importe AS DECIMAL(12,2)),
    DATE_ADD(s.fecha_inicio, INTERVAL CAST(o.cantidadperiodo AS UNSIGNED) MONTH),
    CASE
        WHEN CAST(o.cantidadperiodo AS UNSIGNED) = 1
            THEN DATE_FORMAT(s.fecha_inicio, '%m-%Y')
        ELSE CONCAT(
            DATE_FORMAT(s.fecha_inicio, '%m-%Y'), ' / ',
            DATE_FORMAT(
                DATE_ADD(s.fecha_inicio, INTERVAL (CAST(o.cantidadperiodo AS UNSIGNED) - 1) MONTH),
                '%m-%Y'
            )
        )
    END,
    1,
    s.codigo_usuario,
    CAST(o.codcob AS UNSIGNED),
    1,
    CASE WHEN CAST(o.cantidadperiodo AS UNSIGNED) = 0 THEN 3 ELSE 1 END,
    CAST(o.catego AS UNSIGNED),
    1
FROM recoleccion_migracion.comprobantes o
JOIN servicios s ON s.cuenta_corriente = o.cuenta
WHERE o.cuenta IN ('31-0062-02', '31-0520-07')
  AND NOT EXISTS (
      SELECT 1
      FROM comprobantes c
      WHERE c.numero_comprobante = CAST(o.nrorecibo AS UNSIGNED)
        AND c.codigo_serie = CASE o.tiporecibo
            WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3
            WHEN 'D' THEN 4 WHEN 'G' THEN 7 ELSE 2
        END
        AND c.codigo_punto_expedicion = 1
        AND c.codigo_sucursal = 1
        AND c.codigo_tipo_comprobante = 1
  );

COMMIT;
