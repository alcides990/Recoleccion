INSERT INTO  recoleccion.categorias (codigo_categoria, categoria, tarifa, fecha_modificacion, codigo_sucursal )
SELECT CASE
    WHEN c.catego = 'A240' THEN 1
    WHEN c.catego = 'FRI' THEN 2
    WHEN c.catego = '055' THEN 3
    ELSE CAST(c.catego AS UNSIGNED)
END AS codigo_categoria,
 c.nombre, c.importe, now(), '1'
FROM recoleccion_migracion.categorias as c;