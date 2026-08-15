-- Sincroniza el primer periodo pendiente con el ultimo pago activo.
-- No retrocede fechas adelantadas por exoneraciones u otros ajustes.
SET NAMES utf8mb4;

DROP TABLE IF EXISTS backup_servicios_fecha_desde_pagos_20260814;
CREATE TABLE backup_servicios_fecha_desde_pagos_20260814 AS
SELECT s.*
FROM servicios s
JOIN (
    SELECT cuenta_corriente, MAX(pago_hasta) AS ultimo_pago_hasta
    FROM comprobantes
    WHERE codigo_estado = 1
      AND pago_hasta IS NOT NULL
    GROUP BY cuenta_corriente
) p ON p.cuenta_corriente = s.cuenta_corriente
WHERE p.ultimo_pago_hasta > s.fecha_desde;

ALTER TABLE backup_servicios_fecha_desde_pagos_20260814
  ADD PRIMARY KEY (cuenta_corriente);

START TRANSACTION;

UPDATE servicios s
JOIN (
    SELECT cuenta_corriente, MAX(pago_hasta) AS ultimo_pago_hasta
    FROM comprobantes
    WHERE codigo_estado = 1
      AND pago_hasta IS NOT NULL
    GROUP BY cuenta_corriente
) p ON p.cuenta_corriente = s.cuenta_corriente
SET s.fecha_desde = p.ultimo_pago_hasta
WHERE p.ultimo_pago_hasta > s.fecha_desde;

SELECT ROW_COUNT() AS servicios_actualizados;
COMMIT;

SELECT COUNT(*) AS servicios_respaldados
FROM backup_servicios_fecha_desde_pagos_20260814;
