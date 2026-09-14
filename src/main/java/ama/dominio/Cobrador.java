package ama.dominio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "cobradores")
public class Cobrador implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_cobrador")
    private Integer codigoCobrador;
    @NotEmpty
    private String nombre;
    private String apellido;
    private String celular;
    private String direccion;
    @Transient
    private String nombreCompleto;
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonIgnore
    private Estado estado;
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonIgnore
    private Sucursal sucursal;

    public Cobrador() {
    }

    public Cobrador(Integer codigoCobrador) {
        this.codigoCobrador = codigoCobrador;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    @Override
    public String toString() {
        return "Cobrador{" + "codigoCobrador=" + codigoCobrador + ", nombre=" + nombre + ", apellido=" + apellido + ", celular=" + celular + ", direccion=" + direccion + ", nombreCompleto=" + nombreCompleto + '}';
    }
}
