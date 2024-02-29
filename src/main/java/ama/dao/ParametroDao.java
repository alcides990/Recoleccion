package ama.dao;

import ama.dominio.Parametro;
import ama.dominio.Sucursal;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ParametroDao extends CrudRepository<Parametro, Integer> {

    @Query("SELECT MAX(p.codigoParametro) as codigoParametro FROM Parametro p ")
    Integer generarCodigo();
   
    @Query("""
           SELECT p FROM Parametro p  
           JOIN FETCH p.comision
           WHERE p.sucursal= ?1 
           """)
    Optional<Parametro> getParametro(Sucursal sucursal);
}
