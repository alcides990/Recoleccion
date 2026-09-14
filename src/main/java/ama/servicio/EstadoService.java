 
package ama.servicio;
 
import ama.dominio.Estado;
import java.util.List;
public interface EstadoService {
    
    public List<Estado> listar();
    
    public List<Estado> findByEstadoIn(List<String> estados );
    
    public void guardar(Estado estado);
    
    public void eliminar(Estado estado);
    
    public Estado encontrar(Estado estado);
    
}
