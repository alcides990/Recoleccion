package ama.modulos.comprobantesv2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ComprobanteDetalleV2 {
    private String tipoComprobante;
    private String puntoExpedicion;
    private String serie;
    private Integer numeroComprobante;
    private String numeroFiscal;
    private String cuentaCorriente;
    private Integer codigoUsuario;
    private String receptor;
    private String documento;
    private LocalDateTime fechaEmision;
    private LocalDate fechaPago;
    private LocalDate pagoDesde;
    private String periodoPago;
    private Integer cantidadDeuda;
    private Integer cantidadPago;
    private BigDecimal tarifa;
    private BigDecimal recargo;
    private BigDecimal importe;
    private BigDecimal saldo;
    private String estado;
    private Integer codigoEstado;
    private String cobrador;
    private Integer codigoCobrador;
    private String categoria;
    private Integer codigoCategoria;
    private String condicionVenta;
    private Integer codigoCondicionVenta;
    private String observacion;
    private List<DetallePagoV2> pagos;
}
