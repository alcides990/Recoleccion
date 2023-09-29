package ama.dao;

import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface SucursalDao extends CrudRepository<Sucursal, Integer> {

    @Query("SELECT MAX(s.codigoSucursal) as codigoSucursal FROM Sucursal s ")
    Integer getCodigoSucursal();

    //    listar Sucursal
    @Query(value = """
           SELECT  s FROM Sucursal AS s
           JOIN FETCH s.ciudad AS ciud
           """)
    List<Sucursal> listar();

    //    Encontrar Sucursal
    @Query(value = """
           SELECT  s FROM Sucursal AS s
           JOIN FETCH s.ciudad AS ciud
           WHERE s= ?1
           """)
    Sucursal encontrar (Sucursal sucursal);
}
