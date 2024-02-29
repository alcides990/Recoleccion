 
package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ComprobantePK implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Numero comprobante no puede ser nulo!!")
    @Min(value = 1, message = "Numero de comprobante debe ser mayor que cero!!")
    @Max(value = 9999999, message = "Numero de comprobante debe ser menor o igual a 9.999.999 !!")
    @Basic(optional = false)
    @Column(name = "numero_comprobante", nullable = false)
    private Integer numeroComprobante;
    
    @Basic(optional = false)
    @Column(name = "codigo_sucursal", nullable = false)
    private Integer codigoSucursal;
    
    @Basic(optional = false)
    @Column(name = "codigo_punto_expedicion", nullable = false)
    private Integer codigoPuntoExpedicion;
    
    @Basic(optional = false)
    @Column(name = "codigo_tipo_factura", nullable = false)
    private Integer codigoTipoFactura;
    
    @Basic(optional = false)
    @Column(name = "codigo_serie", nullable = false)
    private Integer codigoSerie;

}
