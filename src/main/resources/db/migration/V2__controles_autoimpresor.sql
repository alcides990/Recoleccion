-- Controles mínimos para operar comprobantes bajo el régimen de autoimpresor.
-- Las asociaciones existentes permanecen en modo MANUAL hasta que un administrador
-- cargue el rango autorizado por la DNIT y active AUTOIMPRESOR.

ALTER TABLE detalle_timbrado
    ADD COLUMN modo_emision VARCHAR(20) NOT NULL DEFAULT 'MANUAL' AFTER codigo_estado,
    ADD COLUMN numero_desde INT NOT NULL DEFAULT 1 AFTER modo_emision,
    ADD COLUMN numero_hasta INT NOT NULL DEFAULT 9999999 AFTER numero_desde,
    ADD CONSTRAINT chk_detalle_timbrado_modo
        CHECK (modo_emision IN ('MANUAL', 'AUTOIMPRESOR')),
    ADD CONSTRAINT chk_detalle_timbrado_rango
        CHECK (numero_desde BETWEEN 1 AND 9999999
            AND numero_hasta BETWEEN numero_desde AND 9999999);

ALTER TABLE comprobantes
    MODIFY COLUMN obs VARCHAR(500) NULL;

CREATE TABLE numeradores_autoimpresor (
    codigo_timbrado INT NOT NULL,
    codigo_punto_expedicion INT NOT NULL,
    codigo_sucursal INT NOT NULL,
    codigo_tipo_comprobante INT NOT NULL,
    codigo_serie INT NOT NULL,
    ultimo_numero INT NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (codigo_timbrado, codigo_punto_expedicion, codigo_sucursal,
                 codigo_tipo_comprobante, codigo_serie),
    CONSTRAINT fk_numerador_detalle_timbrado
        FOREIGN KEY (codigo_timbrado, codigo_punto_expedicion, codigo_sucursal)
        REFERENCES detalle_timbrado (codigo_timbrado, codigo_punto_expedicion, codigo_sucursal),
    CONSTRAINT fk_numerador_tipo_comprobante
        FOREIGN KEY (codigo_tipo_comprobante)
        REFERENCES tipos_comprobante (codigo_tipo_comprobante),
    CONSTRAINT fk_numerador_serie
        FOREIGN KEY (codigo_serie) REFERENCES series (codigo_serie),
    CONSTRAINT chk_numerador_ultimo
        CHECK (ultimo_numero BETWEEN 0 AND 9999999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE auditoria_comprobantes (
    codigo_auditoria BIGINT NOT NULL AUTO_INCREMENT,
    fecha_evento DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    accion VARCHAR(30) NOT NULL,
    numero_comprobante INT NOT NULL,
    codigo_punto_expedicion INT NOT NULL,
    codigo_sucursal INT NOT NULL,
    codigo_tipo_comprobante INT NOT NULL,
    codigo_serie INT NOT NULL,
    codigo_timbrado INT NOT NULL,
    codigo_usuario_sistema INT NULL,
    motivo VARCHAR(500) NULL,
    datos_comprobante JSON NOT NULL,
    PRIMARY KEY (codigo_auditoria),
    KEY idx_auditoria_documento (codigo_sucursal, codigo_punto_expedicion,
        codigo_tipo_comprobante, codigo_serie, numero_comprobante, fecha_evento),
    KEY idx_auditoria_usuario_fecha (codigo_usuario_sistema, fecha_evento),
    CONSTRAINT chk_auditoria_accion CHECK (accion IN
        ('EMISION', 'IMPRESION', 'REIMPRESION', 'ANULACION', 'INTENTO_MODIFICACION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
