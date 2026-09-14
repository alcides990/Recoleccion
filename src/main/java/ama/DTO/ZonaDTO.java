package ama.DTO;

import ama.dominio.Cobrador;
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

public class ZonaDTO implements Serializable {
    private static final long serialVersionUID = 1L;
   
    private Integer codigoZona;
  
    private String nombreZona;
    private Cobrador cobrador;

    

    @Override
    public String toString() {
        return "Zona{" + "codigoZona=" + codigoZona + ", nombreZona=" + nombreZona +  '}';
    }
    
    

}
