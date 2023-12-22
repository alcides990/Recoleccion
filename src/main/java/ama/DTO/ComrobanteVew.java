
package ama.DTO;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class ComrobanteVew implements Serializable{
   double importe;
   int cantidadPago;
    Date fechaEmision;
   
}