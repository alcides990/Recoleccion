package ama.dao;

import ama.dominio.Timbrado;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface TimbradoDao extends CrudRepository<Timbrado, Integer> {

    @Query("SELECT MAX(t.codigoTimbrado) as codigoTimbrado FROM Timbrado t ")
    Integer getCodigoTimbrado();
    
    @Query("SELECT t FROM Timbrado t WHERE t= ?1")
    Timbrado getTimbrado(Timbrado timbrado);

    @Query("""
           SELECT t FROM Timbrado t
           WHERE t.estado.codigoEstado = 1
           ORDER BY t.numeroTimbrado
           """)
    List<Timbrado> listarActivos();
}
    
