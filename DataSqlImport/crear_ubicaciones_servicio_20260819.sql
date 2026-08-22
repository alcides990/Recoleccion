-- Geolocalización actual de los servicios e historial inmutable de cambios.
-- Ejecutar una sola vez sobre la base de datos de la aplicación.
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
    CONSTRAINT chk_historial_ubicacion_latitud_nueva
        CHECK (latitud_nueva BETWEEN -90 AND 90),
    CONSTRAINT chk_historial_ubicacion_longitud_nueva
        CHECK (longitud_nueva BETWEEN -180 AND 180),
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

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('ubicaciones_servicio', 'historial_ubicaciones_servicio')
ORDER BY TABLE_NAME;
