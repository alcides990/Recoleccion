package ama.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "series")
public class Serie implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "codigo_serie")
    Integer codigoSerie;

    @Column(name = "serie")
    Integer serie;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "serie")
    List<Comprobante> comprobante;

    public Serie() {
    }

    public Serie(Integer codigoSerie) {
        this.codigoSerie = codigoSerie;
    }

}
