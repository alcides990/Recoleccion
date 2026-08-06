 
package ama.api.controladores;


import com.roshka.sifen.Sifen;
import com.roshka.sifen.core.SifenConfig;
import com.roshka.sifen.core.beans.DocumentoElectronico;
import org.springframework.stereotype.Controller;

 @Controller
public class ControladorSifen {
    
    private final  SifenConfig config;
//    private Factura

    public ControladorSifen() {
        this.config = new SifenConfig();
    }
    Sifen sifen = new Sifen();
    DocumentoElectronico DE=new DocumentoElectronico();
    
//     RespuestaRecepcionDE response = Sifen.recepcionDE(documentoElectronico);
}
