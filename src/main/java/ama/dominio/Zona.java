package ama.dominio;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "zonas")
public class Zona implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_zona", nullable = false)
    private Integer codigoZona;
    @Basic(optional = false)
    @Column(name = "zona", nullable = false, length = 50)
    private String nombreZona;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JoinColumn(name = "codigo_cobrador", referencedColumnName = "codigo_cobrador", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Cobrador cobrador;

    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", nullable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Sucursal sucursal;

    public Zona() {
    }

    public Zona(Integer codigoZona) {
        this.codigoZona = codigoZona;
    }

   

}
