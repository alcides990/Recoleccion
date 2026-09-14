package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "detalle_pago")
public class DetallePago implements Serializable {

    @EmbeddedId
    DetallePagoPK detallePagoPK;
    Double importe;
    
    @JoinColumn(name = "codigo_metodo_pago", referencedColumnName = "codigo_metodo_pago", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private MetodoPago metodoPago;
   
}
