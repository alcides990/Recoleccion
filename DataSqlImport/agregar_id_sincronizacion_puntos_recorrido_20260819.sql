-- Evita duplicar puntos cuando la aplicación móvil reintenta una sincronización.
-- Ejecutar una vez en cada esquema que ya tenga puntos_recorrido_cobrador.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @agregar_columna = IF(
    EXISTS(
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'puntos_recorrido_cobrador'
           AND COLUMN_NAME = 'id_sincronizacion'
    ),
    'DO 0',
    'ALTER TABLE puntos_recorrido_cobrador
         ADD COLUMN id_sincronizacion CHAR(36)
             CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL
             AFTER codigo_punto'
);
PREPARE sentencia FROM @agregar_columna;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;

UPDATE puntos_recorrido_cobrador
   SET id_sincronizacion = LOWER(UUID())
 WHERE id_sincronizacion IS NULL OR id_sincronizacion = '';

ALTER TABLE puntos_recorrido_cobrador
    MODIFY id_sincronizacion CHAR(36)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

SET @agregar_indice = IF(
    EXISTS(
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'puntos_recorrido_cobrador'
           AND INDEX_NAME = 'uq_punto_id_sincronizacion'
    ),
    'DO 0',
    'ALTER TABLE puntos_recorrido_cobrador
         ADD UNIQUE INDEX uq_punto_id_sincronizacion (id_sincronizacion)'
);
PREPARE sentencia FROM @agregar_indice;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;

SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLLATION_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'puntos_recorrido_cobrador'
   AND COLUMN_NAME = 'id_sincronizacion';
