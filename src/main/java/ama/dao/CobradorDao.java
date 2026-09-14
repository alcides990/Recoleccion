package ama.dao;

import ama.dominio.Cobrador;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface CobradorDao extends JpaRepository<Cobrador, Integer> {

    @Query("SELECT MAX(c.codigoCobrador) as codigoCobrador FROM Cobrador c ")
    Integer getCodigoCobrador();
 
    @Query("""
           SELECT c FROM Cobrador AS c
           JOIN FETCH c.estado e
           WHERE c.sucursal=?1
            """)
    List<Cobrador> listar(Sucursal sucursal);

    @Query(value = """
           SELECT c FROM Cobrador c
           JOIN FETCH c.estado e
           JOIN FETCH c.sucursal s
           JOIN FETCH s.ciudad ciud
           WHERE s.codigoSucursal = :codigoSucursal
           """, countQuery = """
           SELECT COUNT(c) FROM Cobrador c
           WHERE c.sucursal.codigoSucursal = :codigoSucursal
           """)
    Page<Cobrador> listarPorSucursal(Pageable pageable,
            @Param("codigoSucursal") Integer codigoSucursal);

    @Query(value = """
           SELECT c FROM Cobrador c
           JOIN FETCH c.estado e
           JOIN FETCH c.sucursal s
           JOIN FETCH s.ciudad ciud
           WHERE s.codigoSucursal = :codigoSucursal
             AND (LOWER(CONCAT(c.nombre, ' ', COALESCE(c.apellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(c.celular, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(c.direccion, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(e.estado) LIKE LOWER(CONCAT('%', :filtro, '%')))
           """, countQuery = """
           SELECT COUNT(c) FROM Cobrador c
           JOIN c.estado e
           WHERE c.sucursal.codigoSucursal = :codigoSucursal
             AND (LOWER(CONCAT(c.nombre, ' ', COALESCE(c.apellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(c.celular, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(COALESCE(c.direccion, '')) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(e.estado) LIKE LOWER(CONCAT('%', :filtro, '%')))
           """)
    Page<Cobrador> buscarPorSucursal(Pageable pageable,
            @Param("codigoSucursal") Integer codigoSucursal,
            @Param("filtro") String filtro);

    @Query("SELECT COUNT(c) FROM Cobrador c WHERE c.sucursal.codigoSucursal = :codigoSucursal")
    long contarPorSucursal(@Param("codigoSucursal") Integer codigoSucursal);
    
    @Query("""
           SELECT c FROM Cobrador AS c
           JOIN FETCH c.estado e
           WHERE c.sucursal=?1
           AND e.codigoEstado=1
            """)
    List<Cobrador> listarIsEstdoActivo(Sucursal sucursal);
    
    @Query("""
           SELECT c FROM Cobrador AS c
           JOIN FETCH c.sucursal AS suc
           JOIN FETCH suc.ciudad AS ciud
           JOIN FETCH c.estado AS est
           WHERE c = ?1
            """)
    Cobrador encontrar(Cobrador cobrador);
}
