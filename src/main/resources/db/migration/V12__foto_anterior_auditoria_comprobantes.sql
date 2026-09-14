-- Conserva la foto previa para comparar modificaciones de comprobantes.
ALTER TABLE auditoria_comprobantes
    ADD COLUMN datos_anteriores JSON NULL AFTER datos_comprobante;
