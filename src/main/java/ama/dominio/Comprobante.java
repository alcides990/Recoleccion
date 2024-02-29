package ama.dominio;

import ama.dominio.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEmision;

    @Column(name = "fecha_pago")
     @Temporal(TemporalType.TIMESTAMP)
    private Date fechaPago;

    @Column(name = "cantidad_pago")
    @Min(value = 1, message = "Cantidad pago no puede ser menor que 1")
    @Digits(integer = 10, fraction = 0, message = "Cantidad pago debe ser un numero entero")
    private Integer cantidadPago;

    @DecimalMin(value = "0", message = "Tarifa no puede negativo")
    @Column(name = "total_importe")
    private double totalImporte;

    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @OneToOne(mappedBy = "comprobante", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private DetalleComprobante detalleComprobante;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @JoinColumn(name = "codigo_cobrador", referencedColumnName = "codigo_cobrador", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Cobrador cobrador;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @JoinColumn(name = "codigo_condicion_venta", referencedColumnName = "codigo_condicion_venta", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private CondicionVenta condicionVenta;

    @JsonIgnore
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Estado estado;

    @JsonIgnore
    @JoinColumn(name = "codigo_punto_expedicion", referencedColumnName = "codigo_punto_expedicion", nullable = false, insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private PuntoExpedicion puntoExpedicion;

    @JsonIgnore
    @JoinColumn(name = "cuenta_corriente", referencedColumnName = "cuenta_corriente", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Servicio servicio;

    @JsonIgnore
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", nullable = false, insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Sucursal sucursal;

//    @JsonIgnore
    @JoinColumn(name = "codigo_tipo_factura", referencedColumnName = "codigo_tipo_factura", nullable = false, insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private TipoFactura tipoFactura;

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
    @JoinColumn(name = "codigo_serie", referencedColumnName = "codigo_serie", nullable = false, insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Serie serie;

    public Comprobante(ComprobantePK comprobantePK) {
        this.comprobantePK = comprobantePK;
    }

    @PrePersist
    public void getEmision() {
        Date now = new Date();
        this.fechaEmision = now;
        this.fechaPago = now;
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
