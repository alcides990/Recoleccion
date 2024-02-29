package ama.dao;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.Servicio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ComprobanteDao extends CrudRepository<Comprobante, Integer> {

    @Query("SELECT COUNT(*) FROM Comprobante c  WHERE c.servicio.cuentaCorriente= ?1")
    int getCantidadComprobante(String cuentaCorriente);

    //recuperar comprobantes paginados
    @Query(value = """
            SELECT c FROM Comprobante AS c 
                JOIN FETCH c.detalleComprobante AS dtc
                JOIN FETCH c.sucursal AS suc
                JOIN FETCH c.puntoExpedicion AS pe
                JOIN FETCH pe.empresa AS emp
                JOIN FETCH c.tipoFactura AS tf
                JOIN FETCH c.usuario AS usu
                JOIN FETCH c.servicio AS servi
                JOIN FETCH c.estado AS e 
                   ORDER BY c.fechaPago DESC
             """,
            countQuery = "SELECT COUNT(c) FROM Comprobante c")
    Page<Comprobante> getAllComprobantes(Pageable pageable);

    //filtro de comprobantes
    @Query(value = """
            SELECT c FROM Comprobante AS c 
            JOIN FETCH c.comprobantePK AS cPK
            JOIN FETCH c.detalleComprobante AS dtc
            JOIN FETCH c.sucursal AS suc
            JOIN FETCH suc.ciudad
            JOIN FETCH c.puntoExpedicion AS pe
            JOIN FETCH c.tipoFactura AS tf
            JOIN FETCH c.usuario AS usu
            JOIN FETCH c.servicio AS servi
            JOIN FETCH c.cobrador AS cob
            JOIN FETCH c.estado AS e  
            JOIN FETCH c.usuarioSistema AS uSist
                   WHERE suc.codigoSucursal=?1 AND  pe.codigoPuntoExpedicion=?2 AND CONCAT(cPK.numeroComprobante) LIKE  %?3% 
              """,
            countQuery = """
                         SELECT COUNT(c) FROM Comprobante c 
                         WHERE c.comprobantePK.codigoSucursal=?1 
                         AND  c.comprobantePK.codigoPuntoExpedicion=?2 
                         AND CONCAT(c.comprobantePK.numeroComprobante) LIKE  %?3% 
                         """)
    Page<Comprobante> filtrarComprobantes(Pageable pageable, Integer sucursal, Integer puntoExpedicion, Integer numeroComprobante);

    @Query(value = """
                      SELECT c.comprobantePK FROM Comprobante AS c 
                     WHERE c.servicio = :servicio
                   """,
            countQuery = "SELECT COUNT(c) FROM Comprobante c WHERE c.servicio =:servicio")
    Page<ComprobantePK> getComprobantePKs(Pageable pageable, @Param("servicio") Servicio servicio);

    @Query(value = """
            SELECT c FROM Comprobante AS c 
             JOIN FETCH c.detalleComprobante AS dtc
             JOIN FETCH c.sucursal AS suc
             JOIN FETCH c.puntoExpedicion AS pe
             JOIN FETCH c.tipoFactura AS tf
             JOIN FETCH c.usuario AS usu
             JOIN FETCH c.servicio AS servi
             JOIN FETCH c.estado AS e  
            WHERE c.comprobantePK in :comprobantePKs  
            ORDER BY  c.fechaPago DESC    
                   """,
            countQuery = "SELECT COUNT(c) FROM Comprobante c WHERE c.comprobantePK in :comprobantePKs  ")
    List<Comprobante> getComprobantesCuenta(@Param("comprobantePKs") List<ComprobantePK> comprobantePKs);

//    Encontrar comprobante por  ComprobantePK
    @Query("""
            SELECT c FROM Comprobante AS c 
                        JOIN FETCH c.comprobantePK AS cPK
                        JOIN FETCH c.detalleComprobante AS dtc
                        JOIN FETCH c.sucursal AS suc
                        JOIN FETCH suc.ciudad ciud
                        JOIN FETCH c.puntoExpedicion AS pe
                        JOIN FETCH c.tipoFactura AS tf
                        JOIN FETCH c.condicionVenta AS cv
                        JOIN FETCH c.usuario AS usu
                        JOIN FETCH c.cobrador AS cob
                        JOIN FETCH c.servicio AS servi
                        JOIN FETCH servi.categoria AS cat
                        JOIN FETCH c.estado AS e  
           WHERE c.comprobantePK=?1
           """)
    Comprobante getComprobante(ComprobantePK comprobantePK);

//   Generar numero de comprobantes
    @Query("""
           SELECT MAX(cPK.numeroComprobante) AS numeroComprobante FROM Comprobante c 
           JOIN  c.comprobantePK AS cPK
           WHERE cPK.codigoSucursal= ?1
           AND cPK.codigoPuntoExpedicion= ?2 
           AND cPK.codigoTipoFactura= ?3 
           AND cPK.codigoSerie= ?4 """)
    Integer getNumeroComprobante(Integer codigoSucursal, Integer codigoPuntoExpedicion, Integer codigoTipoFactura, Integer codigoSerie);

    @Query("SELECT SUM(c.cantidadPago) FROM Comprobante c  "
            + "WHERE c.servicio.cuentaCorriente= ?1 ")
    Integer getCantidadPago(String cuentaCorriente);

    @Query("""
         SELECT c  FROM Comprobante c 
            JOIN FETCH c.detalleComprobante AS dtc
            WHERE c.fechaPago = (SELECT MAX(c2.fechaPago) FROM Comprobante c2 WHERE c2.servicio.cuentaCorriente = ?1)
            AND c.servicio.cuentaCorriente = ?1
       """)
    Optional<Comprobante> getUltimoComprobanteCuenta(String cuentaCorriente);

   
}
