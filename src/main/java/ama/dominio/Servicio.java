package ama.dominio;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@Entity
@Table(name = "servicios")
public class Servicio implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "cuenta_corriente")
    @NotBlank(message = "Cuentacorriente no puede estar vacio")
    // @Pattern(regexp = "[0-9]{2}-\\d{4}-\\d{2}", message = "Formato de cuenta
    // corriente no es valido")
    private String cuentaCorriente;

    @Column(name = "direccion")
    private String direccion;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "ocupado", nullable = false)
    @Pattern(regexp = "OCUPADO|DESOCUPADO|BALDIO", message = "Ocupación no válida")
    private String ocupado = "OCUPADO";

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_categoria", referencedColumnName = "codigo_categoria")
    private Categoria categoria;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado")
    private Estado estado;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_usuario", referencedColumnName = "codigo_usuario")
    private Usuario usuario;

    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal", insertable = false, updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Sucursal sucursal;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JoinColumns({
            @JoinColumn(name = "codigo_manzana", referencedColumnName = "codigo_manzana"),
            @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal")
    })

    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Manzana manzana;

//    @JsonIgnore
//    @ManyToOne(optional = false, fetch = FetchType.LAZY)
//    @JoinColumn(name = "codigo_zona", referencedColumnName = "codigo_zona")
//    private Zona zona;
    public Servicio(String cuentaCorriente) {
        this.cuentaCorriente = cuentaCorriente;
    }

    private String observacion;

    @Transient
    EstadoCuenta estadoCuenta;

    @PrePersist
    @PreUpdate
    private void completarDatosCuenta() {
        if (ocupado == null) {
            ocupado = "OCUPADO";
        }
    }

}
