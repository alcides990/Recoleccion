package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class DetallePagoPK implements Serializable {
 private static final long serialVersionUID = 1L;
 
    @Basic(optional = false)
    @Column(name = "codigo_metodo_pago")
    private Integer codigoMetodoPago;

    private PuntoExpedicionPK puntoExpedicionPK;

    @Basic(optional = false)
    @Column(name = "numero_comprobante")
    private Integer numeroComprobante;

    @Basic(optional = false)
    @Column(name = "codigo_tipo_factura")
    private Integer codigoTioFactura;

    @Basic(optional = false)
    @Column(name = "codigo_serie")
    private Integer codigoSerie;

}
