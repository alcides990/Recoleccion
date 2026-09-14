 
package ama.dao;
 
import ama.dominio.Estado;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface EstadoDao extends CrudRepository<Estado, Integer>{
    
   List<Estado> findByEstadoIn(List<String> estados);
    
}
