
package ama.servicio;
 
import ama.dominio.Comision;
import java.util.List;

public interface ServicioComision {
    
    public List<Comision> listar();
    
    public void guardar(Comision comision);
    
    public void eliminar(Comision comision);
    
    public void encontrar(Comision comision);
}
