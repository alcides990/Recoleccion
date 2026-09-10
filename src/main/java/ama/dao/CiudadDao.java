 
package ama.dao;

import ama.dominio.Ciudad;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
 
public interface CiudadDao extends JpaRepository<Ciudad, Integer>{
    @Query(value = " SELECT MAX(c.codigoCiudad) FROM Ciudad c")
    Integer getCodigoCiudad();
}
