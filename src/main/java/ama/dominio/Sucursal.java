package ama.dominio;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Entity
@Table(name = "sucursales")
public class Sucursal implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_sucursal")
    private Integer codigoSucursal;
    @Column(name = "sucursal")
    private String nombreSucursal;
    private String celular;
    private String telefono;
    private String direccion;

    @JoinColumn(name = "codigo_ciudad", referencedColumnName = "codigo_ciudad")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private Ciudad ciudad;

    @JoinColumn(name = "codigo_empresa", referencedColumnName = "codigo_empresa")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private Empresa empresa;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToOne(mappedBy = "sucursal", fetch = FetchType.LAZY)
    private Parametro parametro;

    public Sucursal(Integer codigoSucursal) {
        this.codigoSucursal = codigoSucursal;
    }

    public Sucursal() {
    }

}
