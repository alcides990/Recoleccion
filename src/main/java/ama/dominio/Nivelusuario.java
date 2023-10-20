
package ama.dominio;

 import java.io.Serializable;
import jakarta.persistence.*;

/**
 *
 * @author Alcides
 */
@Entity
@Table(name = "nivelusuarios")
public class NivelUsuario implements  Serializable{
       private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "codigo_nivel_usuario")
    private Integer codigoNivelUsuario;
    @Column(name = "nivel_usuario")
    private String nivelUsuario;

}
