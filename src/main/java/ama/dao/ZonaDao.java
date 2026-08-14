package ama.dao;

import ama.dominio.Zona;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
           SELECT z FROM Zona z
           JOIN FETCH z.sucursal suc
           JOIN FETCH suc.ciudad ciud
           JOIN FETCH z.cobrador cob
           WHERE suc.codigoSucursal = :codigoSucursal
           """, countQuery = """
           SELECT COUNT(z) FROM Zona z
           WHERE z.sucursal.codigoSucursal = :codigoSucursal
           """)
    Page<Zona> listarPorSucursal(Pageable pageable,
            @Param("codigoSucursal") Integer codigoSucursal);

    @Query(value = """
           SELECT z FROM Zona z
           JOIN FETCH z.sucursal suc
           JOIN FETCH suc.ciudad ciud
           JOIN FETCH z.cobrador cob
           WHERE suc.codigoSucursal = :codigoSucursal
             AND (LOWER(z.nombreZona) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(suc.nombreSucursal) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(ciud.nombreCiudad) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(CONCAT(cob.nombre, ' ', COALESCE(cob.apellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')))
           """, countQuery = """
           SELECT COUNT(z) FROM Zona z
           JOIN z.sucursal suc
           JOIN suc.ciudad ciud
           JOIN z.cobrador cob
           WHERE suc.codigoSucursal = :codigoSucursal
             AND (LOWER(z.nombreZona) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(suc.nombreSucursal) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(ciud.nombreCiudad) LIKE LOWER(CONCAT('%', :filtro, '%'))
               OR LOWER(CONCAT(cob.nombre, ' ', COALESCE(cob.apellido, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')))
           """)
    Page<Zona> buscarPorSucursal(Pageable pageable,
            @Param("codigoSucursal") Integer codigoSucursal,
            @Param("filtro") String filtro);

    @Query("SELECT COUNT(z) FROM Zona z WHERE z.sucursal.codigoSucursal = :codigoSucursal")
    long contarPorSucursal(@Param("codigoSucursal") Integer codigoSucursal);

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
