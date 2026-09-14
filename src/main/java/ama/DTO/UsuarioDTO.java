package ama.DTO;

import java.io.Serializable;
import lombok.Data;

@Data
public class UsuarioDTO  implements Serializable  {

    private static final long serialVersionUID = 1L;

    private Integer codigoUsuario;
    private String numeroDocumento;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String celular;
    private String correo;
    private String barrio;
    private String direccion;
    private String nombreSucursal;


    public UsuarioDTO() {
    }

    

}
