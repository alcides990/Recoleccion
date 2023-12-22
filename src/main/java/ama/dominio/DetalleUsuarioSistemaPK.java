 
package ama.dominio;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class DetalleUsuarioSistemaPK implements Serializable {

    @Basic(optional = false)
    @Column(name = "codigo_usuario_sistema")
    private int codigoUsuarioSistema;
    
    @Basic(optional = false)
    @Column(name = "codigo_rol")
    private int codigoRol;
    
 

  
    

    
}
