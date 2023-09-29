 
package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Embeddable
public class DetalleZonaPK implements Serializable {

    @Basic(optional = false)
    @Column(name = "codigo_cobrador")
    private int codigoCobrador;
    
    @Basic(optional = false)
    @Column(name = "codigo_zona")
    private int codigoZona;
    
    @Basic(optional = false)
    @Column(name = "codigo_sucursal")
    private int codigoSucursal;

  
    

    
}
