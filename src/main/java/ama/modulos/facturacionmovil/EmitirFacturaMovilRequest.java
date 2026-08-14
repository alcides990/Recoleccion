package ama.modulos.facturacionmovil;

import java.util.List;
import lombok.Data;

/** Datos seleccionados por el cobrador; importes y datos del contribuyente se recalculan en servidor. */
@Data
public class EmitirFacturaMovilRequest {
    private String cuentaCorriente;
    private Integer codigoPuntoExpedicion;
    private Integer codigoTimbrado;
    private Integer codigoSerie;
    private Integer codigoTipoComprobante;
    private Integer codigoCobrador;
    private Integer codigoComision;
    private Integer codigoCondicionVenta;
    private Integer codigoMetodoPago;
    private Integer cantidadPago;
    private Double recargo;
    private List<PagoMovilRequest> pagos;

    @Data
    public static class PagoMovilRequest {
        private Integer codigoMetodoPago;
        private Double importe;
    }
}
