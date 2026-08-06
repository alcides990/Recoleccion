--  Para realizar migracion cambiar el campo catego con los siguentes valores
-- CASE
--     WHEN c.catego = 'A240' THEN 1
--     WHEN c.catego = 'FRI' THEN 2
--     WHEN c.catego = '055' THEN 3
--      WHEN c.catego = 0 THEN 4
--     else c.catego
--     END

INSERT INTO recoleccion.servicios (
  cuenta_corriente,
  direccion,
  fecha_inicio,
  codigo_usuario,
  codigo_categoria,
  codigo_sucursal,
  codigo_manzana,
  codigo_estado,
  observacion
)
SELECT
  c.cuenta AS cuenta_corriente,
  c.direccion,
  STR_TO_DATE(c.inicio, '%Y-%m-%d') AS fecha_inicio,
  u.codigo_usuario,   -- buscamos el código del usuario por nombre
  c.catego ,
  1 AS codigo_sucursal,
  CAST(c.nromanzana AS UNSIGNED) AS codigo_manzana,
  1 AS codigo_estado,
  c.observacion
FROM recoleccion_migracion.cuentas c
JOIN recoleccion.usuarios u
  ON u.cuenta_temporal = c.cuenta
  left JOIN recoleccion.servicios s
  ON s.cuenta_corriente = c.cuenta
where  s.cuenta_corriente is null 
  
 
