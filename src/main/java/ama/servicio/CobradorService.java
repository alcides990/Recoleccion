 
package ama.servicio;
 
import ama.dominio.Cobrador;
import ama.dominio.Sucursal;
import java.util.List;

public interface CobradorService {
    
    public List<Cobrador> listar(Sucursal sucursal);
    
    public List<Cobrador> listarIsEstadoActivo(Sucursal sucursal);
    
    public void guardar(Cobrador cobrador);
    
    public void eliminar(Cobrador cobrador);
    
    public Cobrador encontrar(Cobrador cobrador);
    
    public Integer getCodigoCobrador();
}
