-- Inactiva solamente cobradores activos que no estén relacionados con ninguna zona.
UPDATE cobradores c
   SET c.codigo_estado = 2
 WHERE c.codigo_estado = 1
   AND NOT EXISTS (
       SELECT 1
         FROM zonas z
        WHERE z.codigo_cobrador = c.codigo_cobrador
   );

SELECT ROW_COUNT() AS cobradores_inactivados;

-- Verificación: esta consulta debe devolver cero registros.
SELECT c.codigo_cobrador,
       CONCAT_WS(' ', c.nombre, c.apellido) AS cobrador,
       c.codigo_sucursal
  FROM cobradores c
 WHERE c.codigo_estado = 1
   AND NOT EXISTS (
       SELECT 1
         FROM zonas z
        WHERE z.codigo_cobrador = c.codigo_cobrador
   )
 ORDER BY c.codigo_sucursal, c.codigo_cobrador;
