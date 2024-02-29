 
package ama.dao;
 
import ama.dominio.MetodoPago;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface MetodoPagoDao extends CrudRepository<MetodoPago ,Integer>{
    
    @Query("SELECT MAX(mp.codigoMetodoPago) FROM MetodoPago mp ")
    Integer getCodigoMetodoPago();
}
