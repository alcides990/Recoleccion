package ama.dao;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.PuntoExpedicion;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Servicio;
import ama.dominio.Sucursal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ComprobanteDao extends CrudRepository<Comprobante, ComprobantePK> {

     @Query("SELECT COUNT(*) FROM Comprobante c  WHERE c.servicio.cuentaCorriente= ?1")
     int getCantidadComprobante(String cuentaCorriente);

     // recuperar comprobantes paginados
     @Query(value = """
               SELECT c FROM Comprobante AS c 
                   JOIN FETCH c.puntoExpedicion AS pe
                   JOIN FETCH pe.sucursal AS suc
                   JOIN FETCH pe.empresa AS emp
                   JOIN FETCH c.tipoComprobante AS tc
                   JOIN FETCH c.usuario AS usu
                   JOIN FETCH c.servicio AS servi
                   JOIN FETCH servi.categoria AS cat
                   JOIN FETCH c.estado AS e
                   JOIN FETCH c.serie s
                      ORDER BY c.fechaEmision DESC
                """, countQuery = "SELECT COUNT(c) FROM Comprobante c")
     Page<Comprobante> getAllComprobantes(Pageable pageable);

     // filtro de comprobantes
     @Query(value = """
               SELECT c FROM Comprobante AS c
               JOIN FETCH c.serie s 
               JOIN FETCH c.puntoExpedicion AS pe
               JOIN FETCH pe.sucursal AS suc
               JOIN FETCH suc.ciudad
               JOIN FETCH c.tipoComprobante AS tc
               JOIN FETCH c.usuario AS usu
               JOIN FETCH c.servicio AS servi
               JOIN FETCH c.cobrador AS cob
               JOIN FETCH c.estado AS e
               JOIN FETCH c.usuarioSistema AS uSist
                      WHERE  suc=?1 AND CONCAT(c.comprobantePK.numeroComprobante) LIKE  ?2%  ORDER BY c.fechaEmision DESC
                 """, countQuery = """
               SELECT COUNT(c) FROM Comprobante c
                INNER JOIN c.puntoExpedicion.sucursal AS suc
                JOIN c.serie s
               WHERE  suc=?1 AND CONCAT(c.comprobantePK.numeroComprobante) LIKE  ?2%
               """)
     Page<Comprobante> filterBySucursalAndNumeroComprobante(Pageable pageable, Sucursal sucursal, Integer numeroComprobante);

     @Query(value = """
               SELECT c FROM Comprobante AS c
               JOIN FETCH c.serie s 
               JOIN FETCH c.puntoExpedicion AS pe
               JOIN FETCH pe.sucursal AS suc
               JOIN FETCH suc.ciudad
               JOIN FETCH c.tipoComprobante AS tc
               JOIN FETCH c.usuario AS usu
               JOIN FETCH c.cobrador AS cob
               JOIN FETCH c.estado AS e
               JOIN FETCH c.usuarioSistema AS uSist
               JOIN FETCH c.servicio AS servi
                      WHERE  pe=?1  AND CONCAT(c.comprobantePK.numeroComprobante) LIKE  ?2% 
                    ORDER BY c.fechaEmision DESC
                 """, countQuery = """
               SELECT COUNT(c) FROM Comprobante c
               WHERE c.puntoExpedicion=?1 AND CONCAT(c.comprobantePK.numeroComprobante) LIKE  ?2%
               """)
     Page<Comprobante> filterByPuntoExpedicionAndNumeroComprobante(Pageable pageable,
               PuntoExpedicion puntoExpedicion,  Integer numeroComprobante);
     
        @Query(value = """
               SELECT c FROM Comprobante AS c
               JOIN FETCH c.serie s 
               JOIN FETCH c.puntoExpedicion AS pe
               JOIN FETCH pe.sucursal AS suc
               JOIN FETCH suc.ciudad
               JOIN FETCH c.tipoComprobante AS tc
               JOIN FETCH c.usuario AS usu
               JOIN FETCH c.cobrador AS cob
               JOIN FETCH c.estado AS e
               JOIN FETCH c.usuarioSistema AS uSist
               JOIN FETCH c.servicio AS servi
                      WHERE  pe=?1  AND  s.codigoSerie=?2  AND CONCAT(c.comprobantePK.numeroComprobante) LIKE  ?3% 
                    ORDER BY c.fechaEmision DESC
                 """, countQuery = """
               SELECT COUNT(c) FROM Comprobante c
               WHERE c.puntoExpedicion=?1 AND  c.serie.codigoSerie=?2 AND CONCAT(c.comprobantePK.numeroComprobante) LIKE  ?3%
               """)
     Page<Comprobante> filterByPuntoExpedicionAndSerieAndNumeroComprobante(Pageable pageable,
               PuntoExpedicion puntoExpedicion, Integer codigoSerie, Integer numeroComprobante);

     @Query(value = """
               SELECT c FROM Comprobante AS c
               JOIN FETCH c.serie s
               JOIN FETCH c.puntoExpedicion AS pe
               JOIN FETCH pe.sucursal AS suc
               JOIN FETCH suc.ciudad
               JOIN FETCH c.tipoComprobante AS tf
               JOIN FETCH c.usuario AS usu
               JOIN FETCH c.servicio AS servi
               JOIN FETCH c.cobrador AS cob
               JOIN FETCH c.estado AS e
               JOIN FETCH c.usuarioSistema AS uSist
                      WHERE  pe=?1  ORDER BY c.fechaEmision DESC
                 """, countQuery = """
               SELECT COUNT(c) FROM Comprobante c
               WHERE c.puntoExpedicion=?1    
               """)
     Page<Comprobante> findByPuntoExpedicion(Pageable pageable, PuntoExpedicion puntoExpedicion);

     @Query(value = """
               SELECT c FROM Comprobante AS c
               JOIN FETCH c.serie s
               JOIN FETCH c.puntoExpedicion AS pe
               JOIN FETCH pe.sucursal AS suc
               JOIN FETCH suc.ciudad
               JOIN FETCH c.tipoComprobante AS tc
               JOIN FETCH c.usuario AS usu
               JOIN FETCH c.servicio AS servi
               JOIN FETCH c.cobrador AS cob
               JOIN FETCH c.estado AS e
               JOIN FETCH c.usuarioSistema AS uSist
                      WHERE  suc=?1
                 """, countQuery = """
               SELECT COUNT(c) FROM Comprobante c
                INNER JOIN c.puntoExpedicion.sucursal AS suc
                JOIN  c.serie s
                WHERE  suc=?1
                ORDER BY  c.fechaEmision DESC
               """)
     Page<Comprobante> findBySucursal(Pageable pageable, Sucursal sucursal);

     @Query(value = """
                  SELECT c.comprobantePK FROM Comprobante AS c
                 WHERE c.servicio = :servicio
               """, countQuery = "SELECT COUNT(c) FROM Comprobante c WHERE c.servicio =:servicio")
     Page<ComprobantePK> getComprobantePKs(Pageable pageable, @Param("servicio") Servicio servicio);

     @Query(value = """
               SELECT c FROM Comprobante AS c
                JOIN FETCH c.puntoExpedicion AS pe
                JOIN FETCH pe.sucursal AS suc
                JOIN FETCH c.tipoComprobante AS tc
                JOIN FETCH c.usuario AS usu
                JOIN FETCH c.servicio AS servi
                JOIN FETCH c.estado AS e
               WHERE servi.cuentaCorriente=?1
               ORDER BY  c.fechaEmision DESC
                      """, countQuery = """
                                        SELECT COUNT(c) FROM Comprobante c 
                                        JOIN c.servicio AS servi 
                                        WHERE servi.cuentaCorriente=?1
                                        """)
     Page<Comprobante> getComprobantesCuenta(Pageable paget, String cuentaCorriente);

     // Encontrar comprobante por ComprobantePK
     @Query("""
                SELECT c FROM Comprobante AS c
                            LEFT JOIN FETCH c.detallePago AS dtp
                            LEFT JOIN FETCH dtp.metodoPago AS mtp
                            JOIN FETCH c.puntoExpedicion AS pe
                            JOIN FETCH pe.sucursal AS s
                            JOIN FETCH s.ciudad ciud
                            JOIN FETCH c.tipoComprobante AS tc
                            JOIN FETCH c.serie AS serie
                            JOIN FETCH c.condicionVenta AS cv
                            JOIN FETCH c.usuario AS usu
                            JOIN FETCH c.cobrador AS cob
                            JOIN FETCH c.servicio AS servi
                            JOIN FETCH servi.categoria AS cat
                            JOIN FETCH c.estado AS e
               WHERE c.comprobantePK=?1
               """)
     Comprobante getComprobante(ComprobantePK comprobantePK);

     // Generar numero de comprobantes
     @Query("""
               SELECT MAX(cPK.numeroComprobante) AS numeroComprobante FROM Comprobante c
               JOIN  c.comprobantePK AS cPK
               WHERE cPK.puntoExpedicionPK= ?1
               AND cPK.codigoTipoComprobante= ?2
               AND cPK.codigoSerie= ?3 """)
     Integer getNumeroComprobante(PuntoExpedicionPK puntoExpedicionPK, Integer codigoTipoFactura,
               Integer codigoSerie);

     @Query("SELECT SUM(c.cantidadPago) FROM Comprobante c  "
               + "WHERE c.servicio.cuentaCorriente= ?1 ")
     Integer getCantidadPago(String cuentaCorriente);

     @Query("""
                 SELECT c  FROM Comprobante c
                    WHERE c.fechaEmision = (SELECT MAX(c2.fechaEmision) FROM Comprobante c2 WHERE c2.servicio.cuentaCorriente = ?1 AND c2.estado.codigoEstado ='1')
                    AND c.servicio.cuentaCorriente = ?1
               """)
     Optional<Comprobante> getUltimoComprobanteCuentaActivo(String cuentaCorriente);
     
     @Query(value = "SELECT fn_pago_hasta(?1)", 
             nativeQuery = true)
     Optional<String> getPagoHasta(String cuentaCorriente);

     @Query("""
                 SELECT fn_pago_hasta(?1), c.saldo  FROM Comprobante c
                    WHERE c.fechaEmision = (SELECT MAX(c2.fechaEmision) FROM Comprobante c2 WHERE c2.servicio.cuentaCorriente = ?1 AND c2.estado.codigoEstado ='1')
                    AND c.servicio.cuentaCorriente = ?1

               """)
     List<Object[]> getPagoHastaAndSaldo(String cuentaCorriente);

}
