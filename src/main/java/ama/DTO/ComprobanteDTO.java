package ama.DTO;

import ama.dominio.Categoria;
import ama.dominio.Cobrador;
import ama.dominio.CondicionVenta;
import ama.dominio.PuntoExpedicion;
import ama.dominio.Serie;
import ama.dominio.Sucursal;
import ama.dominio.TipoFactura;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

@Data
public class ComprobanteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Sucursal sucursal;
    private PuntoExpedicion puntoExpedicion;
    private TipoFactura tipoFactura;
    private CondicionVenta condicionVenta;
    private Serie serie;
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
    private double importe;
    private String estado;

}
