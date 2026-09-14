ALTER TABLE egreso_catalogos
    MODIFY clase VARCHAR(30) NOT NULL;

ALTER TABLE egreso_catalogos
    ADD COLUMN categoria_id BIGINT NULL AFTER monto,
    ADD KEY idx_egreso_catalogos_categoria (codigo_sucursal, categoria_id),
    ADD CONSTRAINT fk_egreso_catalogos_categoria
        FOREIGN KEY (categoria_id, codigo_sucursal)
        REFERENCES egreso_catalogos (id, codigo_sucursal)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;

INSERT INTO egreso_catalogos (codigo_sucursal, clase, nombre, documento, telefono, direccion, correo, monto)
SELECT sucursal.codigo_sucursal, 'CATEGORIA_PRODUCTO', 'Combustible', '', '', '', '', 0
  FROM sucursales sucursal
 WHERE NOT EXISTS (
       SELECT 1
         FROM egreso_catalogos categoria
        WHERE categoria.codigo_sucursal = sucursal.codigo_sucursal
          AND categoria.clase = 'CATEGORIA_PRODUCTO'
          AND categoria.nombre = 'Combustible'
 );

UPDATE egreso_catalogos producto
  JOIN egreso_catalogos categoria
    ON categoria.codigo_sucursal = producto.codigo_sucursal
   AND categoria.clase = 'CATEGORIA_PRODUCTO'
   AND categoria.nombre = 'Combustible'
   SET producto.categoria_id = categoria.id
 WHERE producto.clase = 'PRODUCTO'
   AND producto.categoria_id IS NULL
   AND (LOWER(producto.nombre) LIKE '%diesel%'
        OR LOWER(producto.nombre) LIKE '%disel%');
