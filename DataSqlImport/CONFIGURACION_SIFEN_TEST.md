# Configuración pendiente para SIFEN TEST

La infraestructura de almacenamiento, envío, respuesta, auditoría y KUDE ya está implementada.
Antes de habilitar la generación automática del Documento Electrónico se deben confirmar estos datos fiscales.

## Credenciales y seguridad

- Archivo de certificado cliente PFX.
- Contraseña del PFX.
- CSC de pruebas y su identificador.
- Mantener todos estos valores fuera del JAR y de Git.

## Datos obligatorios del emisor/establecimiento

- RUC y dígito verificador.
- Razón social y nombre de fantasía.
- Tipo de contribuyente.
- Código de actividad económica y descripción.
- Número de establecimiento SIFEN.
- Código de departamento, distrito y ciudad conforme al catálogo SIFEN.
- Dirección, número de casa, teléfono y correo.
- Tipo de documento electrónico y tipo de transacción.
- Moneda y condición de cambio.
- Código de producto/servicio y unidad de medida.
- Afectación y tasa de IVA aplicable al servicio de recolección.

## Variables del servicio

```properties
SIFEN_ENABLED=false
SIFEN_AMBIENTE=TEST
SIFEN_STORAGE_ROOT=/opt/recoleccion/documentos/factura-electronica
SIFEN_CERTIFICADO=/etc/recoleccion/sifen/certificado-test.pfx
SIFEN_CERTIFICADO_PASSWORD=CAMBIAR
SIFEN_CSC_ID=0001
SIFEN_CSC=CAMBIAR
```

No activar `SIFEN_ENABLED=true` hasta cargar los datos fiscales anteriores y validar un DE contra los XSD del ambiente TEST.

## Permisos del servidor

El usuario `recoleccion` necesita lectura sobre el PFX y escritura sobre
`/opt/recoleccion/documentos`. El PFX no debe estar dentro del directorio público ni del JAR.

