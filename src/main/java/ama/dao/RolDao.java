 
package ama.dao;
 
import ama.dominio.Rol;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface RolDao extends CrudRepository<Rol, Integer>{

    @Query("""
           SELECT r FROM Rol r 
           WHERE r.codigoRol > 0
           """)
    @Override
    List<Rol> findAll();
    
}
