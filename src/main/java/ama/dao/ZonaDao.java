package ama.dao;

import ama.dominio.Zona;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ZonaDao extends CrudRepository<Zona, Integer> {

    @Query("SELECT MAX(z.codigoZona) as codigoZona FROM Zona z ")
    Integer getCodigoZona();
//  listar zonas

    @Query("""
           SELECT z FROM Zona z 
           JOIN FETCH z.sucursal suc
           JOIN FETCH z.cobrador cob
           JOIN FETCH suc.ciudad ciud
           """)
    List<Zona> listar();

//  Enconrar zonas
    @Query("""
           SELECT z FROM Zona z 
           JOIN FETCH z.cobrador cob
           JOIN FETCH z.sucursal suc
           JOIN FETCH suc.ciudad ciud
           WHERE z=?1
           """)
   Zona econtrar(Zona zona);
}
