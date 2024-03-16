
package ama.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class ComprobanteGuardar {
    private String cuentaCorriente;
    private Integer codigoSerie;
    private Integer codigoTimbrado;
    private Integer numeroComprobante;
    private Integer codigoPuntoExpedicion;
    private Integer codigoTipoFactura;
    private Integer codigoCobrador;
    private Integer codigoCondicionVenta;
    private LocalDate pagoHasta;
    private LocalDate fechaPago;
    private Integer cantidadPago;
    private double recargoPago;
    private double saldoAnterior;

    private List<DetallePago> detallePago;
    private Parametro parametro;
    private Servicio servicio;

    public ComprobanteGuardar(List<DetallePago> detallePagos) {
        this.detallePago = detallePagos;
    }

    public List<DetallePago> getDetallePago() {
        return detallePago;
    }

    public void setDetallePago(List<DetallePago> detallePago) {
        this.detallePago = detallePago;
    }

    public Parametro getParametro() {
        return parametro;
    }

    public void setParametro(Parametro parametro) {
        this.parametro = parametro;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

}
