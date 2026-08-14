
package ama.servicio;
 
import ama.dominio.Comision;
import java.util.List;

public interface ComisionService {
    
    public List<Comision> listar();
    
    public void guardar(Comision comision);
    
    public void eliminar(Comision comision);
    
    public Comision encontrar(Comision comision);
}
