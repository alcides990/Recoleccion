package ama.dao;

import ama.dominio.Cobrador;
import ama.dominio.DetalleZona;
import ama.dominio.Manzana;
import ama.dominio.ManzanaPK;
import ama.dominio.Zona;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ManzanaDao extends JpaRepository<Manzana, Integer> {

    //Seleccionar una manzana
    @Query("""
           SELECT  m  FROM Manzana m 
           JOIN FETCH m.zona z 
           JOIN FETCH m.sucursal s 
           JOIN FETCH m.cobrador c 
           WHERE m.manzanaPK = ?1 """)
    Manzana encontrar(ManzanaPK manzanaPK);

    //Seleccionar Lista  detallezona correspomdiende a una zona y cobrador
    @Query(value = """
           SELECT  m FROM Manzana m
            JOIN FETCH m.zona z 
            JOIN FETCH m.cobrador c 
            JOIN FETCH m.sucursal suc
            JOIN FETCH suc.ciudad ciud 
            WHERE c=?1 AND z=?2""")
    List<Manzana> listar(Cobrador cobrador, Zona zona);

    //Encontrar  detallezona correspomdiende a una zona y cobrador
    @Query(value = """
           SELECT  dtz FROM DetalleZona dtz 
            JOIN FETCH dtz.zona z 
            JOIN FETCH dtz.cobrador c
            JOIN FETCH dtz.sucursal suc
            JOIN FETCH suc.ciudad ciud
            WHERE z.codigoZona=?1
            AND c.codigoCobrador = ?2 
            GROUP BY z.codigoZona, c.codigoCobrador """)
    DetalleZona encontrar(Zona zona, Cobrador cobrador);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Manzana m WHERE m.manzanaPK = ?1")
    void eliminarManzana(ManzanaPK manzanaPK);


}
