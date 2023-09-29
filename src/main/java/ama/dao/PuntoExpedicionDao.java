 
package ama.dao;
 
import ama.dominio.PuntoExpedicion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface PuntoExpedicionDao extends CrudRepository<PuntoExpedicion ,Integer>{
    
    @Query("SELECT MAX(p.codigoPuntoExpedicion) as codigoPuntoExpedicon FROM PuntoExpedicion p ")
    Integer getCodigoPuntoExpedicion();
}
