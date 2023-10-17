package ama.dominio;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "sucursales")
public class Sucursal implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_sucursal")
    private Integer codigoSucursal;
     @Column(name = "sucursal")
    private String nombreSucursal;
    @JoinColumn(name = "codigo_ciudad", referencedColumnName = "codigo_ciudad")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Ciudad ciudad;
    @JoinColumn(name = "codigo_empresa", referencedColumnName = "codigo_empresa")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Empresa empresa; 

    public Sucursal(Integer codigoSucursal) {
        this.codigoSucursal = codigoSucursal;
    }

    public Sucursal() {
    }

    
    @Override
    public String toString() {
        return "Sucursal{" + "codigoSucursal=" + codigoSucursal + ", nombreSucursal=" + nombreSucursal + '}';
    }
    

}
