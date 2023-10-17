 
package ama.servicio;
 
import ama.dominio.CondicionVenta;
import java.util.List;

public interface ServicioCondicionVenta {
    
    public List<CondicionVenta> listar();
    
    public void guardar(CondicionVenta condicionVenta);
    
    public void eliminar(CondicionVenta condicionVenta);
    
    public void encontrar(CondicionVenta condicionVenta);
}
