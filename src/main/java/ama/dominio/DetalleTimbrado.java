package ama.dominio;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "detalle_timbrado")
public class DetalleTimbrado implements Serializable {

    @EmbeddedId
    private DetalleTimbradoPK detalleTimbradoPK;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_timbrado", insertable = false, updatable = false)
    private Timbrado timbrado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "codigo_punto_expedicion", referencedColumnName = "codigo_punto_expedicion", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = false, updatable = false)
    })
    private PuntoExpedicion puntoExpedicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_serie", nullable = false)
    private Serie serie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_estado", nullable = false)
    private Estado estado;
}
