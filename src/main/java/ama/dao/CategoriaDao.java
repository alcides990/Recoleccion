package ama.dao;

import ama.dominio.Categoria;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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
           WHERE  s= ?1    
           """,
            countQuery = """
           SELECT COUNT(c) FROM Categoria c
           WHERE c.sucursal = ?1
           """
    )
    Page<Categoria> filtrar(Pageable pageable, Sucursal sucursal);

    @Query(value = """
           SELECT c FROM Categoria c
           JOIN FETCH c.sucursal s
           JOIN FETCH s.ciudad ciud
           WHERE s.codigoSucursal = :codigoSucursal
             AND (LOWER(c.nombreCategoria) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(s.nombreSucursal) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(ciud.nombreCiudad) LIKE LOWER(CONCAT('%', :filtro, '%')))
           """, countQuery = """
           SELECT COUNT(c) FROM Categoria c
           JOIN c.sucursal s
           JOIN s.ciudad ciud
           WHERE s.codigoSucursal = :codigoSucursal
             AND (LOWER(c.nombreCategoria) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(s.nombreSucursal) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(ciud.nombreCiudad) LIKE LOWER(CONCAT('%', :filtro, '%')))
           """)
    Page<Categoria> buscarPorSucursal(Pageable pageable,
            @Param("codigoSucursal") Integer codigoSucursal,
            @Param("filtro") String filtro);

    @Query("SELECT COUNT(c) FROM Categoria c WHERE c.sucursal.codigoSucursal = :codigoSucursal")
    long contarPorSucursal(@Param("codigoSucursal") Integer codigoSucursal);

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
