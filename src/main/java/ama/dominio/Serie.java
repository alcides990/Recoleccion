package ama.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
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
    String serie;

    public Serie() {
    }

    public Serie(Integer codigoSerie) {
        this.codigoSerie = codigoSerie;
    }

}
