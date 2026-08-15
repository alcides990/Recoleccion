package ama.facturaelectronica;

import ama.dominio.Comprobante;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacturaElectronicaService {
    private final JdbcTemplate jdbc;
    private final SifenProperties properties;
    private final AlmacenDocumentosSifen almacen;

    public FacturaElectronicaService(JdbcTemplate jdbc, SifenProperties properties,
            AlmacenDocumentosSifen almacen) {
        this.jdbc = jdbc; this.properties = properties; this.almacen = almacen;
    }

    @Transactional
    public void registrar(Comprobante comprobante) {
        ClaveComprobanteElectronico clave = ClaveComprobanteElectronico.desde(comprobante);
        String estado = properties.configuracionCompleta() ? "PENDIENTE_GENERACION" : "PENDIENTE_CONFIGURACION";
        jdbc.update("""
            INSERT IGNORE INTO factura_electronica_documentos
              (codigo_sucursal,codigo_punto_expedicion,codigo_tipo_comprobante,codigo_serie,
               numero_comprobante,ambiente,estado)
            VALUES (?,?,?,?,?,?,?)
            """, clave.sucursal(), clave.puntoExpedicion(), clave.tipoComprobante(), clave.serie(),
                clave.numero(), properties.getAmbiente(), estado);
        Long codigo = codigo(clave);
        if (codigo != null) jdbc.update("""
            INSERT INTO factura_electronica_eventos
              (codigo_documento,tipo_evento,estado_nuevo,detalle) VALUES (?,?,?,?)
            """, codigo, "REGISTRO", estado,
                properties.configuracionCompleta() ? "Documento preparado para generación" : "SIFEN aún no configurado");
    }

    public Map<String,Object> estado(ClaveComprobanteElectronico clave) {
        return jdbc.queryForMap("""
            SELECT codigo_documento AS codigoDocumento, cdc, ambiente, estado,
                   codigo_respuesta AS codigoRespuesta, mensaje_respuesta AS mensajeRespuesta,
                   protocolo_autorizacion AS protocoloAutorizacion,
                   cantidad_intentos AS cantidadIntentos, fecha_creacion AS fechaCreacion,
                   fecha_ultimo_intento AS fechaUltimoIntento, fecha_aprobacion AS fechaAprobacion,
                   ultimo_error AS ultimoError,
                   ruta_xml_firmado IS NOT NULL AS tieneXml,
                   ruta_respuesta IS NOT NULL AS tieneRespuesta,
                   ruta_kude IS NOT NULL AS tieneKude
              FROM factura_electronica_documentos
             WHERE codigo_sucursal=? AND codigo_punto_expedicion=? AND codigo_tipo_comprobante=?
               AND codigo_serie=? AND numero_comprobante=?
            """, parametros(clave));
    }

    @Transactional
    public ResultadoEnvio enviarXml(ClaveComprobanteElectronico clave, String xml) {
        Long codigo = codigo(clave);
        if (codigo == null) return new ResultadoEnvio(false, "El comprobante no está registrado para SIFEN");
        if (!properties.configuracionCompleta()) return new ResultadoEnvio(false, "Complete la configuración SIFEN TEST y habilite SIFEN_ENABLED");
        try {
            var generado = almacen.guardar(clave, "de-generado.xml", xml.getBytes(StandardCharsets.UTF_8));
            var config = new com.roshka.sifen.core.SifenConfig(
                    "PROD".equalsIgnoreCase(properties.getAmbiente())
                            ? com.roshka.sifen.core.SifenConfig.TipoAmbiente.PROD
                            : com.roshka.sifen.core.SifenConfig.TipoAmbiente.DEV,
                    properties.getCscId(), properties.getCsc(),
                    com.roshka.sifen.core.SifenConfig.TipoCertificadoCliente.PFX,
                    properties.getCertificado(), properties.getCertificadoPassword());
            var documento = new com.roshka.sifen.core.beans.DocumentoElectronico(xml);
            String cdc = documento.obtenerCDC();
            var respuesta = com.roshka.sifen.Sifen.recepcionDE(documento, config);
            String firmado = respuesta.getRequestSent() == null ? xml : respuesta.getRequestSent();
            var archivoFirmado = almacen.guardar(clave, "de-firmado.xml", firmado.getBytes(StandardCharsets.UTF_8));
            String respuestaXml = respuesta.getRespuestaBruta() == null ? "" : respuesta.getRespuestaBruta();
            var archivoRespuesta = almacen.guardar(clave, "respuesta-sifen.xml", respuestaXml.getBytes(StandardCharsets.UTF_8));
            String estado = respuesta.getxProtDE() != null && "Aprobado".equalsIgnoreCase(respuesta.getxProtDE().getdEstRes())
                    ? "APROBADO" : "RECHAZADO";
            String protocolo = respuesta.getxProtDE() == null ? null : respuesta.getxProtDE().getdProtAut();
            jdbc.update("""
                UPDATE factura_electronica_documentos SET cdc=?,estado=?,codigo_respuesta=?,
                  mensaje_respuesta=?,protocolo_autorizacion=?,ruta_xml_generado=?,ruta_xml_firmado=?,
                  ruta_respuesta=?,hash_xml_firmado=?,hash_respuesta=?,cantidad_intentos=cantidad_intentos+1,
                  fecha_ultimo_intento=?,fecha_aprobacion=IF(?='APROBADO',?,NULL),ultimo_error=NULL
                WHERE codigo_documento=?
                """, cdc, estado, respuesta.getdCodRes(), respuesta.getdMsgRes(), protocolo,
                    generado.ruta(), archivoFirmado.ruta(), archivoRespuesta.ruta(), archivoFirmado.sha256(),
                    archivoRespuesta.sha256(), LocalDateTime.now(), estado, LocalDateTime.now(), codigo);
            evento(codigo, "ENVIO_SIFEN", "ENVIANDO", estado, respuesta.getdMsgRes());
            if ("APROBADO".equals(estado)) {
                generarKude(clave, codigo, cdc, estado, documento.getEnlaceQR());
            }
            return new ResultadoEnvio("APROBADO".equals(estado), respuesta.getdMsgRes());
        } catch (Exception ex) {
            jdbc.update("""
                UPDATE factura_electronica_documentos SET estado='ERROR_ENVIO',
                  cantidad_intentos=cantidad_intentos+1,fecha_ultimo_intento=?,ultimo_error=?
                WHERE codigo_documento=?
                """, LocalDateTime.now(), mensaje(ex), codigo);
            evento(codigo, "ERROR_ENVIO", null, "ERROR_ENVIO", mensaje(ex));
            return new ResultadoEnvio(false, mensaje(ex));
        }
    }

    public byte[] documento(ClaveComprobanteElectronico clave, String tipo) throws Exception {
        String columna = switch (tipo) {
            case "xml" -> "ruta_xml_firmado"; case "respuesta" -> "ruta_respuesta";
            case "kude" -> "ruta_kude"; default -> throw new IllegalArgumentException("Tipo no válido");
        };
        String ruta = jdbc.queryForObject("SELECT " + columna + " FROM factura_electronica_documentos WHERE codigo_documento=?",
                String.class, codigo(clave));
        if (ruta == null) throw new IllegalStateException("El documento aún no está disponible");
        return almacen.leer(ruta);
    }

    private void evento(Long codigo, String tipo, String anterior, String nuevo, String detalle) {
        jdbc.update("INSERT INTO factura_electronica_eventos(codigo_documento,tipo_evento,estado_anterior,estado_nuevo,detalle) VALUES(?,?,?,?,?)",
                codigo, tipo, anterior, nuevo, detalle);
    }
    private void generarKude(ClaveComprobanteElectronico clave, Long codigo, String cdc,
            String estado, String enlaceQr) throws Exception {
        Map<String,Object> dato = jdbc.queryForMap("""
            SELECT e.razon_social emisorRazonSocial,e.ruc emisorRuc,su.direccion emisorDireccion,
                   COALESCE(su.telefono,su.celular,'') emisorTelefono,t.numero_timbrado timbrado,
                   DATE_FORMAT(t.fecha_inicio,'%d/%m/%Y') vigenciaTimbrado,
                   DATE_FORMAT(c.fecha_emision,'%d/%m/%Y %H:%i') fechaEmision,
                   c.razon_social receptorNombre,u.numero_documento receptorDocumento,
                   COALESCE(u.direccion,'') receptorDireccion,cv.condicion_venta condicionVenta,
                   COALESCE(c.periodo_pago,'') periodo,c.cantidad_pago cantidad,c.tarifa precioUnitario,
                   c.total_importe total
              FROM comprobantes c JOIN sucursales su ON su.codigo_sucursal=c.codigo_sucursal
              JOIN empresas e ON e.codigo_empresa=su.codigo_empresa
              JOIN timbrados t ON t.codigo_timbrado=c.codigo_timbrado
              JOIN usuarios u ON u.codigo_usuario=c.codigo_usuario
              JOIN condiciones_venta cv ON cv.codigo_condicion_venta=c.codigo_condicion_venta
             WHERE c.codigo_sucursal=? AND c.codigo_punto_expedicion=? AND c.codigo_tipo_comprobante=?
               AND c.codigo_serie=? AND c.numero_comprobante=?
            """, parametros(clave));
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("es", "PY"));
        Map<String,Object> p = new HashMap<>();
        dato.forEach((k,v) -> p.put(k, v == null ? "" : String.valueOf(v)));
        p.put("numeroDocumento", "%03d-%03d-%07d".formatted(clave.sucursal(),clave.puntoExpedicion(),clave.numero()));
        p.put("descripcion", "Servicio de recolección");
        p.put("gravada10", nf.format(((Number)dato.get("total")).doubleValue() / 1.1));
        p.put("iva10", nf.format(((Number)dato.get("total")).doubleValue() / 11));
        p.put("total", nf.format(((Number)dato.get("total")).doubleValue()));
        p.put("cdc", cdc); p.put("estadoSifen", estado);
        BufferedImage qr = null;
        if (enlaceQr != null && !enlaceQr.isBlank()) {
            var matriz = new com.google.zxing.qrcode.QRCodeWriter().encode(enlaceQr,
                    com.google.zxing.BarcodeFormat.QR_CODE, 300, 300);
            qr = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(matriz);
        }
        p.put("qrImage", qr);
        try (InputStream plantilla = new ClassPathResource("reportes/kudeFacturaElectronica.jrxml").getInputStream()) {
            var reporte = JasperCompileManager.compileReport(plantilla);
            var impresion = JasperFillManager.fillReport(reporte, p, new JREmptyDataSource());
            byte[] pdf = JasperExportManager.exportReportToPdf(impresion);
            var guardado = almacen.guardar(clave, "kude.pdf", pdf);
            jdbc.update("UPDATE factura_electronica_documentos SET ruta_kude=?,hash_kude=? WHERE codigo_documento=?",
                    guardado.ruta(), guardado.sha256(), codigo);
            evento(codigo, "KUDE_GENERADO", estado, estado, "KUDE PDF y QR almacenados");
        }
    }
    private Long codigo(ClaveComprobanteElectronico c) {
        var lista = jdbc.query("SELECT codigo_documento FROM factura_electronica_documentos WHERE codigo_sucursal=? AND codigo_punto_expedicion=? AND codigo_tipo_comprobante=? AND codigo_serie=? AND numero_comprobante=?",
                (rs,n) -> rs.getLong(1), parametros(c));
        return lista.isEmpty() ? null : lista.get(0);
    }
    private Object[] parametros(ClaveComprobanteElectronico c) { return new Object[]{c.sucursal(),c.puntoExpedicion(),c.tipoComprobante(),c.serie(),c.numero()}; }
    private String mensaje(Throwable ex) { Throwable c=ex; while(c.getCause()!=null)c=c.getCause(); return c.getMessage()==null?c.getClass().getSimpleName():c.getMessage(); }
    public record ResultadoEnvio(boolean aprobado, String mensaje) {}
}
