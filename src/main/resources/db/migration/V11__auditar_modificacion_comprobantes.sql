-- Permite registrar las modificaciones autorizadas de comprobantes manuales.
ALTER TABLE auditoria_comprobantes
    DROP CHECK chk_auditoria_accion;

ALTER TABLE auditoria_comprobantes
    ADD CONSTRAINT chk_auditoria_accion CHECK (accion IN
        ('EMISION', 'MODIFICACION', 'IMPRESION', 'REIMPRESION', 'ANULACION',
         'INTENTO_MODIFICACION'));
