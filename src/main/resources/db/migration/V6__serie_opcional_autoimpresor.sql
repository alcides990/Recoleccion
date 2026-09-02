-- En autoimpresor la serie fiscal es opcional. Los comprobantes conservan
-- codigo_serie = 0 como clave tecnica por compatibilidad con su PK historica,
-- pero el detalle del timbrado debe representar la ausencia real con NULL.
ALTER TABLE detalle_timbrado
    MODIFY COLUMN codigo_serie INT NULL;

UPDATE detalle_timbrado
   SET codigo_serie = NULL
 WHERE modo_emision = 'AUTOIMPRESOR'
   AND codigo_serie = 0;
