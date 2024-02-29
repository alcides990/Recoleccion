package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.Data;

@Data
@Entity
@Table(name = "manzanas")
public class Manzana implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ManzanaPK manzanaPK;

    @JoinColumn(name = "codigo_zona", referencedColumnName = "codigo_zona", nullable = false, insertable = true)
    @ManyToOne(optional = false, fetch = FetchType.LAZY )
    private Zona zona;
    
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Sucursal sucursal;

    public Manzana(ManzanaPK manzanaPK) {
        this.manzanaPK = manzanaPK;
    }

    public Manzana() {
    }

    @Override
    public String toString() {
        return "Manzana{" + "manzanaPK=" + manzanaPK + ", zona=" + zona + ", sucursal=" + sucursal + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Manzana other = (Manzana) obj;
        return Objects.equals(this.manzanaPK, other.manzanaPK);
    }
    
    
   
}
