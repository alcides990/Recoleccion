package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipos_comprobante")
public class TipoComprobante implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "codigo_tipo_comprobante")
    private Integer codigoTipoComprobante;
    @Column(name = "tipo_comprobante")
    private String nombreTipoComprobante;

    public TipoComprobante() {
    }

    public TipoComprobante(Integer codigoTipoComprobante) {
        this.codigoTipoComprobante = codigoTipoComprobante;
    }

    public TipoComprobante(Integer codigonombreTipoComprobante, String tipoComprobante) {
        this.codigoTipoComprobante = codigonombreTipoComprobante;
        this.nombreTipoComprobante = tipoComprobante;
    }

}
