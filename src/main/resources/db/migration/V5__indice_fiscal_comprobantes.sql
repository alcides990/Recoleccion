-- Optimiza la busqueda y la asignacion correlativa de comprobantes por contexto fiscal.
-- Se conserva la clave primaria existente para no afectar las claves foraneas dependientes.
CREATE INDEX idx_comprobantes_contexto_fiscal_numero
    ON comprobantes (codigo_sucursal, codigo_punto_expedicion,
        codigo_tipo_comprobante, codigo_serie, numero_comprobante);
