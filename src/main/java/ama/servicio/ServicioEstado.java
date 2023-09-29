 
package ama.servicio;
 
import ama.dominio.Estado;
import java.util.List;
public interface ServicioEstado {
    
    public List<Estado> listar();
    
    public void guardar(Estado estado);
    
    public void eliminar(Estado estado);
    
    public Estado encontrar(Estado estado);
    
}
