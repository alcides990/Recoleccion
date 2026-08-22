package ama.servicio;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Mantiene compatible el esquema de seguimiento en todos los perfiles instalados. */
@Component
@RequiredArgsConstructor
public class RecorridoSchemaMigration implements ApplicationRunner {
    private static final Logger LOG=LoggerFactory.getLogger(RecorridoSchemaMigration.class);
    private final JdbcTemplate jdbc;

    @Override public void run(ApplicationArguments args){
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS dispositivos_cobrador (
                  id_dispositivo VARCHAR(64) NOT NULL,
                  codigo_sucursal INT NOT NULL,
                  codigo_cobrador INT NULL,
                  nombre_dispositivo VARCHAR(120) NOT NULL,
                  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  ultima_conexion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  activo TINYINT(1) NOT NULL DEFAULT 1,
                  PRIMARY KEY (id_dispositivo),
                  KEY idx_dispositivo_cobrador (codigo_cobrador)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        Integer column=jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='puntos_recorrido_cobrador'
                  AND COLUMN_NAME='id_dispositivo'
                """,Integer.class);
        if(column==null||column==0){jdbc.execute("ALTER TABLE puntos_recorrido_cobrador ADD COLUMN id_dispositivo VARCHAR(64) NULL AFTER codigo_recorrido");LOG.info("Migración de recorridos: columna id_dispositivo creada");}
        Integer index=jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='puntos_recorrido_cobrador'
                  AND INDEX_NAME='idx_punto_dispositivo'
                """,Integer.class);
        if(index==null||index==0)jdbc.execute("ALTER TABLE puntos_recorrido_cobrador ADD INDEX idx_punto_dispositivo (id_dispositivo)");
    }
}
