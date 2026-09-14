package ama.DTO;

import ama.dominio.Categoria;
import ama.dominio.Cobrador;
import ama.dominio.CondicionVenta;
import ama.dominio.Estado;
import ama.dominio.Serie;
import ama.dominio.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private TipoComprobante tipoComprobante;
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
    private Estado estado;
    private String motivoAnulacion;

    public ComprobanteDTO() {
        this.detallePago = new ArrayList<>();
    }

    public void DetallePagoAdd(DetallePagoDTO detallePagoDTO) {
        this.detallePago.add(detallePagoDTO);
    }

}
