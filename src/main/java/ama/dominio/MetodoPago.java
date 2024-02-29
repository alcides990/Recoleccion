package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "metodos_pago")
public class MetodoPago implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_metodo_pago")
    private Integer codigoMetodoPago;
    @Column(name = "metodo_pago")
    private String metodoPago;

    public MetodoPago(Integer cdigoMetodoPago) {
        this.codigoMetodoPago = cdigoMetodoPago;
    }
}
