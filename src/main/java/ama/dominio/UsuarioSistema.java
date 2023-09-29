 
package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "usuariossistema")
public class UsuarioSistema implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "codigo_usuario_sistema")
    private Integer codigoUsuarioSistema;
    private String usuario;
    private String calve;
    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado")
    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Estado estado;
    @JoinColumn(name = "codigo_nivel_usuario", referencedColumnName = "codigo_nivel_usuario")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Nivelusuario nivelUsuario;
    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Sucursal sucursal;

    public UsuarioSistema() {
    }

    public UsuarioSistema(Integer codigoUsuarioSistema) {
        this.codigoUsuarioSistema = codigoUsuarioSistema;
    }
    
}
