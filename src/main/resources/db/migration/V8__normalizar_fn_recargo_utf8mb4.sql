DROP FUNCTION IF EXISTS fn_recargo;

DELIMITER $$

CREATE FUNCTION fn_recargo(
    cuenta_corriente_param VARCHAR(45)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    codigo_sucursal_param INT
)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE periodo_deuda INT DEFAULT 0;
    DECLARE tarifa DECIMAL(10, 2) DEFAULT 0;
    DECLARE recargo_total DECIMAL(10, 2) DEFAULT 0;
    DECLARE porcentaje_recargo INT DEFAULT 0;

    SELECT c.tarifa
      INTO tarifa
      FROM servicios s
      JOIN categorias c
        ON c.codigo_categoria = s.codigo_categoria
       AND c.codigo_sucursal = s.codigo_sucursal
     WHERE s.cuenta_corriente = cuenta_corriente_param
       AND s.codigo_sucursal = codigo_sucursal_param
     LIMIT 1;

    SELECT p.recargo_mora
      INTO porcentaje_recargo
      FROM parametros p
     WHERE p.codigo_sucursal = codigo_sucursal_param
     LIMIT 1;

    SET periodo_deuda = fn_mes_deuda(cuenta_corriente_param, codigo_sucursal_param);

    IF periodo_deuda > 2 THEN
        SET recargo_total = (periodo_deuda * tarifa) * (porcentaje_recargo / 100);
    END IF;

    RETURN COALESCE(recargo_total, 0);
END$$

DELIMITER ;
