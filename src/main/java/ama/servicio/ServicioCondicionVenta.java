 
package ama.servicio;
 
import ama.dominio.Condicionventa;
import java.util.List;

public interface ServicioCondicionVenta {
    
    public List<Condicionventa> listar();
    
    public void guardar(Condicionventa condicionVenta);
    
    public void eliminar(Condicionventa condicionVenta);
    
    public void encontrar(Condicionventa condicionVenta);
}
