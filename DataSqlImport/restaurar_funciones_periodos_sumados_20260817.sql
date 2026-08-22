-- Restaura el cálculo simple basado en la fecha de inicio y períodos acumulados.
-- Primer período pendiente = fecha_inicio + pagos activos + exoneraciones.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Un único índice cubre la suma de períodos y la búsqueda del último pago.
-- La creación es idempotente para poder ejecutar nuevamente este script.
SET @indice_existe = (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'comprobantes'
       AND index_name = 'idx_comprobante_cuenta_estado_emision'
);
SET @crear_indice = IF(
    @indice_existe = 0,
    'CREATE INDEX idx_comprobante_cuenta_estado_emision ON comprobantes '
    '(cuenta_corriente, codigo_estado, fecha_emision DESC, numero_comprobante DESC, fecha_pago, cantidad_pago)',
    'DO 0'
);
PREPARE sentencia_indice FROM @crear_indice;
EXECUTE sentencia_indice;
DEALLOCATE PREPARE sentencia_indice;

DROP FUNCTION IF EXISTS fn_pagar_desde;
DELIMITER $$
CREATE FUNCTION fn_pagar_desde(
    cuenta_corriente_param VARCHAR(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci)
RETURNS VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
READS SQL DATA
NOT DETERMINISTIC
BEGIN
    DECLARE periodo_inicio DATE;
    DECLARE periodos_pagados INT DEFAULT 0;
    DECLARE periodos_exonerados INT DEFAULT 0;

    SELECT s.fecha_inicio
      INTO periodo_inicio
      FROM servicios s
     WHERE s.cuenta_corriente = cuenta_corriente_param
     LIMIT 1;

    SELECT COALESCE(SUM(c.cantidad_pago), 0)
      INTO periodos_pagados
      FROM comprobantes c
     WHERE c.cuenta_corriente = cuenta_corriente_param
       AND c.codigo_estado = 1;

    SELECT COALESCE(SUM(he.cantidad_periodos), 0)
      INTO periodos_exonerados
      FROM historial_exoneraciones_servicio he
     WHERE he.cuenta_corriente = cuenta_corriente_param;

    RETURN DATE_FORMAT(
        DATE_ADD(periodo_inicio,
                 INTERVAL (periodos_pagados + periodos_exonerados) MONTH),
        '%m-%Y');
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_mes_deuda;
DELIMITER $$
CREATE FUNCTION fn_mes_deuda(
    cuenta_corriente_param VARCHAR(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    codigo_sucursal_param INT)
RETURNS INT
READS SQL DATA
NOT DETERMINISTIC
BEGIN
    DECLARE periodo_inicio DATE;
    DECLARE fecha_cierre DATE;
    DECLARE periodos_pagados INT DEFAULT 0;
    DECLARE periodos_exonerados INT DEFAULT 0;

    SELECT s.fecha_inicio
      INTO periodo_inicio
      FROM servicios s
     WHERE s.cuenta_corriente = cuenta_corriente_param
     LIMIT 1;

    SELECT COALESCE(SUM(c.cantidad_pago), 0)
      INTO periodos_pagados
      FROM comprobantes c
     WHERE c.cuenta_corriente = cuenta_corriente_param
       AND c.codigo_estado = 1;

    SELECT COALESCE(SUM(he.cantidad_periodos), 0)
      INTO periodos_exonerados
      FROM historial_exoneraciones_servicio he
     WHERE he.cuenta_corriente = cuenta_corriente_param;

    SELECT p.cierre_periodo
      INTO fecha_cierre
      FROM parametros p
     WHERE p.codigo_sucursal = codigo_sucursal_param
     LIMIT 1;

    RETURN TIMESTAMPDIFF(
        MONTH,
        DATE_ADD(periodo_inicio,
                 INTERVAL (periodos_pagados + periodos_exonerados) MONTH),
        DATE_ADD(fecha_cierre, INTERVAL 5 DAY));
END$$
DELIMITER ;

DROP FUNCTION IF EXISTS fn_ultimo_pago;
DELIMITER $$
CREATE FUNCTION fn_ultimo_pago(
    cuenta_corriente_param VARCHAR(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci)
RETURNS DATE
READS SQL DATA
NOT DETERMINISTIC
BEGIN
    DECLARE fecha_ultimo_pago DATE DEFAULT NULL;

    SELECT c.fecha_pago
      INTO fecha_ultimo_pago
      FROM comprobantes c
     WHERE c.cuenta_corriente = cuenta_corriente_param
       AND c.codigo_estado = 1
     ORDER BY c.fecha_emision DESC,
              c.numero_comprobante DESC
     LIMIT 1;

    RETURN fecha_ultimo_pago;
END$$
DELIMITER ;

SELECT s.cuenta_corriente,
       s.fecha_inicio,
       fn_pagar_desde(s.cuenta_corriente) AS periodo_desde,
       fn_mes_deuda(s.cuenta_corriente, s.codigo_sucursal) AS periodos_pendientes
FROM servicios s
ORDER BY s.cuenta_corriente
LIMIT 10;
