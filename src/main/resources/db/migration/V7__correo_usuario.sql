-- El celular ya cubre el contacto telefónico del usuario. Se agrega correo sin
-- eliminar la columna telefono para conservar datos históricos e importaciones.
ALTER TABLE usuarios
    ADD COLUMN correo VARCHAR(150) NULL AFTER celular;

CREATE INDEX idx_usuarios_correo ON usuarios (correo);
