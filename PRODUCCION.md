# Puesta en produccion

Documentos relacionados:

- [Documentación técnica de facturación y autoimpresor](docs/DOCUMENTACION_TECNICA_AUTOIMPRESOR.md)
- [Manual de usuario de facturación](docs/MANUAL_USUARIO_FACTURACION.md)

## Estado de esta entrega

La facturación manual, el autoimpresor, la impresión, la anulación, la auditoría y el reporte de servicios por categoría están implementados. Antes del despliegue se debe ejecutar y firmar la prueba de aceptación descrita en la documentación técnica.

Verificación automatizada del 27/08/2026: **30 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas** (`mvn test`).

Nota de crédito/débito y retención no forman parte de esta entrega. No debe anunciarse una cobertura fiscal completa hasta implementar y validar esos comprobantes.

La aplicacion usa el perfil `prod` y recibe la conexion a MySQL mediante variables de entorno. No se deben guardar contrasenas reales en Git.

## MySQL

Crear una cuenta exclusiva para la aplicacion y concederle permisos solamente sobre su base:

```sql
CREATE USER 'recoleccion'@'localhost'
  IDENTIFIED WITH caching_sha2_password BY 'CAMBIAR_ESTA_CLAVE';
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE
  ON recoleccion.* TO 'recoleccion'@'localhost';
```

Si Java y MySQL estan en equipos diferentes, reemplazar `localhost` por la IP del servidor de aplicaciones. Evitar `%` cuando sea posible.

## Variables requeridas

Ejemplo para Linux con MySQL local en el puerto 3307:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://127.0.0.1:3307/recoleccion?useSSL=false&serverTimezone=America/Asuncion&allowPublicKeyRetrieval=true'
export DB_USERNAME='recoleccion'
export DB_PASSWORD='CAMBIAR_ESTA_CLAVE'
export SERVER_PORT='8080'
```

Si MySQL admite TLS, utilizar `sslMode=REQUIRED` en `DB_URL` y eliminar `useSSL=false` y `allowPublicKeyRetrieval=true`.

## Compilar y ejecutar

```bash
./mvnw clean package -DskipTests
java -jar target/recoleccion-1.0.jar
```

Este proyecto no contiene Maven Wrapper actualmente; en ese caso compilar con `mvn clean package -DskipTests`.

Antes de publicar, ejecutar las pruebas con `mvn test`, colocar la aplicacion detras de HTTPS y establecer `SESSION_COOKIE_SECURE=true`.

## Lista de salida

- Respaldo completo de MySQL realizado y restauración probada.
- Variables de entorno configuradas; ninguna contraseña real incluida en Git.
- Migraciones Flyway aplicadas y validadas.
- `mvn test` ejecutado sin errores.
- Pruebas manual, autoimpresor concurrente, impresión, reimpresión y anulación aprobadas.
- Timbrado, vigencia, establecimientos, puntos y rangos comparados con la autorización.
- Acceso por rol y aislamiento por sucursal comprobados.
- HTTPS activo y `SESSION_COOKIE_SECURE=true`.
- Procedimiento de reversión y responsable del despliegue definidos.
