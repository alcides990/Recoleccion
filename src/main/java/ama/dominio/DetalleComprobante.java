package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "detalle_comprobantes")
public class DetalleComprobante implements Serializable {

    private static final long serialVersionUID = 1L;
    @Valid
    @EmbeddedId
    protected DetalleComprobantePK detalleComprobantePK;

 
    
    private Integer cantidadDeuda;

    @DecimalMin(value = "1", message = "Tarifa debe ser mayor que 0")
    private double tarifa;

    @DecimalMin(value = "0", message = "Recargo no puede ser numero negativo")
    private double recargo;
    
    @DecimalMin(value = "0", message = "Saldo no puede ser numero negativo")
    private double saldo;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "pago_hasta")
    private LocalDate pagoHasta;
    
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
    @OneToOne(fetch = FetchType.LAZY)
    private Comprobante comprobante;

    @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Serie serie;

    @JoinColumn(name = "codigo_categoria", referencedColumnName = "codigo_categoria")
    @ManyToOne(fetch = FetchType.LAZY)
    private Categoria categoria;

    @JoinColumns({
        @JoinColumn(name = "numero_comprobante", referencedColumnName = "numero_comprobante", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_punto_expedicion", referencedColumnName = "codigo_punto_expedicion", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_tipo_factura", referencedColumnName = "codigo_tipo_factura", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", insertable = false, updatable = false),})
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<DetallePago> detallePago;

    @Override
    public String toString() {
        return "DetalleComprobante{" + "detalleComprobantePK=" + detalleComprobantePK + ", recargo=" + recargo + ", saldo=" + saldo + ", periodoPago=" + periodoPago + ", obs=" + obs + '}';
    }

}
