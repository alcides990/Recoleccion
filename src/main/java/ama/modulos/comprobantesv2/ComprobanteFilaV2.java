package ama.modulos.comprobantesv2;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ComprobanteFilaV2 {
    private Integer codigoSucursal;
    private Integer codigoPuntoExpedicion;
    private Integer codigoTipoComprobante;
    private Integer codigoSerie;
    private Integer numeroComprobante;
    private String numeroFiscal;
    private String serie;
    private String tipoComprobante;
    private String puntoExpedicion;
    private String cuentaCorriente;
    private String receptor;
    private LocalDate fechaPago;
    private BigDecimal tarifa;
    private Integer cantidadPago;
    private BigDecimal recargo;
    private BigDecimal importe;
    private BigDecimal saldo;
    private String estado;
}
