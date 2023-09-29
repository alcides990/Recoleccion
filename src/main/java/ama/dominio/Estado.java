package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "estados")
public class Estado implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_estado")
    private Integer codigoEstado;
    private String estado;

    public Estado() {
    }

    public Estado(Integer codigoEstado) {
        this.codigoEstado = codigoEstado;
    }
    
    
     
}
