SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS backup_ocupacion_servicios_20260814 (
    cuenta_corriente VARCHAR(30) NOT NULL,
    ocupado VARCHAR(15) NOT NULL,
    PRIMARY KEY (cuenta_corriente)
) AS
SELECT cuenta_corriente, ocupado
FROM recoleccion.servicios;

START TRANSACTION;

UPDATE recoleccion.servicios s
JOIN recoleccion_migracion.cuentas o
  ON o.cuenta = s.cuenta_corriente
SET s.ocupado = CASE UPPER(TRIM(o.ocupado))
    WHEN 'S' THEN 'OCUPADO'
    WHEN 'SI' THEN 'OCUPADO'
    WHEN '1' THEN 'OCUPADO'
    WHEN 'OCUPADO' THEN 'OCUPADO'
    WHEN 'N' THEN 'DESOCUPADO'
    WHEN 'NO' THEN 'DESOCUPADO'
    WHEN '0' THEN 'DESOCUPADO'
    WHEN 'NO OCUPADO' THEN 'DESOCUPADO'
    WHEN 'DESOCUPADO' THEN 'DESOCUPADO'
    WHEN 'BALDIO' THEN 'BALDIO'
    WHEN 'BALDÍO' THEN 'BALDIO'
    WHEN 'VALDIO' THEN 'BALDIO'
    WHEN 'VALDÍO' THEN 'BALDIO'
    ELSE s.ocupado
END;

SELECT ROW_COUNT() AS filas_modificadas;
COMMIT;

SELECT ocupado, COUNT(*) AS cantidad
FROM recoleccion.servicios
GROUP BY ocupado
ORDER BY ocupado DESC;

SELECT s.cuenta_corriente AS cuenta_sin_origen
FROM recoleccion.servicios s
LEFT JOIN recoleccion_migracion.cuentas o
  ON o.cuenta = s.cuenta_corriente
WHERE o.cuenta IS NULL;
