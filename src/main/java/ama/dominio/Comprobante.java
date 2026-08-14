package ama.dominio;

import ama.dominio.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comprobantes")

public class Comprobante implements Serializable {

    private static final long serialVersionUID = 1L;
    @Valid
    @EmbeddedId
    protected ComprobantePK comprobantePK;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "cantidad_deuda")
    private Integer cantidadDeuda;

    @DecimalMin(value = "1", message = "Tarifa debe ser mayor que 0")
    private double tarifa;
    @Column(name = "cantidad_pago")
    @Min(value = 0, message = "Cantidad pago no puede ser numero negativo")
    @Digits(integer = 10, fraction = 0, message = "Cantidad pago debe ser un numero entero")
     @NotNull(message = "Cantidad pago no puede estar vacia")
    private Integer cantidadPago;

    @DecimalMin(value = "0", message = "Recargo no puede ser numero negativo")
    private double recargo;

    @DecimalMin(value = "0", message = "Saldo no puede ser numero negativo")
    private double saldo;

    @DecimalMin(value = "0", message = "Tarifa no puede negativo")
    @Column(name = "total_importe")
    private double totalImporte;

    @Column(name = "periodo_pago")
    private String periodoPago;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "pago_hasta")
    private LocalDate pagoHasta;

    private String obs;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @JoinColumn(name = "codigo_cobrador", referencedColumnName = "codigo_cobrador", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Cobrador cobrador;

    @JsonIgnore
    @JoinColumn(name = "codigo_comision", referencedColumnName = "codigo_comision")
    @ManyToOne(fetch = FetchType.LAZY)
    private Comision comision;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @JoinColumn(name = "codigo_condicion_venta", referencedColumnName = "codigo_condicion_venta", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private CondicionVenta condicionVenta;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Estado estado;

    @JsonIgnore
    @JoinColumns({
        @JoinColumn(name = "codigo_punto_expedicion", referencedColumnName = "codigo_punto_expedicion", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = false, updatable = false)
    })
    @ManyToOne(fetch = FetchType.LAZY)
    private PuntoExpedicion puntoExpedicion;

    @JsonIgnore
    @JoinColumn(name = "cuenta_corriente", referencedColumnName = "cuenta_corriente", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Servicio servicio;

    // @JsonIgnore
    @JoinColumn(name = "codigo_tipo_comprobante", referencedColumnName = "codigo_tipo_comprobante", nullable = false, insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private TipoComprobante tipoComprobante;

    @JsonIgnore
    @JoinColumn(name = "codigo_timbrado", referencedColumnName = "codigo_timbrado", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Timbrado timbrado;

    @JsonIgnore
    @JoinColumn(name = "codigo_usuario", referencedColumnName = "codigo_usuario", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario usuario;

    @JsonIgnore
    @JoinColumn(name = "codigo_usuario_sistema", referencedColumnName = "codigo_usuario_sistema", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private UsuarioSistema usuarioSistema;
    
    @JsonIgnore
    @JoinColumn(name = "codigo_categoria", referencedColumnName = "codigo_categoria", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Categoria categoria;

    @JsonIgnore
    @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", nullable = false, insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Serie serie;

    @JoinColumns({
        @JoinColumn(name = "numero_comprobante", referencedColumnName = "numero_comprobante", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_punto_expedicion", referencedColumnName = "codigo_punto_expedicion", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_tipo_comprobante", referencedColumnName = "codigo_tipo_comprobante", insertable = false, updatable = false),
        @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", insertable = false, updatable = false),})
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<DetallePago> detallePago;

    public Comprobante(ComprobantePK comprobantePK) {
        this.comprobantePK = comprobantePK;
    }

    @PrePersist
    public void getEmision() {
        this.fechaEmision = LocalDateTime.now();
        this.fechaPago = LocalDate.now();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.comprobantePK);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Comprobante other = (Comprobante) obj;
        return Objects.equals(this.comprobantePK, other.comprobantePK);
    }

}
