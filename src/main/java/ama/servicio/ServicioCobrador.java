 
package ama.servicio;
 
import ama.dominio.Cobrador;
import java.util.List;

public interface ServicioCobrador {
    
    public List<Cobrador> listar();
    
    public void guardar(Cobrador cobrador);
    
    public void eliminar(Cobrador cobrador);
    
    public Cobrador encontrar(Cobrador cobrador);
    
    public Integer getCodigoCobrador();
}
