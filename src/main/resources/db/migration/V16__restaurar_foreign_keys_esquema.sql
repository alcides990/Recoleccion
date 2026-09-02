-- Reconstruye las relaciones historicas eliminadas del esquema.
-- No modifica datos. La validacion se ejecuta antes de retirar restricciones.

DROP PROCEDURE IF EXISTS validar_integridad_relaciones_historicas;

DELIMITER $$

CREATE PROCEDURE validar_integridad_relaciones_historicas()
BEGIN
    DECLARE huerfanos BIGINT DEFAULT 0;

    SELECT
        (SELECT COUNT(*) FROM categorias a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM cobradores a LEFT JOIN estados b ON b.codigo_estado=a.codigo_estado WHERE b.codigo_estado IS NULL)
      + (SELECT COUNT(*) FROM cobradores a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM detalle_pago a LEFT JOIN metodos_pago b ON b.codigo_metodo_pago=a.codigo_metodo_pago WHERE b.codigo_metodo_pago IS NULL)
      + (SELECT COUNT(*) FROM detalle_pago a LEFT JOIN comprobantes b
            ON b.numero_comprobante=a.numero_comprobante
           AND b.codigo_punto_expedicion=a.codigo_punto_expedicion
           AND b.codigo_sucursal=a.codigo_sucursal
           AND b.codigo_serie=a.codigo_serie
           AND b.codigo_tipo_comprobante=a.codigo_tipo_comprobante
          WHERE b.numero_comprobante IS NULL)
      + (SELECT COUNT(*) FROM detalle_usuario_sistema a LEFT JOIN usuarios_sistema b ON b.codigo_usuario_sistema=a.codigo_usuario_sistema WHERE b.codigo_usuario_sistema IS NULL)
      + (SELECT COUNT(*) FROM detalle_usuario_sistema a LEFT JOIN roles b ON b.codigo_rol=a.codigo_rol WHERE b.codigo_rol IS NULL)
      + (SELECT COUNT(*) FROM manzanas a LEFT JOIN zonas b ON b.codigo_zona=a.codigo_zona WHERE b.codigo_zona IS NULL)
      + (SELECT COUNT(*) FROM manzanas a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM parametros a LEFT JOIN comisiones b ON b.codigo_comision=a.codigo_comision WHERE b.codigo_comision IS NULL)
      + (SELECT COUNT(*) FROM parametros a LEFT JOIN empresas b ON b.codigo_empresa=a.codigo_empresa WHERE b.codigo_empresa IS NULL)
      + (SELECT COUNT(*) FROM parametros a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM puntos_expedicion a LEFT JOIN empresas b ON b.codigo_empresa=a.codigo_empresa WHERE b.codigo_empresa IS NULL)
      + (SELECT COUNT(*) FROM puntos_expedicion a LEFT JOIN estados b ON b.codigo_estado=a.codigo_estado WHERE b.codigo_estado IS NULL)
      + (SELECT COUNT(*) FROM puntos_expedicion a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM servicios a LEFT JOIN categorias b ON b.codigo_categoria=a.codigo_categoria WHERE b.codigo_categoria IS NULL)
      + (SELECT COUNT(*) FROM servicios a LEFT JOIN estados b ON b.codigo_estado=a.codigo_estado WHERE b.codigo_estado IS NULL)
      + (SELECT COUNT(*) FROM servicios a LEFT JOIN usuarios b ON b.codigo_usuario=a.codigo_usuario WHERE b.codigo_usuario IS NULL)
      + (SELECT COUNT(*) FROM servicios a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM servicios a LEFT JOIN manzanas b
            ON b.codigo_manzana=a.codigo_manzana AND b.codigo_sucursal=a.codigo_sucursal
          WHERE b.codigo_manzana IS NULL)
      + (SELECT COUNT(*) FROM sucursales a LEFT JOIN ciudades b ON b.codigo_ciudad=a.codigo_ciudad WHERE b.codigo_ciudad IS NULL)
      + (SELECT COUNT(*) FROM sucursales a LEFT JOIN empresas b ON b.codigo_empresa=a.codigo_empresa WHERE b.codigo_empresa IS NULL)
      + (SELECT COUNT(*) FROM timbrados a LEFT JOIN empresas b ON b.codigo_empresa=a.codigo_empresa WHERE b.codigo_empresa IS NULL)
      + (SELECT COUNT(*) FROM timbrados a LEFT JOIN estados b ON b.codigo_estado=a.codigo_estado WHERE b.codigo_estado IS NULL)
      + (SELECT COUNT(*) FROM usuarios a LEFT JOIN tipos_documento b ON b.codigo_tipo_documento=a.codigo_tipo_documento WHERE b.codigo_tipo_documento IS NULL)
      + (SELECT COUNT(*) FROM usuarios a LEFT JOIN estados b ON b.codigo_estado=a.codigo_estado WHERE b.codigo_estado IS NULL)
      + (SELECT COUNT(*) FROM usuarios a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM usuarios_sistema a LEFT JOIN estados b ON b.codigo_estado=a.codigo_estado WHERE b.codigo_estado IS NULL)
      + (SELECT COUNT(*) FROM usuarios_sistema a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM zonas a LEFT JOIN cobradores b ON b.codigo_cobrador=a.codigo_cobrador WHERE b.codigo_cobrador IS NULL)
      + (SELECT COUNT(*) FROM zonas a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM historial_suspensiones_servicio a LEFT JOIN servicios b ON b.cuenta_corriente=a.cuenta_corriente WHERE b.cuenta_corriente IS NULL)
      + (SELECT COUNT(*) FROM historial_suspensiones_servicio a LEFT JOIN usuarios_sistema b ON b.codigo_usuario_sistema=a.codigo_usuario_sistema WHERE b.codigo_usuario_sistema IS NULL)
      + (SELECT COUNT(*) FROM historial_exoneraciones_servicio a LEFT JOIN servicios b ON b.cuenta_corriente=a.cuenta_corriente WHERE b.cuenta_corriente IS NULL)
      + (SELECT COUNT(*) FROM historial_exoneraciones_servicio a LEFT JOIN usuarios_sistema b ON b.codigo_usuario_sistema=a.codigo_usuario_sistema WHERE b.codigo_usuario_sistema IS NULL)
      + (SELECT COUNT(*) FROM dispositivos_cobrador a LEFT JOIN sucursales b ON b.codigo_sucursal=a.codigo_sucursal WHERE b.codigo_sucursal IS NULL)
      + (SELECT COUNT(*) FROM dispositivos_cobrador a LEFT JOIN cobradores b ON b.codigo_cobrador=a.codigo_cobrador WHERE a.codigo_cobrador IS NOT NULL AND b.codigo_cobrador IS NULL)
      INTO huerfanos;

    IF huerfanos > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se pueden crear las FK generales: existen referencias huerfanas. Restaure primero los datos faltantes.';
    END IF;
END$$

DELIMITER ;

CALL validar_integridad_relaciones_historicas();
DROP PROCEDURE validar_integridad_relaciones_historicas;

DROP PROCEDURE IF EXISTS eliminar_foreign_keys_relaciones_historicas;

DELIMITER $$

CREATE PROCEDURE eliminar_foreign_keys_relaciones_historicas()
BEGIN
    DECLARE terminado BOOLEAN DEFAULT FALSE;
    DECLARE tabla_actual VARCHAR(64);
    DECLARE restriccion_actual VARCHAR(64);
    DECLARE relaciones CURSOR FOR
        SELECT DISTINCT kcu.TABLE_NAME, kcu.CONSTRAINT_NAME
          FROM information_schema.KEY_COLUMN_USAGE kcu
         WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
           AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
           AND kcu.TABLE_NAME IN (
               'categorias', 'cobradores', 'detalle_pago', 'detalle_usuario_sistema',
               'manzanas', 'parametros', 'puntos_expedicion', 'servicios',
               'sucursales', 'timbrados', 'usuarios', 'usuarios_sistema', 'zonas',
               'historial_suspensiones_servicio', 'historial_exoneraciones_servicio',
               'dispositivos_cobrador'
           );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET terminado = TRUE;

    OPEN relaciones;
    ciclo: LOOP
        FETCH relaciones INTO tabla_actual, restriccion_actual;
        IF terminado THEN LEAVE ciclo; END IF;
        SET @ddl_fk = CONCAT('ALTER TABLE `', REPLACE(tabla_actual, '`', '``'),
            '` DROP FOREIGN KEY `', REPLACE(restriccion_actual, '`', '``'), '`');
        PREPARE sentencia_fk FROM @ddl_fk;
        EXECUTE sentencia_fk;
        DEALLOCATE PREPARE sentencia_fk;
    END LOOP;
    CLOSE relaciones;
END$$

DELIMITER ;

CALL eliminar_foreign_keys_relaciones_historicas();
DROP PROCEDURE eliminar_foreign_keys_relaciones_historicas;

ALTER TABLE categorias
    ADD CONSTRAINT fk_categorias_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE cobradores
    ADD CONSTRAINT fk_cobradores_estado FOREIGN KEY (codigo_estado) REFERENCES estados (codigo_estado) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_cobradores_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE detalle_pago
    ADD CONSTRAINT fk_detalle_pago_metodo FOREIGN KEY (codigo_metodo_pago) REFERENCES metodos_pago (codigo_metodo_pago) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_detalle_pago_comprobante FOREIGN KEY (numero_comprobante, codigo_punto_expedicion, codigo_sucursal, codigo_serie, codigo_tipo_comprobante)
        REFERENCES comprobantes (numero_comprobante, codigo_punto_expedicion, codigo_sucursal, codigo_serie, codigo_tipo_comprobante)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE detalle_usuario_sistema
    ADD CONSTRAINT fk_detalle_usuario_usuario FOREIGN KEY (codigo_usuario_sistema) REFERENCES usuarios_sistema (codigo_usuario_sistema) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_detalle_usuario_rol FOREIGN KEY (codigo_rol) REFERENCES roles (codigo_rol) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE manzanas
    ADD CONSTRAINT fk_manzanas_zona FOREIGN KEY (codigo_zona) REFERENCES zonas (codigo_zona) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_manzanas_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE parametros
    ADD CONSTRAINT fk_parametros_comision FOREIGN KEY (codigo_comision) REFERENCES comisiones (codigo_comision) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_parametros_empresa FOREIGN KEY (codigo_empresa) REFERENCES empresas (codigo_empresa) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_parametros_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE puntos_expedicion
    ADD CONSTRAINT fk_puntos_empresa FOREIGN KEY (codigo_empresa) REFERENCES empresas (codigo_empresa) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_puntos_estado FOREIGN KEY (codigo_estado) REFERENCES estados (codigo_estado) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_puntos_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE servicios
    ADD CONSTRAINT fk_servicios_categoria FOREIGN KEY (codigo_categoria) REFERENCES categorias (codigo_categoria) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_servicios_estado FOREIGN KEY (codigo_estado) REFERENCES estados (codigo_estado) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_servicios_usuario FOREIGN KEY (codigo_usuario) REFERENCES usuarios (codigo_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_servicios_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_servicios_manzana FOREIGN KEY (codigo_manzana, codigo_sucursal) REFERENCES manzanas (codigo_manzana, codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE sucursales
    ADD CONSTRAINT fk_sucursales_ciudad FOREIGN KEY (codigo_ciudad) REFERENCES ciudades (codigo_ciudad) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_sucursales_empresa FOREIGN KEY (codigo_empresa) REFERENCES empresas (codigo_empresa) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE timbrados
    ADD CONSTRAINT fk_timbrados_empresa FOREIGN KEY (codigo_empresa) REFERENCES empresas (codigo_empresa) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_timbrados_estado FOREIGN KEY (codigo_estado) REFERENCES estados (codigo_estado) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_tipo_documento FOREIGN KEY (codigo_tipo_documento) REFERENCES tipos_documento (codigo_tipo_documento) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_usuarios_estado FOREIGN KEY (codigo_estado) REFERENCES estados (codigo_estado) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_usuarios_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE usuarios_sistema
    ADD CONSTRAINT fk_usuarios_sistema_estado FOREIGN KEY (codigo_estado) REFERENCES estados (codigo_estado) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_usuarios_sistema_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE zonas
    ADD CONSTRAINT fk_zonas_cobrador FOREIGN KEY (codigo_cobrador) REFERENCES cobradores (codigo_cobrador) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_zonas_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE historial_suspensiones_servicio
    ADD CONSTRAINT fk_suspension_servicio FOREIGN KEY (cuenta_corriente) REFERENCES servicios (cuenta_corriente) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_suspension_usuario_sistema FOREIGN KEY (codigo_usuario_sistema) REFERENCES usuarios_sistema (codigo_usuario_sistema) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE historial_exoneraciones_servicio
    ADD CONSTRAINT fk_exoneracion_servicio FOREIGN KEY (cuenta_corriente) REFERENCES servicios (cuenta_corriente) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_exoneracion_usuario_sistema FOREIGN KEY (codigo_usuario_sistema) REFERENCES usuarios_sistema (codigo_usuario_sistema) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE dispositivos_cobrador
    ADD CONSTRAINT fk_dispositivos_sucursal FOREIGN KEY (codigo_sucursal) REFERENCES sucursales (codigo_sucursal) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_dispositivos_cobrador FOREIGN KEY (codigo_cobrador) REFERENCES cobradores (codigo_cobrador) ON UPDATE CASCADE ON DELETE RESTRICT;
