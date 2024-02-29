package ama.dominio;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Entity
@Table(name = "parametros")
public class Parametro implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_parametro")
    private Integer codigoParametro;

    @Column(name = "cierre_periodo")
    private LocalDate cierrePeriodo;

    @DecimalMax(value = "100", message = "recargoMora no puede ser mayor a 100")
    @DecimalMin(value = "0", message = "recargoMora no puede ser numero negativo")
    @Column(name = "recargo_mora")
    private double recargoMora;
    
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = true)
    @OneToOne(fetch = FetchType.LAZY)
    private Sucursal sucursal;
    
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinColumn(name = "codigo_comision", referencedColumnName = "codigo_comision", insertable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private Comision comision;
    
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinColumn(name = "codigo_empresa", referencedColumnName = "codigo_empresa")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Empresa empresa;

    public Parametro(Integer codigoParametro) {
        this.codigoParametro = codigoParametro;
    }

}
