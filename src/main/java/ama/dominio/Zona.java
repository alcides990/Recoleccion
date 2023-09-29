  
package ama.dominio;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "zonas", catalog = "cliba_sa", schema = "")
public class Zona implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_zona", nullable = false)
    private Integer codigoZona;
    @Basic(optional = false)
    @Column(name = "zona", nullable = false, length = 50)
    private String nombreZona;
    
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Sucursal sucursal;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "zona", fetch = FetchType.LAZY)
    private List<DetalleZona> detalleZona;

    public Zona() {
    }

    public Zona(Integer codigoZona) {
        this.codigoZona = codigoZona;
    }

    @Override
    public String toString() {
        return "Zona{" + "codigoZona=" + codigoZona + ", nombreZona=" + nombreZona + '}';
    }

  

    
    
}
