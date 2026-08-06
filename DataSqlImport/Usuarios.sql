-- 1️⃣ Inicializamos la variable con el último ID existente
SET @id := (SELECT IFNULL(MAX(codigo_usuario), 0) FROM recoleccion.usuarios);

-- 2️⃣ Insertamos los registros incrementando @id automáticamente
INSERT INTO recoleccion.usuarios (
  codigo_usuario,
  codigo_tipo_documento,
  numero_documento,
  nombre, apellido,
  celular,
  barrio,
  direccion,
  codigo_sucursal,
  codigo_estado,
  observacion,
  cuenta_temporal
)
SELECT
  (@id := @id + 1) AS codigo_usuario,
  1 AS codigo_tipo_documento,
  o.nroruc AS numero_documento,
  o.nombre, '',
  o.telefono AS celular,
  o.barrio,
  o.direccion,
  1 AS codigo_sucursal,
  1 AS codigo_estado,
  '' AS observacion, 
  cuenta
FROM recoleccion_migracion.cuentas o 
left JOIN recoleccion.usuarios u
  ON u.cuenta_temporal = o.cuenta
where u.cuenta_temporal is null 