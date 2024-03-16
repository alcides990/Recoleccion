package ama.DTO;

import ama.dominio.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class CategoriaDTO implements Serializable {
    private static final long serialVersionUID = 1L;
   
    private Integer codigoCategoria;
  
    private String nombreCategoria;

    private Double tarifa;
    
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    private Sucursal sucursal;
   

    @Override
    public String toString() {
        return "Categoria{" + "codigoCategoria=" + codigoCategoria + ", nombreCategoria=" + nombreCategoria + ", tarifa=" + tarifa +   '}';
    }
    
    

}
