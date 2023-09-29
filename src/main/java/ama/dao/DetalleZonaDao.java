package ama.dao;

import ama.dominio.Cobrador;
import ama.dominio.DetalleZona;
import ama.dominio.DetalleZonaPK;
import ama.dominio.Zona;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DetalleZonaDao extends JpaRepository<DetalleZona, Integer> {

    //Seleccionar un detallezona correspomdiende a una manzana
    @Query("SELECT  dtz  FROM DetalleZona dtz WHERE dtz.detalleZonaPK = ?1")
    DetalleZona getManzana(DetalleZonaPK detalleZonaPK);

    //Seleccionar Lista  detallezona correspomdiende a una zona y cobrador
    @Query(value = """
           SELECT  dtz FROM DetalleZona dtz 
            JOIN FETCH dtz.zona z 
            JOIN FETCH dtz.cobrador c
            JOIN FETCH dtz.sucursal suc
            JOIN FETCH suc.ciudad ciud
            WHERE z=?1""")
    List<DetalleZona> listar(Zona zona);

    //Encontrar  detallezona correspomdiende a una zona y cobrador
    @Query(value = """
           SELECT  dtz FROM DetalleZona dtz 
            JOIN FETCH dtz.zona z 
            JOIN FETCH dtz.cobrador c
            JOIN FETCH dtz.sucursal suc
            JOIN FETCH suc.ciudad ciud
            WHERE z=?1
            AND c= ?2 
            """)
    DetalleZona encontrar(Zona zona, Cobrador cobrador);

    @Query("""
           SELECT  dtz  FROM DetalleZona dtz 
            JOIN dtz.manzana m 
            WHERE dtz.detalleZonaPK = ?1""")
    List<DetalleZona> getDetalleZonaPorZona(DetalleZonaPK detalleZonaPK);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM DetalleZona dtz WHERE dtz.detalleZonaPK = ?1")
    void eliminarManzana(DetalleZona detalleZona);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("""
           UPDATE DetalleZona dtz  SET dtz.cobrador = ?2 
           WHERE dtz.detalleZonaPK = ?1
           """)
    void modificar(DetalleZonaPK detalleZonaPK, Cobrador cobradorNuevo);
}
