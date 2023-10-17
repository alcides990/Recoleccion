 
package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
@Data
@Entity
@Table(name = "timbrados")
public class Timbrado implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_timbrado")
    private Integer codigoTimbrado;
    @Column(name = "numero_timbrado")
    private Integer numeroTimbrado;
    @Column(name = "fecha_inicio")
    @Temporal(TemporalType.DATE)
    private Date fechaInicio;
    @Column(name = "fecha_fin")
    @Temporal(TemporalType.DATE)
    private Date fechaFin;
    @Column(name = "numero_inicio")
    private Integer numeroInicio;
    @Column(name = "NumeroFin")
    private Integer numeroFin;
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado")
    @ManyToOne(optional = false)
    private Estado estado;
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal")
    @ManyToOne(optional = false)
    private Sucursal sucursal;
    @JoinColumn(name = "codigo_tipo_factura", referencedColumnName = "codigo_tipo_factura")
    @ManyToOne(optional = false)
    private TipoFactura tipoFactura;

    public Timbrado() {
    }

    public Timbrado(Integer codigoTimbrado) {
        this.codigoTimbrado = codigoTimbrado;
    }
    

}
