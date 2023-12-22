 
package ama.servicio;
 
import ama.dominio.Rol;
import java.util.List;
public interface RolService {
    
    public List<Rol> listar();
    
    public void guardar(Rol rol);
    
    public void eliminar(Rol rol);
    
    public Rol encontrar(Rol rol);
    
}
