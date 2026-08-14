package ama.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DetalleTimbradoPK implements Serializable {

    @Column(name = "codigo_timbrado")
    private Integer codigoTimbrado;

    @Column(name = "codigo_punto_expedicion")
    private Integer codigoPuntoExpedicion;

    @Column(name = "codigo_sucursal")
    private Integer codigoSucursal;
}
