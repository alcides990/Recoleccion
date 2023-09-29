 
package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;
@Data
@Entity
@Table(name = "comisiones")
public class Comision implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_comision")
    private Integer codigoComision;
    @Column(name = "nombre_comision")
    private String nombreComision;
    private String comision;

    public Comision() {
    }

    public Comision(Integer codigoComision) {
        this.codigoComision = codigoComision;
    }
   
    
}
