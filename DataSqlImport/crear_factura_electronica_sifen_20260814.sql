CREATE TABLE factura_electronica_documentos (
    codigo_documento BIGINT NOT NULL AUTO_INCREMENT,
    codigo_sucursal INT NOT NULL,
    codigo_punto_expedicion INT NOT NULL,
    codigo_tipo_comprobante INT NOT NULL,
    codigo_serie INT NOT NULL,
    numero_comprobante INT NOT NULL,
    cdc VARCHAR(44) NULL,
    ambiente VARCHAR(10) NOT NULL DEFAULT 'TEST',
    estado VARCHAR(40) NOT NULL DEFAULT 'PENDIENTE_CONFIGURACION',
    codigo_respuesta VARCHAR(20) NULL,
    mensaje_respuesta VARCHAR(1000) NULL,
    protocolo_autorizacion VARCHAR(100) NULL,
    ruta_xml_generado VARCHAR(500) NULL,
    ruta_xml_firmado VARCHAR(500) NULL,
    ruta_respuesta VARCHAR(500) NULL,
    ruta_kude VARCHAR(500) NULL,
    hash_xml_firmado CHAR(64) NULL,
    hash_respuesta CHAR(64) NULL,
    hash_kude CHAR(64) NULL,
    cantidad_intentos INT NOT NULL DEFAULT 0,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_ultimo_intento DATETIME NULL,
    fecha_aprobacion DATETIME NULL,
    ultimo_error TEXT NULL,
    PRIMARY KEY (codigo_documento),
    UNIQUE KEY uk_fe_comprobante (codigo_sucursal, codigo_punto_expedicion,
        codigo_tipo_comprobante, codigo_serie, numero_comprobante),
    UNIQUE KEY uk_fe_cdc (cdc),
    INDEX idx_fe_estado_fecha (estado, fecha_creacion),
    CONSTRAINT fk_fe_comprobante FOREIGN KEY (numero_comprobante, codigo_punto_expedicion,
        codigo_sucursal, codigo_serie, codigo_tipo_comprobante)
        REFERENCES comprobantes (numero_comprobante, codigo_punto_expedicion,
            codigo_sucursal, codigo_serie, codigo_tipo_comprobante)
);

CREATE TABLE factura_electronica_eventos (
    codigo_evento BIGINT NOT NULL AUTO_INCREMENT,
    codigo_documento BIGINT NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    estado_anterior VARCHAR(40) NULL,
    estado_nuevo VARCHAR(40) NULL,
    detalle VARCHAR(2000) NULL,
    fecha_evento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (codigo_evento),
    INDEX idx_fe_evento_documento (codigo_documento, fecha_evento),
    CONSTRAINT fk_fe_evento_documento FOREIGN KEY (codigo_documento)
        REFERENCES factura_electronica_documentos (codigo_documento)
);
