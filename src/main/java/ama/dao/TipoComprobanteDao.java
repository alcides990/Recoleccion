 
package ama.dao;
 
import ama.dominio.TipoComprobante;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface TipoComprobanteDao extends CrudRepository<TipoComprobante, Integer>{

    @Query("SELECT MAX(t.codigoTipoComprobante) as codigoZona FROM TipoComprobante t ")
    Integer getCodigoTipoComprobante();
    
}
