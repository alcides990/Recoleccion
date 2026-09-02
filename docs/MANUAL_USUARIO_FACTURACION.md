# Manual de usuario de facturación

Versión: 1.0 — 27/08/2026

## 1. Perfiles

- **ROOT / Administrador:** configura timbrados, sucursales, puntos y detalles.
- **Supervisor / Secretario:** emite y consulta comprobantes según sus permisos.
- Todos los datos de facturación y auditoría se limitan a la sucursal del usuario conectado.

## 2. Configurar un timbrado

Esta tarea debe realizarla un administrador con los datos autorizados.

1. Abra **Administración → Timbrados**.
2. Registre el número de timbrado, inicio y fin de vigencia y estado activo.
3. Entre en **Detalles de timbrado**.
4. Seleccione sucursal, punto de expedición y timbrado.
5. Seleccione el modo:
   - **Manual / preimpreso:** la serie es obligatoria y el operador escribe el número.
   - **Autoimpresor:** la serie puede dejarse vacía y el sistema asigna el número.
6. Ingrese exactamente el rango desde/hasta autorizado.
7. Guarde y verifique que el detalle figure activo.

No reutilice el mismo rango para dos configuraciones que representen una única secuencia fiscal.

## 3. Emitir una factura manual

1. Abra **Comprobantes → Registrar/Emitir Comprobante**.
2. Busque la cuenta corriente del usuario.
3. Seleccione **Factura manual**.
4. Seleccione el punto de expedición.
5. El sistema cargará solamente timbrados manuales vigentes para ese punto.
6. Seleccione el timbrado y la serie.
7. Escriba el **N.º de factura** del talonario. El sistema lo completa a siete dígitos.
8. Revise condición de venta, fecha, cobrador, comisión, períodos, recargo y total.
9. Seleccione el método de pago. Si utiliza varios métodos, abra **Configurar** y distribuya el total.
10. Pulse **Guardar factura** una sola vez.

Después del guardado, la pantalla propone localmente el número siguiente. Verifique siempre que coincida con el talonario físico.

## 4. Emitir con autoimpresor

1. Abra **Comprobantes → Registrar/Emitir Comprobante**.
2. Busque la cuenta corriente.
3. Seleccione **Autoimpresor**.
4. Seleccione el punto de expedición.
5. El sistema cargará solamente timbrados autoimpresor vigentes para ese punto.
6. Seleccione el timbrado. Puede mostrar una serie o **Sin serie**, según la autorización.
7. El campo **N.º de factura** queda bloqueado; no debe escribirse manualmente.
8. Complete y verifique los datos de pago.
9. Pulse **Guardar factura**.

Mientras se procesa, el botón muestra **Guardando…** y permanece deshabilitado. Al finalizar se informa el número fiscal asignado.

## 5. Consultar e imprimir

1. Abra **Comprobantes → Consultar Comprobantes**.
2. Utilice los filtros de la tabla para localizar el documento.
3. Abra el detalle y confirme receptor, importe, timbrado y numeración.
4. Pulse imprimir y elija:
   - ticket 58 mm;
   - ticket 80 mm;
   - A4.

La primera impresión y las reimpresiones quedan registradas. La reimpresión usa la foto fiscal guardada al emitir, aunque posteriormente cambie el maestro del timbrado.

## 6. Anular un comprobante

1. Localice el comprobante en **Consultar Comprobantes**.
2. Pulse **Anular**.
3. Verifique el documento mostrado.
4. Escriba un motivo claro y confirme.

No intente corregir un autoimpreso editándolo. La anulación queda registrada en auditoría. Nota de crédito/débito todavía no está disponible en esta versión.

## 7. Revisar la auditoría

1. Abra **Consultar Comprobantes**.
2. Pulse **Auditoría**.
3. Filtre opcionalmente por número o acción.
4. Revise fecha, acción, documento, timbrado, serie, usuario y motivo.

La tabla es de solo lectura, está paginada en el servidor y muestra únicamente registros de la sucursal de la sesión.

## 8. Reporte de servicios por categoría

1. Abra **Reportes**.
2. Seleccione **Servicios por categoría**.
3. Elija la agrupación:
   - **General**: un resumen total por categoría;
   - **Por zona**: cada zona con sus categorías;
   - **Por cobrador**: cada cobrador con sus categorías.
4. Pulse **Generar reporte**.

Cada categoría muestra tarifa, cantidad de servicios y total calculado como `tarifa × cantidad`.

## 9. Mensajes frecuentes

### “Sin timbrado vigente para este punto y tipo”

No existe una asociación activa y vigente para la sucursal, punto y modo seleccionados. Un administrador debe revisar el detalle del timbrado.

### “El timbrado seleccionado no corresponde al tipo de comprobante”

La configuración no coincide con Manual o Autoimpresor. Vuelva a seleccionar el tipo y el timbrado.

### “Se agotó el rango autorizado”

No continúe facturando con ese detalle. Avise al administrador para cargar una autorización válida; nunca amplíe el rango sin respaldo fiscal.

### El número manual no se puede escribir

Confirme que el tipo sea **Factura manual**, espere la carga del timbrado y actualice la página con `Ctrl+F5` si se desplegó una versión nueva.

### El botón permanece en “Guardando…”

No recargue inmediatamente. Espere la respuesta y luego consulte Comprobantes antes de repetir la operación, para confirmar si fue registrada.

## 10. Cierre diario recomendado

1. Compare la última numeración utilizada con los comprobantes registrados.
2. Revise anulaciones y reimpresiones en Auditoría.
3. Verifique los totales por medio de pago.
4. Registre cualquier salto, error de impresión o incidente.
5. Confirme que el respaldo diario haya finalizado correctamente.

