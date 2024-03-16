package ama.dao;

import ama.dominio.Categoria;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CategoriaDao extends CrudRepository<Categoria, Integer> {

//    querry para generar codigo 
    @Query("SELECT MAX(c.codigoCategoria) as codigoCategoria FROM Categoria c ")
    Integer getCodigoCategoria();

    @Query(value = """
           SELECT  c FROM Categoria AS c
           JOIN FETCH c.sucursal AS s
           JOIN FETCH s.ciudad AS ciud
           WHERE  s= ?1    
           """)
    List<Categoria> listar(Sucursal sucursal);
    
    @Query(value = """
           SELECT  c FROM Categoria AS c
           JOIN FETCH c.sucursal AS s
           JOIN FETCH s.ciudad AS ciud
           """)
    List<Categoria> listar();

//     Encontrar categoria
    @Query(value = """
           SELECT  c FROM Categoria AS c
           JOIN FETCH c.sucursal AS suc
           JOIN FETCH suc.ciudad AS ciud
           WHERE c=?1
           """)
    Categoria encontrar(Categoria categoria);
}
