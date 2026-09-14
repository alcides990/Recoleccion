package ama.dominio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_usuario")
    private Integer codigoUsuario;
    @JoinColumn(name = "codigo_tipo_documento", referencedColumnName = "codigo_tipo_documento")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonIgnore
    private TipoDocumento tipoDocumento;
    @Column(name = "numero_documento")
    private String numeroDocumento;
    private String nombre;
    private String apellido;
    private String celular;
    @Email(message = "Ingrese un correo electrónico válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    @Column(name = "correo", length = 150)
    private String correo;
    private String barrio;
    private String direccion;
    private String observacion;
    
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonIgnore
    private Estado estado;
    
    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "usuario")
    private  List<Servicio> servicio;
    
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JsonIgnore
    private Sucursal sucursal;
     
    public Usuario() {
    }

    public Usuario(Integer codigoUsuario) {
        this.codigoUsuario = codigoUsuario;
    }

    @Override
    public String toString() {
        return "Usuario{" + "codigoUsuario=" + codigoUsuario + ", numeroDocumento=" + numeroDocumento + ", nombre=" + nombre + ", apellido=" + apellido  + ", celular=" + celular + ", correo=" + correo + ", barrio=" + barrio + ", direccion=" + direccion + ", observacion=" + observacion +  '}';
    }

    

}
