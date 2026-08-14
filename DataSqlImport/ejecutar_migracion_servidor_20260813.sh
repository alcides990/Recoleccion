#!/usr/bin/env bash
set -Eeuo pipefail

MIGRATION_DIR=/home/alcides/migracion
BACKUP_DIR=/opt/recoleccion/backups
DATABASE=recoleccion
STAMP=$(date +%Y%m%d_%H%M%S)

set -a
# shellcheck disable=SC1091
source /etc/recoleccion.env
set +a

: "${DB_USERNAME:?Falta DB_USERNAME en /etc/recoleccion.env}"
: "${DB_PASSWORD:?Falta DB_PASSWORD en /etc/recoleccion.env}"
export MYSQL_PWD="$DB_PASSWORD"
MYSQL=(mysql --user="$DB_USERNAME" --host=localhost)
MYSQLDUMP=(mysqldump --user="$DB_USERNAME" --host=localhost --no-tablespaces)

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

echo "Conteo anterior:"
"${MYSQL[@]}" --batch --skip-column-names "$DATABASE" \
  -e "SELECT COUNT(*) FROM comprobantes;"

echo "Creando respaldo de comprobantes..."
"${MYSQLDUMP[@]}" --single-transaction --quick "$DATABASE" comprobantes \
  | gzip > "$BACKUP_DIR/comprobantes_antes_migracion_${STAMP}.sql.gz"
chmod 600 "$BACKUP_DIR/comprobantes_antes_migracion_${STAMP}.sql.gz"

cleanup() { unset MYSQL_PWD; }
trap cleanup EXIT

echo "Importando comprobantes faltantes..."
"${MYSQL[@]}" --table "$DATABASE" \
  < "$MIGRATION_DIR/migrar_ultimos_2000_comprobantes_20260813.sql"

echo "Actualizando periodo_pago y pago_hasta..."
"${MYSQL[@]}" --table "$DATABASE" \
  < "$MIGRATION_DIR/actualizar_periodos_ultimos_2000_20260813.sql"

echo "Verificacion final:"
"${MYSQL[@]}" --table "$DATABASE" -e "
SELECT COUNT(*) AS total_comprobantes,
       MIN(fecha_pago) AS primera_fecha,
       MAX(fecha_pago) AS ultima_fecha
FROM comprobantes;
SELECT COUNT(*) AS activos_incompletos
FROM comprobantes c
JOIN backup_periodos_ultimos_2000_20260813 b
  ON b.numero_comprobante=c.numero_comprobante
 AND b.codigo_punto_expedicion=c.codigo_punto_expedicion
 AND b.codigo_sucursal=c.codigo_sucursal
 AND b.codigo_serie=c.codigo_serie
 AND b.codigo_tipo_comprobante=c.codigo_tipo_comprobante
WHERE c.codigo_estado=1 AND c.cantidad_pago>0
  AND (c.periodo_pago IS NULL OR c.pago_hasta IS NULL);
"

echo "Respaldo: $BACKUP_DIR/comprobantes_antes_migracion_${STAMP}.sql.gz"
