INSERT INTO  recoleccion.manzanas (codigo_manzana, codigo_zona, codigo_sucursal)
SELECT manzana, zona_id, '1'
FROM recoleccion_migracion.manzanas ;