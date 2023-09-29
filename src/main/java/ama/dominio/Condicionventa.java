 
package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "condicionventa")
public class Condicionventa implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_condicion_venta")
    private Integer codigoCondicionVenta;
    @Column(name = "condicion_venta")
    private String condicionVenta;

    public Condicionventa() {
    }

    public Condicionventa(Integer codigoCondicionVenta) {
        this.codigoCondicionVenta = codigoCondicionVenta;
    }
     
    
}
