 
package ama.servicio;
 
import ama.dominio.Categoria;
import java.util.List;

public interface CategoriaService {
    
    public List<Categoria> listar();
    
    public Categoria guardar(Categoria categoria);
    
    public void eliminar(Categoria categoria);
    
    public Categoria encontrar(Categoria categoria);
    
    public Integer getCodigoCategoria();
}
