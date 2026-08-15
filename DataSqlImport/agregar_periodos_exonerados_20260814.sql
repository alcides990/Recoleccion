SET NAMES utf8mb4;

ALTER TABLE historial_exoneraciones_servicio
    ADD COLUMN cantidad_periodos INT NOT NULL DEFAULT 0 AFTER fecha_desde_nueva;

UPDATE historial_exoneraciones_servicio
SET cantidad_periodos = GREATEST(
    TIMESTAMPDIFF(
        MONTH,
        DATE_FORMAT(fecha_desde_anterior, '%Y-%m-01'),
        DATE_FORMAT(fecha_desde_nueva, '%Y-%m-01')
    ),
    0
);
