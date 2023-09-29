package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "historiaservicio")
public class Historiaservicio implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "codigo_historia")
    private Integer codigoHistoria;
    private String descripcion;
    @JoinColumn(name = "cuenta_corriente", referencedColumnName = "cuenta_corriente", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Servicio servicio;
    @JoinColumn(name = "codigo_usuario", referencedColumnName = "codigo_usuario")
    @ManyToOne(optional = false)
    private Usuario usuario;
}
