package ama.dominio;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "usuariossistema")
public class UsuarioSistema implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "codigo_usuario_sistema")
    private Integer codigoUsuarioSistema;

    @Column(name = "usuario")
    private String nombre;

    private String clave;

    @JoinColumn(name = "codigo_estado", referencedColumnName = "codigo_estado")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Estado estado;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "codigo_usuario_sistema", referencedColumnName = "codigo_usuario_sistema", updatable = false, insertable = false)
    private List<DetalleUsuarioSistema> detalleUsuarioSistema;

    @JoinColumn(name = "codigo_sucursal", referencedColumnName = "codigo_sucursal")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Sucursal sucursal;

    public UsuarioSistema() {
    }

    public UsuarioSistema(int codigoUsuario) {
        this.codigoUsuarioSistema = codigoUsuario;
    }

}
