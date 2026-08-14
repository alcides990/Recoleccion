# Puesta en produccion

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
