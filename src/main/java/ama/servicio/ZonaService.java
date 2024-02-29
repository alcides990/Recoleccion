 
package ama.servicio;
 
import ama.dominio.Zona;
import java.util.List;

public interface ZonaService {
    
    public List<Zona> listar();
    
    public void guardar(Zona zona);
    
    public void eliminar(Zona zona);
    
    public Zona encontrar(Zona zona);
    
    public Integer getCodigoZona();
}
