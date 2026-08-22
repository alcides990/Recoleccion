-- Migra la cadena activa de cuenta_corriente a utf8mb4 en MySQL 8.
-- Ejecutar seleccionando previamente la base de datos correspondiente.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE()
              AND TABLE_NAME = 'comprobantes'
              AND CONSTRAINT_NAME = 'fk_Comprobantes_Servicios1'),
    'ALTER TABLE comprobantes DROP FOREIGN KEY fk_Comprobantes_Servicios1',
    'DO 0');
PREPARE sentencia FROM @ddl;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE()
              AND TABLE_NAME = 'historial_exoneraciones_servicio'
              AND CONSTRAINT_NAME = 'fk_exoneracion_servicio'),
    'ALTER TABLE historial_exoneraciones_servicio DROP FOREIGN KEY fk_exoneracion_servicio',
    'DO 0');
PREPARE sentencia FROM @ddl;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE()
              AND TABLE_NAME = 'historial_suspensiones_servicio'
              AND CONSTRAINT_NAME = 'fk_suspension_servicio'),
    'ALTER TABLE historial_suspensiones_servicio DROP FOREIGN KEY fk_suspension_servicio',
    'DO 0');
PREPARE sentencia FROM @ddl;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;

ALTER TABLE servicios
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    MODIFY cuenta_corriente VARCHAR(45)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

ALTER TABLE comprobantes
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    MODIFY cuenta_corriente VARCHAR(45)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

ALTER TABLE historial_exoneraciones_servicio
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    MODIFY cuenta_corriente VARCHAR(45)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

ALTER TABLE historial_suspensiones_servicio
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
    MODIFY cuenta_corriente VARCHAR(45)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

-- Normaliza espacios heredados antes de validar nuevamente las relaciones.
UPDATE servicios
   SET cuenta_corriente = TRIM(cuenta_corriente)
 WHERE cuenta_corriente <> TRIM(cuenta_corriente);
UPDATE comprobantes
   SET cuenta_corriente = TRIM(cuenta_corriente)
 WHERE cuenta_corriente <> TRIM(cuenta_corriente);
UPDATE historial_exoneraciones_servicio
   SET cuenta_corriente = TRIM(cuenta_corriente)
 WHERE cuenta_corriente <> TRIM(cuenta_corriente);
UPDATE historial_suspensiones_servicio
   SET cuenta_corriente = TRIM(cuenta_corriente)
 WHERE cuenta_corriente <> TRIM(cuenta_corriente);

ALTER TABLE comprobantes
    ADD CONSTRAINT fk_Comprobantes_Servicios1
        FOREIGN KEY (cuenta_corriente)
        REFERENCES servicios (cuenta_corriente);
ALTER TABLE historial_exoneraciones_servicio
    ADD CONSTRAINT fk_exoneracion_servicio
        FOREIGN KEY (cuenta_corriente)
        REFERENCES servicios (cuenta_corriente);
ALTER TABLE historial_suspensiones_servicio
    ADD CONSTRAINT fk_suspension_servicio
        FOREIGN KEY (cuenta_corriente)
        REFERENCES servicios (cuenta_corriente);

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

SELECT TABLE_NAME,
       COLUMN_NAME,
       COLUMN_TYPE,
       CHARACTER_SET_NAME,
       COLLATION_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME IN (
       'servicios',
       'comprobantes',
       'historial_exoneraciones_servicio',
       'historial_suspensiones_servicio')
   AND COLUMN_NAME = 'cuenta_corriente'
 ORDER BY TABLE_NAME;
