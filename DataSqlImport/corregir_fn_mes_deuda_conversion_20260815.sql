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
