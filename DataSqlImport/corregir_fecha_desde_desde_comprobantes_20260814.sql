-- Corrige servicios.fecha_desde con el período pendiente real ya registrado.
-- Conserva fechas más avanzadas por exoneraciones y crea respaldo previo.
SET NAMES utf8mb4;

DROP TABLE IF EXISTS backup_servicios_fecha_desde_20260814;
CREATE TABLE backup_servicios_fecha_desde_20260814 AS
SELECT cuenta_corriente, fecha_inicio, fecha_desde
FROM servicios;

ALTER TABLE backup_servicios_fecha_desde_20260814
    ADD PRIMARY KEY (cuenta_corriente);

UPDATE servicios s
LEFT JOIN (
    SELECT c.cuenta_corriente, MAX(c.pago_hasta) AS ultimo_pago_hasta
    FROM comprobantes c
    WHERE c.codigo_estado = 1
      AND c.pago_hasta IS NOT NULL
    GROUP BY c.cuenta_corriente
) p ON p.cuenta_corriente = s.cuenta_corriente
SET s.fecha_desde = GREATEST(
    COALESCE(s.fecha_desde, s.fecha_inicio),
    COALESCE(p.ultimo_pago_hasta, s.fecha_inicio)
);

SOURCE DataSqlImport/actualizar_funciones_fecha_desde_20260814.sql;

SELECT ROW_COUNT() AS servicios_revisados;
