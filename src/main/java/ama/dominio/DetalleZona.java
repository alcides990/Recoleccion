package ama.dominio;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import jakarta.persistence.*;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "detallezonas", catalog = "cliba_sa", schema = "")
public class DetalleZona implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected DetalleZonaPK detalleZonaPK;

    @JoinColumn(name = "codigo_cobrador", referencedColumnName = "codigo_cobrador", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Cobrador cobrador;

    @JoinColumn(name = "codigo_zona", referencedColumnName = "codigo_zona", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Zona zona;

    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Sucursal sucursal;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "detalleZona")
    private List<Manzana> manzana;

    public DetalleZona() {
    }

    public DetalleZona(DetalleZonaPK detalleZonaPK) {
        this.detalleZonaPK = detalleZonaPK;
    }

   

}
