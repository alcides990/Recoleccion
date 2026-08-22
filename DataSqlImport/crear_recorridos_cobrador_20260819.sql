-- Recorridos de cobradores y puntos GPS capturados por la aplicación móvil.
-- La administración de estos registros se restringe al rol ROOT en el backend.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

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
    INDEX idx_recorrido_cobrador_estado
        (codigo_cobrador, estado, fecha_registro DESC),
    INDEX idx_recorrido_estado_inicio (estado, fecha_inicio DESC),
    CONSTRAINT chk_recorrido_estado
        CHECK (estado IN ('PENDIENTE', 'ACTIVO', 'FINALIZADO')),
    CONSTRAINT chk_recorrido_origen_registro
        CHECK (origen_registro IN ('WEB', 'APP')),
    CONSTRAINT chk_recorrido_origen_inicio
        CHECK (origen_inicio IS NULL OR origen_inicio IN ('WEB', 'APP')),
    CONSTRAINT chk_recorrido_origen_fin
        CHECK (origen_fin IS NULL OR origen_fin IN ('WEB', 'APP')),
    CONSTRAINT fk_recorrido_cobrador
        FOREIGN KEY (codigo_cobrador) REFERENCES cobradores (codigo_cobrador),
    CONSTRAINT fk_recorrido_usuario_registro
        FOREIGN KEY (codigo_usuario_registro)
        REFERENCES usuarios_sistema (codigo_usuario_sistema),
    CONSTRAINT fk_recorrido_usuario_inicio
        FOREIGN KEY (codigo_usuario_inicio)
        REFERENCES usuarios_sistema (codigo_usuario_sistema),
    CONSTRAINT fk_recorrido_usuario_fin
        FOREIGN KEY (codigo_usuario_fin)
        REFERENCES usuarios_sistema (codigo_usuario_sistema)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS puntos_recorrido_cobrador (
    codigo_punto BIGINT NOT NULL AUTO_INCREMENT,
    id_sincronizacion CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    codigo_recorrido BIGINT NOT NULL,
    latitud DECIMAL(10,7) NOT NULL,
    longitud DECIMAL(10,7) NOT NULL,
    precision_metros DECIMAL(10,2) NULL,
    velocidad_metros_segundo DECIMAL(10,2) NULL,
    fecha_dispositivo DATETIME NOT NULL,
    fecha_recepcion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    origen VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'APP',
    PRIMARY KEY (codigo_punto),
    UNIQUE INDEX uq_punto_id_sincronizacion (id_sincronizacion),
    INDEX idx_punto_recorrido_fecha
        (codigo_recorrido, fecha_dispositivo, codigo_punto),
    CONSTRAINT chk_punto_recorrido_latitud CHECK (latitud BETWEEN -90 AND 90),
    CONSTRAINT chk_punto_recorrido_longitud CHECK (longitud BETWEEN -180 AND 180),
    CONSTRAINT chk_punto_recorrido_precision
        CHECK (precision_metros IS NULL OR precision_metros >= 0),
    CONSTRAINT chk_punto_recorrido_velocidad
        CHECK (velocidad_metros_segundo IS NULL OR velocidad_metros_segundo >= 0),
    CONSTRAINT chk_punto_recorrido_origen CHECK (origen IN ('WEB', 'APP')),
    CONSTRAINT fk_punto_recorrido
        FOREIGN KEY (codigo_recorrido)
        REFERENCES recorridos_cobrador (codigo_recorrido)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('recorridos_cobrador', 'puntos_recorrido_cobrador')
ORDER BY TABLE_NAME;
