package ama.DTO;

import ama.dominio.Categoria;
import ama.dominio.Cobrador;
import ama.dominio.CondicionVenta;
import ama.dominio.Serie;
import ama.dominio.TipoFactura;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ComprobanteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private TipoFactura tipoFactura;
    private CondicionVenta condicionVenta;
    @ToString.Exclude
    List<DetallePagoDTO> detallePago;
    private Serie serie;
    private Integer codigoSucursal;
    private String sucursal;
    private Integer codigoPuntoExpedicion;
    private String puntoExpedicion;
    private Integer numeroComprobante;
    private String cuentaCorriente;
    private String numeroDocumento;
    private Categoria categoria;
    private Cobrador cobrador;
    private String nombreUsuario;
    private LocalDate fechaPago;
    private String periodoPago;
    private int cantidadPago;
    private double tarifa;
    private double recargo;
    private double saldo;
    private double importe;
    private String estado;

    public ComprobanteDTO() {
        this.detallePago = new ArrayList<>();
    }

    public void DetallePagoAdd(DetallePagoDTO detallePagoDTO) {
        this.detallePago.add(detallePagoDTO);
    }

}
