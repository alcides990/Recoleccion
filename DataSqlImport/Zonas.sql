INSERT INTO  recoleccion.zonas (codigo_zona, zona, codigo_cobrador, codigo_sucursal )
SELECT zona_id, nombre, codcob, '1'
FROM recoleccion_migracion.zonas ;