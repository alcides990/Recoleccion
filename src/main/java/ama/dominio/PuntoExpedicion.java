
package ama.dominio;

import ama.dominio.Estado;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "puntos_expedicion")
public class PuntoExpedicion implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PuntoExpedicionPK puntoExpedicionPK;

    @Column(name = "punto_expedicion")
    private String nombrePuntoExpedicion;

    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinColumn(name = "codigo_empresa", referencedColumnName = "codigo_empresa")
    @ManyToOne(fetch = FetchType.LAZY)
    private Empresa empresa;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado")
    @ManyToOne(fetch = FetchType.LAZY)
    private Estado estado;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Sucursal sucursal;

    public PuntoExpedicion(PuntoExpedicionPK puntoExpedicionPK) {
        this.puntoExpedicionPK = puntoExpedicionPK;
    }

}
