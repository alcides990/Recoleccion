-- Reinicio recuperable del historial Flyway de producción.
-- Conserva todas las tablas y datos de negocio.
-- Ejecutar una sola vez, con la aplicación detenida y antes de desplegar
-- el JAR que contiene V10__migrar_comprobantes_faltantes_sgc.sql.

USE recoleccion;

-- Verificación previa: debe devolver el historial que se va a respaldar.
SELECT installed_rank, version, description, type, success
  FROM flyway_schema_history
 ORDER BY installed_rank;

-- El historial anterior queda disponible para una eventual recuperación.
-- Esta sentencia falla deliberadamente si el respaldo ya existe, evitando
-- sobrescribir una ejecución anterior.
RENAME TABLE flyway_schema_history
          TO flyway_schema_history_backup_20260830;

-- No se crea manualmente una tabla nueva. En el siguiente arranque Flyway:
--   1. detectará el esquema existente sin historial;
--   2. registrará BASELINE en la versión 9;
--   3. ejecutará la V10 autocontenida;
--   4. ejecutará las migraciones repetibles pendientes.

-- Verificación posterior al arranque de la aplicación:
-- SELECT installed_rank, version, description, type, success
--   FROM flyway_schema_history
--  ORDER BY installed_rank;
--
-- El resultado esperado debe contener BASELINE 9 y V10 con success = 1.
