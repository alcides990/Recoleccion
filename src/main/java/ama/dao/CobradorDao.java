package ama.dao;

import ama.dominio.Cobrador;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CobradorDao extends CrudRepository<Cobrador, Integer> {

    @Query("SELECT MAX(c.codigoCobrador) as codigoCobrador FROM Cobrador c ")
    Integer getCodigoCobrador();
//Listar todos los cobradores 
    @Query("""
           SELECT c FROM Cobrador AS c
           JOIN FETCH c.estado
            """)
    List<Cobrador> listar();
    
    @Query("""
           SELECT c FROM Cobrador AS c
           JOIN FETCH c.sucursal AS suc
           JOIN FETCH suc.ciudad AS ciud
           JOIN FETCH c.estado AS est
           WHERE c = ?1
            """)
    Cobrador encontrar(Cobrador cobrador);
}
