package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipo_factura")
public class TipoFactura implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "codigo_tipo_factura")
    private Integer codigoTipoFactura;
    @Column(name = "tipo_factura")
    private String tipoFactura;

    public TipoFactura() {
    }

    public TipoFactura(Integer codigoTipoFactura) {
        this.codigoTipoFactura = codigoTipoFactura;
    }

    public TipoFactura(Integer codigoTipoFactura, String tipoFactura) {
        this.codigoTipoFactura = codigoTipoFactura;
        this.tipoFactura = tipoFactura;
    }

}
