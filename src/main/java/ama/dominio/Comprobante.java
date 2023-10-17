package ama.dominio;

import ama.dominio.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "comprobantes", catalog = "cliba_sa", schema = "")

public class Comprobante implements Serializable {

    private static final long serialVersionUID = 1L;
    @Valid
    @EmbeddedId
    protected ComprobantePK comprobantePK;

    @Column(name = "fecha_emision")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEmision;

    @Column(name = "hora_emision")
    @Temporal(TemporalType.TIME)
    private Date horaEmision;

    @Column(name = "fecha_pago")
    @Temporal(TemporalType.DATE)
    private Date fechaPago;

    @Column(name = "cantidad_deuda")
    private Integer cantidadDeuda;

    private Double tarifa;
    
    @ToString.Exclude
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "comprobante")
    private List<DetalleComprobante> detalleComprobante;

    @JsonIgnore
    @JoinColumn(name = "codigo_cobrador", referencedColumnName = "codigo_cobrador", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Cobrador cobrador;

    @JsonIgnore
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

    public Comprobante() {
    }

    public Comprobante(ComprobantePK comprobantePK) {
        this.comprobantePK = comprobantePK;
    }

    @PrePersist
    public void getEmision() {
        Date now = new Date();
        this.fechaEmision = now;
        this.horaEmision = now;
        this.fechaPago = now;
    }

//    @Override
//    public String toString() {
//        return "Comprobante{" + "comprobantePK=" + comprobantePK + ", fechaEmision=" + fechaEmision + ", horaEmision=" + horaEmision + ", fechaPago=" + fechaPago + ", cantidadDeuda=" + cantidadDeuda + ", tarifa=" + tarifa + '}';
//    }

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
