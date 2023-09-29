package ama.DTO;

import java.io.Serializable;
import lombok.Data;

@Data
public class CuentaDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String cuentaCorriente;

    public CuentaDTO() {
    }

    public CuentaDTO(String cuentaCorriente) {
        this.cuentaCorriente = cuentaCorriente;
    }

    public String getCuentaCorriente() {
        return cuentaCorriente;
    }

    public void setCuentaCorriente(String cuentaCorriente) {
        this.cuentaCorriente = cuentaCorriente;
    }
    
    

}
