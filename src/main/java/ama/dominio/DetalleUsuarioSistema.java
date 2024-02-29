package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "detalle_usuario_sistema")
public class DetalleUsuarioSistema implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected DetalleUsuarioSistemaPK detalleUsuarioSistemaPK;

    @JoinColumn(name = "codigo_usuario_sistema", referencedColumnName = "codigo_usuario_sistema", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private UsuarioSistema usuarioSistema;

    @JoinColumn(name = "codigo_rol", referencedColumnName = "codigo_rol", nullable = false, insertable = false, updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Rol rol;


    public DetalleUsuarioSistema(DetalleUsuarioSistemaPK detalleUsuarioSistemaPK) {
        this.detalleUsuarioSistemaPK = detalleUsuarioSistemaPK;
    }

}
