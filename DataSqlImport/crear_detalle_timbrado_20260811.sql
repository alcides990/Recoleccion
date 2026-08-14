CREATE TABLE IF NOT EXISTS detalle_timbrado (
    codigo_timbrado INT NOT NULL,
    codigo_punto_expedicion INT NOT NULL,
    codigo_sucursal INT NOT NULL,
    codigo_serie INT NOT NULL,
    codigo_estado INT NOT NULL,
    PRIMARY KEY (codigo_timbrado, codigo_punto_expedicion, codigo_sucursal),
    CONSTRAINT fk_detalle_timbrado_timbrado
        FOREIGN KEY (codigo_timbrado) REFERENCES timbrados (codigo_timbrado),
    CONSTRAINT fk_detalle_timbrado_punto
        FOREIGN KEY (codigo_punto_expedicion, codigo_sucursal)
        REFERENCES puntos_expedicion (codigo_punto_expedicion, codigo_sucursal),
    CONSTRAINT fk_detalle_timbrado_serie
        FOREIGN KEY (codigo_serie) REFERENCES series (codigo_serie),
    CONSTRAINT fk_detalle_timbrado_estado
        FOREIGN KEY (codigo_estado) REFERENCES estados (codigo_estado)
);

INSERT INTO detalle_timbrado (
    codigo_timbrado, codigo_punto_expedicion, codigo_sucursal,
    codigo_serie, codigo_estado
)
SELECT 1, 1, 1, 2, 1
WHERE EXISTS (SELECT 1 FROM timbrados WHERE codigo_timbrado = 1)
  AND EXISTS (
      SELECT 1 FROM puntos_expedicion
      WHERE codigo_punto_expedicion = 1 AND codigo_sucursal = 1
  )
  AND NOT EXISTS (
      SELECT 1 FROM detalle_timbrado
      WHERE codigo_timbrado = 1
        AND codigo_punto_expedicion = 1
        AND codigo_sucursal = 1
  );
