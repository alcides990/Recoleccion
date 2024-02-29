package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Embeddable
public class ManzanaPK implements Serializable {

    @Basic(optional = false)
    @Min(value = 1, message = "Numero de manzana debe ser mayor a 0")
    @Column(name = "codigo_manzana")
    private int numeroManzana;

    @Basic(optional = false)
    @Column(name = "codigo_sucursal")
    private int codigoSucursal;

    public ManzanaPK(int numeroManzana, int codigoSucursal) {
        this.numeroManzana = numeroManzana;
        this.codigoSucursal = codigoSucursal;
    }

    public ManzanaPK() {
    }

}
