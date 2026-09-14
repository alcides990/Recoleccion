-- Sincronizacion puntual verificada contra el SGC el 2026-08-30.
-- Esta migracion no depende de recoleccion_migracion y puede ejecutarse
-- tanto en una instalacion nueva como en una base sincronizada manualmente.

SET NAMES utf8mb4;

-- Respaldo recuperable de los cobradores que deben normalizarse.
CREATE TABLE IF NOT EXISTS backup_sgc_cobradores_20260830 LIKE cobradores;

INSERT IGNORE INTO backup_sgc_cobradores_20260830
SELECT c.*
  FROM cobradores c
 WHERE c.codigo_sucursal = 1
   AND c.codigo_cobrador IN (48, 60, 86, 110, 113);

-- Se actualizan los datos publicados por el SGC, conservando los telefonos
-- locales cuando la fuente no aporta uno nuevo.
UPDATE cobradores
   SET nombre = CASE codigo_cobrador
       WHEN 48  THEN 'OSMAR MORAL'
       WHEN 60  THEN 'PEDRO ANDRES MARIN'
       WHEN 86  THEN 'GUSTAVO KAATY'
       WHEN 110 THEN 'GUSTAVO BARSZES'
       WHEN 113 THEN 'LIZ SOSTOA'
       ELSE nombre
   END,
       celular = CASE codigo_cobrador
       WHEN 110 THEN '0985982979'
       WHEN 113 THEN '0984297117'
       ELSE celular
   END
 WHERE codigo_sucursal = 1
   AND codigo_cobrador IN (48, 60, 86, 110, 113);

-- Se crean primero los usuarios de las cuentas ausentes. La asignacion parte
-- del maximo actual porque codigo_usuario no es autoincremental en el legado.
INSERT INTO usuarios (
    codigo_usuario,
    codigo_tipo_documento,
    numero_documento,
    nombre,
    apellido,
    celular,
    barrio,
    direccion,
    codigo_sucursal,
    codigo_estado,
    observacion,
    cuenta_temporal
)
SELECT maximos.codigo_usuario +
           ROW_NUMBER() OVER (ORDER BY origen.cuenta_corriente),
       1,
       '0',
       origen.nombre,
       '',
       '0',
       '',
       origen.direccion,
       1,
       1,
       '',
       origen.cuenta_corriente
  FROM (
      SELECT '31-0107-05' AS cuenta_corriente,
             'MIRIAN SALETE VICTORIA' AS nombre,
             'LA BOLSA' AS direccion
      UNION ALL
      SELECT '31-0218-61',
             'ANGELA LEZCANO',
             'PUERTAS DE KATUETE'
  ) origen
 CROSS JOIN (
      SELECT COALESCE(MAX(codigo_usuario), 0) AS codigo_usuario
        FROM usuarios
  ) maximos
  LEFT JOIN usuarios existente
    ON BINARY existente.cuenta_temporal = BINARY origen.cuenta_corriente
 WHERE existente.codigo_usuario IS NULL;

-- Finalmente se crean los servicios únicamente cuando la cuenta aún no está
-- registrada. Las FK validan que categoria y manzana existan en el destino.
INSERT INTO servicios (
    cuenta_corriente,
    direccion,
    fecha_inicio,
    ocupado,
    codigo_usuario,
    codigo_categoria,
    codigo_sucursal,
    codigo_manzana,
    codigo_estado,
    observacion
)
SELECT origen.cuenta_corriente,
       origen.direccion,
       origen.fecha_inicio,
       'OCUPADO',
       usuario.codigo_usuario,
       origen.codigo_categoria,
       1,
       origen.codigo_manzana,
       1,
       ''
  FROM (
      SELECT '31-0107-05' AS cuenta_corriente,
             'LA BOLSA' AS direccion,
             DATE('2026-08-01') AS fecha_inicio,
             71 AS codigo_categoria,
             107 AS codigo_manzana
      UNION ALL
      SELECT '31-0218-61',
             'PUERTAS DE KATUETE',
             DATE('2026-07-01'),
             69,
             218
  ) origen
  JOIN usuarios usuario
    ON BINARY usuario.cuenta_temporal = BINARY origen.cuenta_corriente
  LEFT JOIN servicios existente
    ON BINARY existente.cuenta_corriente = BINARY origen.cuenta_corriente
 WHERE existente.cuenta_corriente IS NULL;
