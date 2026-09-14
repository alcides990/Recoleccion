SET @existe_check_entidad = (
    SELECT COUNT(*)
      FROM information_schema.TABLE_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND TABLE_NAME = 'auditoria_entidades'
       AND CONSTRAINT_NAME = 'chk_auditoria_entidad'
       AND CONSTRAINT_TYPE = 'CHECK'
);

SET @sql_quitar_check = IF(
    @existe_check_entidad = 1,
    'ALTER TABLE auditoria_entidades DROP CHECK chk_auditoria_entidad',
    'SELECT 1'
);
PREPARE sentencia FROM @sql_quitar_check;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;

ALTER TABLE auditoria_entidades
    ADD CONSTRAINT chk_auditoria_entidad
    CHECK (entidad IN ('USUARIO', 'CUENTA', 'CATEGORIA', 'USUARIO_SISTEMA'));
