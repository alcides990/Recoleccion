 
package ama.dao;

import ama.dominio.Ciudad;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
 
public interface CiudadDao extends CrudRepository<Ciudad, Integer>{
    @Query(value = " SELECT MAX(c.codigoCiudad) FROM Ciudad c")
    Integer getCodigoCiudad();
    
//    @Query(value = " SELECT c FROM Ciudad c ")
//    List<Ciudad> listaCiudad();
}
