 
package ama.dao;
 
import ama.dominio.Serie;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface SerieDao extends CrudRepository<Serie, Integer>{
    
     @Query("SELECT MAX(s.codigoSerie) as codigoSerie FROM Serie s ")
    Integer getCodigoSerie();
}
