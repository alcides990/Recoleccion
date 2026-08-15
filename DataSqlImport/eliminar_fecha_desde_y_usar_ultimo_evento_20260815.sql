-- El período pendiente se deriva del último pago activo o exoneración registrada.
-- servicios.fecha_desde deja de ser una fuente de estado duplicada.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS backup_servicios_fecha_desde_20260815 AS
SELECT cuenta_corriente, fecha_desde
FROM servicios;

ALTER TABLE historial_exoneraciones_servicio
    MODIFY fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

DROP FUNCTION IF EXISTS fn_pagar_desde;
DELIMITER $$
CREATE FUNCTION fn_pagar_desde(cuenta_corriente_param VARCHAR(30))
RETURNS VARCHAR(7) CHARSET utf8mb4
READS SQL DATA
NOT DETERMINISTIC
BEGIN
    DECLARE periodo_pendiente DATE;

    SELECT evento.periodo
      INTO periodo_pendiente
      FROM (
            SELECT c.pago_hasta AS periodo,
                   c.fecha_emision AS fecha_registro,
                   1 AS prioridad,
                   c.numero_comprobante AS secuencia
              FROM comprobantes c
             WHERE c.cuenta_corriente = cuenta_corriente_param
               AND c.codigo_estado = 1
               AND c.pago_hasta IS NOT NULL
               AND c.fecha_emision IS NOT NULL
            UNION ALL
            SELECT he.fecha_desde_nueva,
                   he.fecha_registro,
                   2,
                   he.codigo_exoneracion
              FROM historial_exoneraciones_servicio he
             WHERE he.cuenta_corriente = cuenta_corriente_param
      ) evento
     ORDER BY evento.fecha_registro DESC,
              evento.prioridad DESC,
              evento.secuencia DESC
     LIMIT 1;

    IF periodo_pendiente IS NULL THEN
        SELECT s.fecha_inicio
          INTO periodo_pendiente
          FROM servicios s
         WHERE s.cuenta_corriente = cuenta_corriente_param
         LIMIT 1;
    END IF;

    RETURN DATE_FORMAT(periodo_pendiente, '%m-%Y');
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_mes_deuda;
DELIMITER $$
CREATE FUNCTION fn_mes_deuda(cuenta_corriente_param VARCHAR(30), codigo_sucursal_param INT)
RETURNS INT
READS SQL DATA
NOT DETERMINISTIC
BEGIN
    DECLARE periodo_pendiente DATE;
    DECLARE fecha_cierre DATE;

    SET periodo_pendiente = STR_TO_DATE(
        CONCAT(fn_pagar_desde(cuenta_corriente_param), '-01'), '%m-%Y-%d');

    SELECT p.cierre_periodo
      INTO fecha_cierre
      FROM parametros p
     WHERE p.codigo_sucursal = codigo_sucursal_param
     LIMIT 1;

    RETURN TIMESTAMPDIFF(
        MONTH, periodo_pendiente, DATE_ADD(fecha_cierre, INTERVAL 5 DAY));
END$$
DELIMITER ;

ALTER TABLE servicios DROP COLUMN fecha_desde;

SELECT fn_pagar_desde(s.cuenta_corriente) AS pago_desde,
       fn_mes_deuda(s.cuenta_corriente, s.codigo_sucursal) AS meses_deuda
FROM servicios s
LIMIT 10;
