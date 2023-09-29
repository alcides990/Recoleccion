 
package ama.DTO;
 
import java.io.Serializable;
import lombok.Data;

@Data
public class sucursalDTO implements Serializable{
    private Integer codigoSucursal;
    private String sucursal;   
    private String ciudad;   
    private String empresa;   
}
