-- Normaliza el esquema completo de la aplicación para MySQL 8.0/8.4.
-- IMPORTANTE:
--   1. Realizar un respaldo antes de ejecutar este archivo.
--   2. Ejecutarlo sin usuarios conectados ni sincronizaciones en curso.
--   3. Este proceso puede tardar porque MySQL debe reconstruir tablas.
--   4. Solo modifica el esquema `recoleccion`; no toca esquemas del sistema.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

ALTER DATABASE `recoleccion`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

DROP PROCEDURE IF EXISTS `recoleccion`.`normalizar_utf8mb4_0900_ai_ci`;

DELIMITER $$

CREATE PROCEDURE `recoleccion`.`normalizar_utf8mb4_0900_ai_ci`()
BEGIN
    DECLARE proceso_finalizado BOOLEAN DEFAULT FALSE;
    DECLARE nombre_tabla VARCHAR(64);
    DECLARE verificacion_fk_anterior INT DEFAULT @@SESSION.FOREIGN_KEY_CHECKS;

    DECLARE tablas CURSOR FOR
        SELECT t.TABLE_NAME
          FROM information_schema.TABLES t
         WHERE t.TABLE_SCHEMA = 'recoleccion'
           AND t.TABLE_TYPE = 'BASE TABLE'
         ORDER BY t.TABLE_NAME;

    DECLARE CONTINUE HANDLER FOR NOT FOUND
        SET proceso_finalizado = TRUE;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET SESSION FOREIGN_KEY_CHECKS = verificacion_fk_anterior;
        RESIGNAL;
    END;

    -- MySQL permite convertir tablas relacionadas si se convierten ambos lados
    -- antes de volver a activar la comprobación de claves foráneas.
    SET SESSION FOREIGN_KEY_CHECKS = 0;

    OPEN tablas;

    convertir_tablas: LOOP
        FETCH tablas INTO nombre_tabla;
        IF proceso_finalizado THEN
            LEAVE convertir_tablas;
        END IF;

        SET @sql_normalizacion = CONCAT(
            'ALTER TABLE `recoleccion`.`',
            REPLACE(nombre_tabla, '`', '``'),
            '` CONVERT TO CHARACTER SET utf8mb4 ',
            'COLLATE utf8mb4_0900_ai_ci'
        );

        PREPARE sentencia_normalizacion FROM @sql_normalizacion;
        EXECUTE sentencia_normalizacion;
        DEALLOCATE PREPARE sentencia_normalizacion;
    END LOOP;

    CLOSE tablas;
    SET SESSION FOREIGN_KEY_CHECKS = verificacion_fk_anterior;
END$$

DELIMITER ;

CALL `recoleccion`.`normalizar_utf8mb4_0900_ai_ci`();
DROP PROCEDURE `recoleccion`.`normalizar_utf8mb4_0900_ai_ci`;

-- Resultado esperado: utf8mb4 / utf8mb4_0900_ai_ci.
SELECT s.SCHEMA_NAME,
       s.DEFAULT_CHARACTER_SET_NAME,
       s.DEFAULT_COLLATION_NAME
  FROM information_schema.SCHEMATA s
 WHERE s.SCHEMA_NAME = 'recoleccion';

-- Resultado esperado: cero filas.
SELECT t.TABLE_NAME,
       t.TABLE_COLLATION
  FROM information_schema.TABLES t
 WHERE t.TABLE_SCHEMA = 'recoleccion'
   AND t.TABLE_TYPE = 'BASE TABLE'
   AND t.TABLE_COLLATION <> 'utf8mb4_0900_ai_ci'
 ORDER BY t.TABLE_NAME;

-- Resultado esperado: cero filas. Se excluyen columnas binarias y numéricas,
-- que correctamente no poseen juego de caracteres ni cotejamiento.
SELECT c.TABLE_NAME,
       c.COLUMN_NAME,
       c.COLUMN_TYPE,
       c.CHARACTER_SET_NAME,
       c.COLLATION_NAME
  FROM information_schema.COLUMNS c
 WHERE c.TABLE_SCHEMA = 'recoleccion'
   AND c.CHARACTER_SET_NAME IS NOT NULL
   AND (c.CHARACTER_SET_NAME <> 'utf8mb4'
        OR c.COLLATION_NAME <> 'utf8mb4_0900_ai_ci')
 ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION;

-- Resultado esperado: cero filas. Detecta diferencias entre los dos lados
-- textuales de cualquier clave foránea.
SELECT k.TABLE_NAME AS tabla_hija,
       k.COLUMN_NAME AS columna_hija,
       hija.CHARACTER_SET_NAME AS charset_hija,
       hija.COLLATION_NAME AS collation_hija,
       k.REFERENCED_TABLE_NAME AS tabla_padre,
       k.REFERENCED_COLUMN_NAME AS columna_padre,
       padre.CHARACTER_SET_NAME AS charset_padre,
       padre.COLLATION_NAME AS collation_padre
  FROM information_schema.KEY_COLUMN_USAGE k
  JOIN information_schema.COLUMNS hija
    ON hija.TABLE_SCHEMA = k.TABLE_SCHEMA
   AND hija.TABLE_NAME = k.TABLE_NAME
   AND hija.COLUMN_NAME = k.COLUMN_NAME
  JOIN information_schema.COLUMNS padre
    ON padre.TABLE_SCHEMA = k.REFERENCED_TABLE_SCHEMA
   AND padre.TABLE_NAME = k.REFERENCED_TABLE_NAME
   AND padre.COLUMN_NAME = k.REFERENCED_COLUMN_NAME
 WHERE k.TABLE_SCHEMA = 'recoleccion'
   AND k.REFERENCED_TABLE_NAME IS NOT NULL
   AND hija.CHARACTER_SET_NAME IS NOT NULL
   AND (hija.CHARACTER_SET_NAME <> padre.CHARACTER_SET_NAME
        OR hija.COLLATION_NAME <> padre.COLLATION_NAME)
 ORDER BY k.TABLE_NAME, k.COLUMN_NAME;
