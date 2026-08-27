-- Indices para la paginacion server-side de la auditoria por sucursal.
CREATE INDEX idx_auditoria_sucursal_fecha
    ON auditoria_comprobantes (codigo_sucursal, fecha_evento DESC, codigo_auditoria DESC);

CREATE INDEX idx_auditoria_sucursal_accion_fecha
    ON auditoria_comprobantes (codigo_sucursal, accion, fecha_evento DESC);

CREATE INDEX idx_auditoria_sucursal_numero_fecha
    ON auditoria_comprobantes (codigo_sucursal, numero_comprobante, fecha_evento DESC);
