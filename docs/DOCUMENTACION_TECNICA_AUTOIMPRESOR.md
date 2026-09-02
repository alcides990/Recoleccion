# Documentación técnica de facturación y autoimpresor

Versión: 1.0  
Fecha de revisión: 27/08/2026  
## 1. Alcance

Esta versión cubre:

- emisión de facturas manuales;
- emisión con autoimpresor y numeración concurrente;
- asociación de timbrado por sucursal y punto de expedición;
- serie obligatoria en modo manual y opcional en autoimpresor;
- conservación de la foto fiscal en cada comprobante;
- impresión A4, ticket de 58 mm y ticket de 80 mm;
- anulación y auditoría de emisión, impresión y reimpresión;
- reporte de servicios por categoría, general, por zona o por cobrador.

No forman parte de esta entrega: nota de crédito, nota de débito ni comprobante de retención.

## 2. Tecnología

- Java 17.
- Spring Boot 3.0.4.
- Spring MVC, Spring Security y Spring Data JPA.
- MySQL con tablas InnoDB.
- Flyway para migraciones.
- Thymeleaf, jQuery y DataTables.
- JasperReports para comprobantes y reportes PDF.

## 3. Modelo fiscal

### Timbrado

`timbrados` contiene el número fiscal, inicio y fin de vigencia, empresa y estado.

### Detalle de timbrado

`detalle_timbrado` vincula un timbrado con:

- sucursal o establecimiento;
- punto de expedición;
- serie, si corresponde;
- modo `MANUAL` o `AUTOIMPRESOR`;
- rango autorizado desde/hasta;
- estado.

Un mismo timbrado puede asociarse a distintos establecimientos o puntos, cada uno con su configuración y rango. La aplicación solamente ofrece detalles activos, vigentes, pertenecientes a la sucursal de la sesión y compatibles con el tipo de comprobante seleccionado.

### Clave del comprobante

La identidad fiscal utiliza, en este orden lógico:

1. sucursal;
2. punto de expedición;
3. tipo de comprobante;
4. serie;
5. número de comprobante.

Para autoimpresor sin serie se utiliza internamente el código `0`; la impresión y la foto fiscal presentan la serie como ausente.

### Foto fiscal

Al emitir se copian al comprobante los valores fiscales vigentes:

- establecimiento;
- punto de expedición;
- número de timbrado;
- inicio y fin de vigencia;
- serie, cuando exista.

Estos valores no dependen posteriormente de cambios en las tablas maestras. Así una reimpresión conserva los datos con los que el documento fue emitido.

## 4. Numeración y concurrencia

### Factura manual

El operador ingresa el número. Después de guardar, la pantalla propone el siguiente número local. La clave primaria de `comprobantes` impide registrar dos veces la misma combinación fiscal.

### Autoimpresor

`numeradores_autoimpresor` conserva el último número por timbrado, sucursal, punto, tipo y serie. La reserva se ejecuta dentro de la misma transacción que emite el comprobante y usa `SELECT ... FOR UPDATE`.

Si dos cajas emiten simultáneamente, la segunda espera la liberación del bloqueo y recibe el número siguiente. Si la transacción falla, también se revierte el avance del numerador.

La interfaz deshabilita el botón **Guardar factura** durante la solicitud y descarta envíos adicionales hasta que termine.

## 5. Validaciones principales

- El usuario debe tener una sucursal activa en sesión.
- El timbrado debe pertenecer a la empresa y sucursal correspondientes.
- El detalle debe corresponder al punto seleccionado.
- Timbrado y detalle deben estar activos y dentro de vigencia.
- El modo del detalle debe coincidir con el tipo de comprobante.
- La serie es obligatoria en manual y opcional en autoimpresor.
- El número debe estar entre 1 y 9.999.999.
- El autoimpresor no puede superar el rango autorizado.
- Debe existir al menos un medio de pago válido.
- La suma de los medios de pago debe corresponder al importe.
- Un autoimpreso emitido no puede editarse; debe anularse o corregirse mediante el comprobante fiscal correspondiente cuando esté implementado.

## 6. Auditoría

La tabla `auditoria_comprobantes` registra eventos fiscales y la pantalla `/comprobantes-v2/auditoria` los consulta con DataTables en modo servidor. El acceso queda limitado a la sucursal del usuario.

Eventos relevantes:

- `EMISION`;
- primera impresión;
- reimpresión;
- `ANULACION`;
- intento de modificación rechazado.

La consulta puede filtrarse por número y acción, y presenta fecha, documento, timbrado, serie, usuario y motivo.

## 7. Migraciones

Flyway está habilitado y debe ser el único mecanismo que modifique el esquema en producción. Las migraciones relevantes son:

- `V2__controles_autoimpresor.sql`: modo, rango, numerador y auditoría;
- migraciones posteriores: foto fiscal e índices de comprobantes;
- `V6__serie_opcional_autoimpresor.sql`: permite serie nula en el detalle de autoimpresor.

No ejecutar `flyway clean` en ningún ambiente con datos. Antes de migrar producción se debe realizar respaldo completo y probar la restauración.

## 8. Compilación y verificación

```bash
mvn clean test
mvn clean package -DskipTests
```

Artefacto esperado: `target/recoleccion-1.0.jar`.

Pruebas mínimas de aceptación:

1. Crear o verificar un timbrado activo y vigente.
2. Asociarlo a sucursal, punto, modo y rango autorizados.
3. Emitir una factura manual con número ingresado.
4. Emitir dos autoimpresos simultáneamente y confirmar números consecutivos.
5. Presionar dos veces Guardar y confirmar una sola emisión.
6. Imprimir en A4, 58 mm y 80 mm.
7. Reimprimir y verificar su registro en auditoría.
8. Anular y comprobar estado, motivo y auditoría.
9. Generar el reporte de servicios por categoría en las tres agrupaciones.
10. Confirmar aislamiento de datos entre dos sucursales.

## 9. Criterio de liberación

El módulo puede liberarse únicamente después de completar la lista anterior en una copia representativa de producción y documentar el resultado. Para una operación fiscal completa continúan pendientes la definición e implementación de notas de crédito/débito, retenciones y, si aplica, la homologación o habilitación exigida por la DNIT.
