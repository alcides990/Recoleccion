package ama.modulos.comprobantesv2;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import lombok.Data;

@Data
public class ComprobanteAccionV2 {
    @NotNull private Integer codigoSucursal;
    @NotNull private Integer codigoPuntoExpedicion;
    @NotNull private Integer codigoTipoComprobante;
    @NotNull private Integer codigoSerie;
    @NotNull private Integer numeroComprobante;
    @Min(1) @Max(9999999) private Integer nuevoNumeroComprobante;
    @Size(max = 45) private String cuentaCorriente;
    private LocalDate fechaPago;
    private LocalDate pagoDesde;
    private Integer codigoEstado;
    private Integer codigoCobrador;
    private Integer codigoCategoria;
    private Integer codigoCondicionVenta;
    @Size(max = 255) private String razonSocial;
    @Size(max = 100) private String periodoPago;
    private Integer cantidadDeuda;
    @Min(0) private Integer cantidadPago;
    @DecimalMin("0") private BigDecimal tarifa;
    @DecimalMin("0") private BigDecimal recargo;
    @DecimalMin("0") private BigDecimal totalImporte;
    @DecimalMin("0") private BigDecimal saldo;
    private List<@Valid DetallePagoV2> pagos;
    @Size(max = 500) private String observacion;
}
