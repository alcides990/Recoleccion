package ama.dao;

import ama.dominio.PuntoExpedicion;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Sucursal;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PuntoExpedicionDao extends CrudRepository<PuntoExpedicion, PuntoExpedicionPK> {

       @Query("""
                     SELECT p FROM PuntoExpedicion p
                     INNER JOIN FETCH p.sucursal s
                     INNER JOIN FETCH s.ciudad
                     INNER JOIN FETCH p.estado
                     ORDER BY  p.puntoExpedicionPK.codigoSucursal
                     """)
       List<PuntoExpedicion> getAll();

       @Query("""
                     SELECT p FROM PuntoExpedicion p
                     INNER JOIN FETCH p.sucursal s
                     INNER JOIN FETCH s.ciudad
                     INNER JOIN FETCH p.estado
                     WHERE s= ?1  ORDER BY p.puntoExpedicionPK.codigoPuntoExpedicion
                     """)
       List<PuntoExpedicion> getAllFiandSucursal(Sucursal sucursal);

       @Query("""
                     SELECT p FROM PuntoExpedicion p
                     INNER JOIN FETCH p.sucursal s
                     INNER JOIN FETCH s.ciudad
                     INNER JOIN FETCH p.estado
                     WHERE s= ?1 AND p.puntoExpedicionPK.codigoPuntoExpedicion > 0 ORDER BY p.puntoExpedicionPK.codigoPuntoExpedicion
                     """)
       List<PuntoExpedicion> fiandBySucursalAndCodigoPunteExpedicionMayorCero(Sucursal sucursal);

       @Query("""
                     SELECT p FROM PuntoExpedicion p
                     INNER JOIN FETCH p.puntoExpedicionPK pPK
                     INNER JOIN FETCH p.sucursal s
                     INNER JOIN FETCH s.ciudad
                     INNER JOIN FETCH p.estado
                     WHERE pPK = ?1
                     """)
       PuntoExpedicion getPuntoExpedicion(PuntoExpedicionPK puntoExpedicionPK);

       @Query("""
                     DELETE FROM PuntoExpedicion p
                      WHERE p.puntoExpedicionPK= ?1
                     """)
       @Modifying
       @Transactional
       void eliminar(PuntoExpedicionPK puntoExpedicionPK);

       @Query("""
                      SELECT MAX(p.puntoExpedicionPK.codigoPuntoExpedicion) as codigoPuntoExpedicon FROM PuntoExpedicion p
                     INNER JOIN  p.sucursal s
                     WHERE s= ?1
                     """)
       Integer getCodigoPuntoExpedicion(Sucursal sucursal);
}
