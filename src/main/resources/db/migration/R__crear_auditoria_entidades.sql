CREATE TABLE IF NOT EXISTS auditoria_entidades (
    codigo_auditoria BIGINT NOT NULL AUTO_INCREMENT,
    entidad VARCHAR(20) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    identificador VARCHAR(100) NOT NULL,
    codigo_sucursal INT NOT NULL,
    codigo_usuario_sistema INT NULL,
    fecha_evento DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    datos_antes JSON NULL,
    datos_despues JSON NULL,
    motivo VARCHAR(500) NULL,
    PRIMARY KEY (codigo_auditoria),
    INDEX idx_auditoria_entidad_sucursal_fecha
        (codigo_sucursal, entidad, fecha_evento, codigo_auditoria),
    INDEX idx_auditoria_entidad_registro
        (entidad, identificador, fecha_evento, codigo_auditoria),
    INDEX idx_auditoria_entidad_usuario
        (codigo_usuario_sistema, fecha_evento),
    CONSTRAINT chk_auditoria_entidad
        CHECK (entidad IN ('USUARIO', 'CUENTA', 'CATEGORIA', 'USUARIO_SISTEMA')),
    CONSTRAINT chk_auditoria_entidad_accion
        CHECK (accion IN ('ALTA', 'MODIFICACION', 'ELIMINACION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @existe_check_entidad = (
    SELECT COUNT(*)
      FROM information_schema.TABLE_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = DATABASE()
       AND TABLE_NAME = 'auditoria_entidades'
       AND CONSTRAINT_NAME = 'chk_auditoria_entidad'
       AND CONSTRAINT_TYPE = 'CHECK'
);
SET @sql = IF(@existe_check_entidad = 0, 'SELECT 1',
    'ALTER TABLE auditoria_entidades DROP CHECK chk_auditoria_entidad');
PREPARE sentencia FROM @sql;
EXECUTE sentencia;
DEALLOCATE PREPARE sentencia;

ALTER TABLE auditoria_entidades
    ADD CONSTRAINT chk_auditoria_entidad
    CHECK (entidad IN ('USUARIO', 'CUENTA', 'CATEGORIA', 'USUARIO_SISTEMA'));
