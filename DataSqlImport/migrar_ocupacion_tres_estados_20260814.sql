-- La ocupación pasa de booleano a un estado explícito.
ALTER TABLE servicios MODIFY ocupado VARCHAR(15) NOT NULL DEFAULT 'OCUPADO';

UPDATE servicios SET ocupado = CASE
    WHEN ocupado IN ('1', 'S', 'SI', 'OCUPADO') THEN 'OCUPADO'
    WHEN ocupado IN ('0', 'N', 'NO', 'DESOCUPADO') THEN 'DESOCUPADO'
    WHEN UPPER(ocupado) IN ('BALDIO', 'BALDÍO', 'VALDIO', 'VALDÍO') THEN 'BALDIO'
    ELSE 'OCUPADO'
END;

-- Sin pagos activos ni exoneraciones, el primer período pendiente es la fecha inicial.
UPDATE servicios s
SET s.fecha_desde = s.fecha_inicio
WHERE NOT EXISTS (
    SELECT 1 FROM comprobantes c
    WHERE c.cuenta_corriente = s.cuenta_corriente AND c.codigo_estado = 1
)
AND NOT EXISTS (
    SELECT 1 FROM historial_exoneraciones_servicio he
    WHERE he.cuenta_corriente = s.cuenta_corriente
);

