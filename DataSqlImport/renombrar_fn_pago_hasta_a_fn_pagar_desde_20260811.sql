-- La función devuelve el primer período pendiente; semánticamente es "pagar desde".
DROP FUNCTION IF EXISTS fn_pagar_desde;

DELIMITER $$
CREATE FUNCTION fn_pagar_desde(cuenta_corriente_param VARCHAR(20))
RETURNS VARCHAR(7)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE pagar_desde DATE;
    DECLARE periodo_inicio DATE;
    DECLARE total_meses INT DEFAULT 0;

    SELECT fecha_inicio
      INTO periodo_inicio
      FROM servicios
     WHERE cuenta_corriente = cuenta_corriente_param
     LIMIT 1;

    SELECT IFNULL(SUM(cantidad_pago), 0)
      INTO total_meses
      FROM comprobantes
     WHERE cuenta_corriente = cuenta_corriente_param
       AND codigo_estado = 1;

    SET pagar_desde = DATE_ADD(periodo_inicio, INTERVAL total_meses MONTH);
    RETURN DATE_FORMAT(pagar_desde, '%m-%Y');
END$$
DELIMITER ;

-- Ejecutar este DROP una vez desplegados el código Java y los reportes Jasper actualizados.
DROP FUNCTION IF EXISTS fn_pago_hasta;
