 
package ama.servicio;
 
import ama.dominio.Serie;
import java.util.List;

public interface ServicioSerie {
    
    public List<Serie> listar();
    
    public Serie guardar(Serie serie);
    
    public void eliminar(Serie serie);
    
    public Serie encontrar(Serie serie);
    
    public Integer getCodigoSerie();
}
