package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "roles")
public class Rol implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "codigo_rol")
    private Integer codigoRol;
    @Column(name = "rol")
    private String nombre;

    public Rol() {
    }

    public Rol(Integer codigoRol) {
        this.codigoRol = codigoRol;
    }

}
