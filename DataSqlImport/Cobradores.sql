INSERT INTO recoleccion.cobradores
(
    codigo_cobrador,
    nombre,
    apellido,
    celular,
    codigo_sucursal,
    codigo_estado
)
SELECT 
    m.codcob,
    m.nombcob,
    '',
    m.telefono,
    1,
    1
FROM recoleccion_migracion.cobradores m
LEFT JOIN recoleccion.cobradores c
    ON m.codcob = c.codigo_cobrador
WHERE c.codigo_cobrador IS NULL;
