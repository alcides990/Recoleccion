-- Esquema de geolocalizacion de servicios y seguimiento de cobradores.
-- Esta migracion tambien admite instalaciones donde parte del esquema fue
-- creada previamente por los scripts manuales de agosto de 2026.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ubicaciones_servicio (
    cuenta_corriente VARCHAR(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    latitud DECIMAL(10,7) NOT NULL,
    longitud DECIMAL(10,7) NOT NULL,
    precision_metros DECIMAL(10,2) NULL,
    metodo VARCHAR(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    origen VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    codigo_usuario_sistema INT NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (cuenta_corriente),
    INDEX idx_ubicacion_usuario (codigo_usuario_sistema),
    CONSTRAINT chk_ubicacion_latitud CHECK (latitud BETWEEN -90 AND 90),
    CONSTRAINT chk_ubicacion_longitud CHECK (longitud BETWEEN -180 AND 180),
    CONSTRAINT chk_ubicacion_precision CHECK (precision_metros IS NULL OR precision_metros >= 0),
    CONSTRAINT chk_ubicacion_metodo CHECK (metodo IN ('GPS', 'MANUAL', 'MAPA')),
    CONSTRAINT chk_ubicacion_origen CHECK (origen IN ('WEB', 'APP')),
    CONSTRAINT fk_ubicacion_servicio
        FOREIGN KEY (cuenta_corriente) REFERENCES servicios (cuenta_corriente),
    CONSTRAINT fk_ubicacion_usuario_sistema
        FOREIGN KEY (codigo_usuario_sistema)
        REFERENCES usuarios_sistema (codigo_usuario_sistema)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS historial_ubicaciones_servicio (
    codigo_historial BIGINT NOT NULL AUTO_INCREMENT,
    cuenta_corriente VARCHAR(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    latitud_anterior DECIMAL(10,7) NULL,
    longitud_anterior DECIMAL(10,7) NULL,
    latitud_nueva DECIMAL(10,7) NOT NULL,
    longitud_nueva DECIMAL(10,7) NOT NULL,
    precision_anterior_metros DECIMAL(10,2) NULL,
    precision_nueva_metros DECIMAL(10,2) NULL,
    metodo VARCHAR(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    origen VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    codigo_usuario_sistema INT NOT NULL,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (codigo_historial),
    INDEX idx_historial_ubicacion_cuenta
        (cuenta_corriente, fecha_modificacion DESC, codigo_historial DESC),
    INDEX idx_historial_ubicacion_usuario (codigo_usuario_sistema),
    CONSTRAINT chk_historial_ubicacion_latitud_anterior
        CHECK (latitud_anterior IS NULL OR latitud_anterior BETWEEN -90 AND 90),
    CONSTRAINT chk_historial_ubicacion_longitud_anterior
        CHECK (longitud_anterior IS NULL OR longitud_anterior BETWEEN -180 AND 180),
    CONSTRAINT chk_historial_ubicacion_latitud_nueva CHECK (latitud_nueva BETWEEN -90 AND 90),
    CONSTRAINT chk_historial_ubicacion_longitud_nueva CHECK (longitud_nueva BETWEEN -180 AND 180),
    CONSTRAINT chk_historial_ubicacion_precision_anterior
        CHECK (precision_anterior_metros IS NULL OR precision_anterior_metros >= 0),
    CONSTRAINT chk_historial_ubicacion_precision_nueva
        CHECK (precision_nueva_metros IS NULL OR precision_nueva_metros >= 0),
    CONSTRAINT chk_historial_ubicacion_metodo CHECK (metodo IN ('GPS', 'MANUAL', 'MAPA')),
    CONSTRAINT chk_historial_ubicacion_origen CHECK (origen IN ('WEB', 'APP')),
    CONSTRAINT fk_historial_ubicacion_servicio
        FOREIGN KEY (cuenta_corriente) REFERENCES servicios (cuenta_corriente),
    CONSTRAINT fk_historial_ubicacion_usuario_sistema
        FOREIGN KEY (codigo_usuario_sistema)
        REFERENCES usuarios_sistema (codigo_usuario_sistema)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS recorridos_cobrador (
    codigo_recorrido BIGINT NOT NULL AUTO_INCREMENT,
    codigo_cobrador INT NOT NULL,
    estado VARCHAR(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDIENTE',
    observacion VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio DATETIME NULL,
    fecha_fin DATETIME NULL,
    codigo_usuario_registro INT NOT NULL,
    codigo_usuario_inicio INT NULL,
    codigo_usuario_fin INT NULL,
    origen_registro VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    origen_inicio VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    origen_fin VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    PRIMARY KEY (codigo_recorrido),
    INDEX idx_recorrido_cobrador_estado (codigo_cobrador, estado, fecha_registro DESC),
    INDEX idx_recorrido_estado_inicio (estado, fecha_inicio DESC),
    CONSTRAINT chk_recorrido_estado CHECK (estado IN ('PENDIENTE', 'ACTIVO', 'FINALIZADO')),
    CONSTRAINT chk_recorrido_origen_registro CHECK (origen_registro IN ('WEB', 'APP')),
    CONSTRAINT chk_recorrido_origen_inicio
        CHECK (origen_inicio IS NULL OR origen_inicio IN ('WEB', 'APP')),
    CONSTRAINT chk_recorrido_origen_fin
        CHECK (origen_fin IS NULL OR origen_fin IN ('WEB', 'APP')),
    CONSTRAINT fk_recorrido_cobrador
        FOREIGN KEY (codigo_cobrador) REFERENCES cobradores (codigo_cobrador),
    CONSTRAINT fk_recorrido_usuario_registro
        FOREIGN KEY (codigo_usuario_registro) REFERENCES usuarios_sistema (codigo_usuario_sistema),
    CONSTRAINT fk_recorrido_usuario_inicio
        FOREIGN KEY (codigo_usuario_inicio) REFERENCES usuarios_sistema (codigo_usuario_sistema),
    CONSTRAINT fk_recorrido_usuario_fin
        FOREIGN KEY (codigo_usuario_fin) REFERENCES usuarios_sistema (codigo_usuario_sistema)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS dispositivos_cobrador (
    id_dispositivo VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    codigo_sucursal INT NOT NULL,
    codigo_cobrador INT NULL,
    nombre_dispositivo VARCHAR(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_conexion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id_dispositivo),
    INDEX idx_dispositivo_cobrador (codigo_cobrador)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS puntos_recorrido_cobrador (
    codigo_punto BIGINT NOT NULL AUTO_INCREMENT,
    id_sincronizacion CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    codigo_recorrido BIGINT NOT NULL,
    id_dispositivo VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
    latitud DECIMAL(10,7) NOT NULL,
    longitud DECIMAL(10,7) NOT NULL,
    precision_metros DECIMAL(10,2) NULL,
    velocidad_metros_segundo DECIMAL(10,2) NULL,
    rumbo_grados DECIMAL(6,2) NULL,
    fecha_dispositivo DATETIME NOT NULL,
    fecha_recepcion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    origen VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'APP',
    PRIMARY KEY (codigo_punto),
    UNIQUE INDEX uq_punto_id_sincronizacion (id_sincronizacion),
    INDEX idx_punto_recorrido_fecha (codigo_recorrido, fecha_dispositivo, codigo_punto),
    INDEX idx_punto_dispositivo (id_dispositivo),
    CONSTRAINT chk_punto_recorrido_latitud CHECK (latitud BETWEEN -90 AND 90),
    CONSTRAINT chk_punto_recorrido_longitud CHECK (longitud BETWEEN -180 AND 180),
    CONSTRAINT chk_punto_recorrido_precision
        CHECK (precision_metros IS NULL OR precision_metros >= 0),
    CONSTRAINT chk_punto_recorrido_velocidad
        CHECK (velocidad_metros_segundo IS NULL OR velocidad_metros_segundo >= 0),
    CONSTRAINT chk_punto_recorrido_rumbo
        CHECK (rumbo_grados IS NULL OR (rumbo_grados >= 0 AND rumbo_grados < 360)),
    CONSTRAINT chk_punto_recorrido_origen CHECK (origen IN ('WEB', 'APP')),
    CONSTRAINT fk_punto_recorrido
        FOREIGN KEY (codigo_recorrido) REFERENCES recorridos_cobrador (codigo_recorrido)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Compatibilidad con una tabla de puntos creada antes de incorporar sincronizacion.
SET @flyway_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'puntos_recorrido_cobrador'
          AND COLUMN_NAME = 'id_sincronizacion'
    ),
    'DO 0',
    'ALTER TABLE puntos_recorrido_cobrador ADD COLUMN id_sincronizacion CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL AFTER codigo_punto'
);
PREPARE flyway_stmt FROM @flyway_ddl;
EXECUTE flyway_stmt;
DEALLOCATE PREPARE flyway_stmt;

SET @flyway_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'puntos_recorrido_cobrador'
          AND COLUMN_NAME = 'rumbo_grados'
    ),
    'DO 0',
    'ALTER TABLE puntos_recorrido_cobrador ADD COLUMN rumbo_grados DECIMAL(6,2) NULL AFTER velocidad_metros_segundo'
);
PREPARE flyway_stmt FROM @flyway_ddl;
EXECUTE flyway_stmt;
DEALLOCATE PREPARE flyway_stmt;

UPDATE puntos_recorrido_cobrador
SET id_sincronizacion = LOWER(UUID())
WHERE id_sincronizacion IS NULL OR id_sincronizacion = '';

ALTER TABLE puntos_recorrido_cobrador
    MODIFY id_sincronizacion CHAR(36)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

SET @flyway_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'puntos_recorrido_cobrador'
          AND INDEX_NAME = 'uq_punto_id_sincronizacion'
    ),
    'DO 0',
    'ALTER TABLE puntos_recorrido_cobrador ADD UNIQUE INDEX uq_punto_id_sincronizacion (id_sincronizacion)'
);
PREPARE flyway_stmt FROM @flyway_ddl;
EXECUTE flyway_stmt;
DEALLOCATE PREPARE flyway_stmt;

SET @flyway_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'puntos_recorrido_cobrador'
          AND COLUMN_NAME = 'id_dispositivo'
    ),
    'DO 0',
    'ALTER TABLE puntos_recorrido_cobrador ADD COLUMN id_dispositivo VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL AFTER codigo_recorrido'
);
PREPARE flyway_stmt FROM @flyway_ddl;
EXECUTE flyway_stmt;
DEALLOCATE PREPARE flyway_stmt;

SET @flyway_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'puntos_recorrido_cobrador'
          AND INDEX_NAME = 'idx_punto_dispositivo'
    ),
    'DO 0',
    'ALTER TABLE puntos_recorrido_cobrador ADD INDEX idx_punto_dispositivo (id_dispositivo)'
);
PREPARE flyway_stmt FROM @flyway_ddl;
EXECUTE flyway_stmt;
DEALLOCATE PREPARE flyway_stmt;
