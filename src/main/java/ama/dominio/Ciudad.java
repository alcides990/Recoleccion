package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "ciudades")
 
public class Ciudad implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_ciudad")
    private Integer codigoCiudad;
    @Column(name = "ciudad")
    private String nombreCiudad;

}
