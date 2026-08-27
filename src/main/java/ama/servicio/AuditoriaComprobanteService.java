package ama.servicio;

import ama.dominio.ComprobantePK;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaComprobanteService {

    private final JdbcTemplate jdbc;

    public AuditoriaComprobanteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String accion, ComprobantePK clave, Integer usuarioSistema, String motivo) {
        jdbc.update("""
            INSERT INTO auditoria_comprobantes
                (accion, numero_comprobante, codigo_punto_expedicion, codigo_sucursal,
                 codigo_tipo_comprobante, codigo_serie, codigo_timbrado,
                 codigo_usuario_sistema, motivo, datos_comprobante)
            SELECT ?, c.numero_comprobante, c.codigo_punto_expedicion, c.codigo_sucursal,
                   c.codigo_tipo_comprobante, c.codigo_serie, c.codigo_timbrado,
                   ?, ?, JSON_OBJECT(
                       'razonSocial', c.razon_social,
                       'fechaEmision', c.fecha_emision,
                       'fechaPago', c.fecha_pago,
                       'cuentaCorriente', c.cuenta_corriente,
                       'usuario', c.codigo_usuario,
                       'categoria', c.codigo_categoria,
                       'condicionVenta', c.codigo_condicion_venta,
                       'cantidadPago', c.cantidad_pago,
                       'tarifa', c.tarifa,
                       'recargo', c.recargo,
                       'saldo', c.saldo,
                       'totalImporte', c.total_importe,
                       'periodoPago', c.periodo_pago,
                       'pagoHasta', c.pago_hasta,
                       'establecimientoFiscal', c.establecimiento_fiscal,
                       'puntoExpedicionFiscal', c.punto_expedicion_fiscal,
                       'numeroTimbradoFiscal', c.numero_timbrado_fiscal,
                       'inicioVigenciaFiscal', c.inicio_vigencia_fiscal,
                       'finVigenciaFiscal', c.fin_vigencia_fiscal,
                       'serieFiscal', c.serie_fiscal,
                       'estado', c.codigo_estado,
                       'observacion', c.obs)
              FROM comprobantes c
             WHERE c.numero_comprobante = ? AND c.codigo_punto_expedicion = ?
               AND c.codigo_sucursal = ? AND c.codigo_tipo_comprobante = ?
               AND c.codigo_serie = ?
            """, accion, usuarioSistema, normalizarMotivo(motivo), clave.getNumeroComprobante(),
                clave.getPuntoExpedicionPK().getCodigoPuntoExpedicion(),
                clave.getPuntoExpedicionPK().getCodigoSucursal(), clave.getCodigoTipoComprobante(),
                clave.getCodigoSerie());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String registrarImpresion(ComprobantePK clave, Integer usuarioSistema, String formato) {
        String accion = esPrimeraImpresion(clave) ? "IMPRESION" : "REIMPRESION";
        registrar(accion, clave, usuarioSistema, "Formato " + formato);
        return accion;
    }

    public boolean esPrimeraImpresion(ComprobantePK clave) {
        Integer impresiones = jdbc.queryForObject("""
            SELECT COUNT(*) FROM auditoria_comprobantes
             WHERE numero_comprobante = ? AND codigo_punto_expedicion = ?
               AND codigo_sucursal = ? AND codigo_tipo_comprobante = ?
               AND codigo_serie = ? AND accion IN ('IMPRESION', 'REIMPRESION')
            """, Integer.class, clave.getNumeroComprobante(),
                clave.getPuntoExpedicionPK().getCodigoPuntoExpedicion(),
                clave.getPuntoExpedicionPK().getCodigoSucursal(), clave.getCodigoTipoComprobante(),
                clave.getCodigoSerie());
        return impresiones == null || impresiones == 0;
    }

    private String normalizarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) return null;
        String limpio = motivo.trim();
        return limpio.length() <= 500 ? limpio : limpio.substring(0, 500);
    }
}
