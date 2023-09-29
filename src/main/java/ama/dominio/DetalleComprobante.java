package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "detallecomprobantes")
public class DetalleComprobante implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    @Valid
    protected DetalleComprobantePK detalleComprobantePK;

    @Column(name = "cantidad_pago")
    @NotNull(message = "Cantidad pago no puede ser nulo !!")
    @Min(value = 1, message = "La cantidad de pago debe ser mayor a 0")
    private Integer cantidadPago;
    
    @DecimalMin(value = "1", message = "Importe debe ser mayor a 0")
    private Double importe;
    
   @DecimalMin(value = "0", message = "Recargo no puede ser menor a 0")
    private Double recargo;

    private Double saldo;

    @Column(name = "periodo_pago")
    private String periodoPago;

    private String obs;

    @JoinColumn(name = "codigo_comision", referencedColumnName = "codigo_comision")
    @ManyToOne(fetch = FetchType.LAZY)
    private Comision comision;

    @JoinColumns({
        @JoinColumn(name = "numero_comprobante", referencedColumnName = "numero_comprobante", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_punto_expedicion", referencedColumnName = "codigo_punto_expedicion", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_tipo_factura", referencedColumnName = "codigo_tipo_factura", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", insertable = false, updatable = false)
    })
    @ManyToOne(fetch = FetchType.LAZY)
    private Comprobante comprobante;

   @JoinColumns({
    @JoinColumn(name = "codigo_metodo_pago", referencedColumnName = "codigo_metodo_pago", insertable = false, updatable = false),
    @JoinColumn(name = "codigo_punto_expedicion", referencedColumnName = "codigo_punto_expedicion", insertable = false, updatable = false),
    @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", insertable = false, updatable = false),
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = false, updatable = false),
    @JoinColumn(name = "codigo_tipo_factura", referencedColumnName = "codigo_tipo_factura", insertable = false, updatable = false),
    @JoinColumn(name = "numero_comprobante", referencedColumnName = "numero_comprobante", insertable = false, updatable = false)
})
    @OneToMany(fetch = FetchType.LAZY)
    private List<MetodoPago> metodopago;

    @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Serie serie;

    @Override
    public String toString() {
        return "DetalleComprobante{" + "detalleComprobantePK=" + detalleComprobantePK + ", cantidadPago=" + cantidadPago + ", importe=" + importe + ", recargo=" + recargo + ", saldo=" + saldo + ", periodoPago=" + periodoPago + ", obs=" + obs + '}';
    }

   

    
}
