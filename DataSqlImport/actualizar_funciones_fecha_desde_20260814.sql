SET NAMES utf8mb4;

DROP FUNCTION IF EXISTS fn_pagar_desde;
DELIMITER $$
CREATE FUNCTION fn_pagar_desde(cuenta_corriente_param VARCHAR(30))
RETURNS VARCHAR(7) CHARSET utf8mb4
READS SQL DATA
NOT DETERMINISTIC
BEGIN
    DECLARE periodo_inicio DATE;
    SELECT GREATEST(
               s.fecha_desde,
               COALESCE((
                   SELECT MAX(c.pago_hasta)
                     FROM comprobantes c
                    WHERE c.cuenta_corriente = s.cuenta_corriente
                      AND c.codigo_estado = 1
               ), s.fecha_desde)
           )
      INTO periodo_inicio
      FROM servicios s
     WHERE s.cuenta_corriente = cuenta_corriente_param
     LIMIT 1;

    RETURN DATE_FORMAT(periodo_inicio, '%m-%Y');
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_mes_deuda;
DELIMITER $$
CREATE FUNCTION fn_mes_deuda(cuent_corriente VARCHAR(30), cod_sucursal INT)
RETURNS INT
READS SQL DATA
NOT DETERMINISTIC
BEGIN
    DECLARE periodo_inicio DATE;
    DECLARE fecha_cierre DATE;
    DECLARE periodo_deuda INT;

    SELECT GREATEST(
               s.fecha_desde,
               COALESCE((
                   SELECT MAX(c.pago_hasta)
                     FROM comprobantes c
                    WHERE c.cuenta_corriente = s.cuenta_corriente
                      AND c.codigo_estado = 1
               ), s.fecha_desde)
           )
      INTO periodo_inicio
      FROM servicios s
     WHERE s.cuenta_corriente = cuent_corriente
     LIMIT 1;

    SELECT p.cierre_periodo
      INTO fecha_cierre
      FROM parametros p
     WHERE p.codigo_sucursal = cod_sucursal
     LIMIT 1;

    SET periodo_deuda = TIMESTAMPDIFF(MONTH, periodo_inicio, DATE_ADD(fecha_cierre, INTERVAL 5 DAY));

    -- Los valores negativos representan períodos pagados por adelantado.
    RETURN periodo_deuda;
END$$
DELIMITER ;

SELECT fn_pagar_desde(cuenta_corriente) AS pago_desde,
       fn_mes_deuda(cuenta_corriente, codigo_sucursal) AS meses_deuda
FROM servicios
ORDER BY cuenta_corriente
LIMIT 5;
