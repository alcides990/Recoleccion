package ama.dao;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.Servicio;
import java.util.List;
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
            LEFT JOIN FETCH c.comprobantePK AS cPK
            JOIN FETCH c.detalleComprobante AS dtc
            LEFT JOIN FETCH c.sucursal AS suc
            LEFT JOIN FETCH c.puntoExpedicion AS pe
            LEFT JOIN FETCH pe.empresa AS emp
            LEFT JOIN FETCH c.tipoFactura AS tf
            LEFT JOIN FETCH c.usuario AS usu
            LEFT JOIN FETCH c.servicio AS servi
            LEFT JOIN FETCH c.estado AS e  """,
            countQuery = "SELECT COUNT(c) FROM Comprobante c")
    Page<Comprobante> getAllComprobantes(Pageable pageable);

    //filtro de comprobantes
    @Query(value = """
            SELECT c FROM Comprobante AS c 
            LEFT JOIN FETCH c.comprobantePK AS cPK
            JOIN FETCH c.detalleComprobante AS dtc
            LEFT JOIN FETCH c.sucursal AS suc
            LEFT JOIN FETCH c.puntoExpedicion AS pe
            LEFT JOIN FETCH c.tipoFactura AS tf
            LEFT JOIN FETCH c.usuario AS usu
            LEFT JOIN FETCH c.servicio AS servi
            LEFT JOIN FETCH c.cobrador AS cob
            LEFT JOIN FETCH c.estado AS e  
            LEFT JOIN FETCH c.usuarioSistema AS uSist
                   WHERE CONCAT(suc.nombreSucursal, '-', pe.nombrePuntoExpedicion, '-', cPK.numeroComprobante) LIKE  %?1% 
              """,
            countQuery = "SELECT COUNT(c) FROM Comprobante c ")
    Page<Comprobante> filtrarComprobantes(Pageable pageable, String flitro);

    //recuperar comprobantes de la cuenta paginados
    @Query(value = """
                      SELECT c.comprobantePK FROM Comprobante AS c 
                     WHERE c.servicio = :servicio
                   """,
            countQuery = "SELECT COUNT(c) FROM Comprobante c WHERE c.servicio =:servicio")
    Page<ComprobantePK> getComprobantePKs(Pageable pageable, @Param("servicio") Servicio servicio);

    @Query(value = """
            SELECT c FROM Comprobante AS c 
            JOIN FETCH c.detalleComprobante AS dtc
            LEFT JOIN FETCH c.sucursal AS suc
            LEFT JOIN FETCH c.puntoExpedicion AS pe
            LEFT JOIN FETCH c.tipoFactura AS tf
            LEFT JOIN FETCH c.usuario AS usu
            LEFT JOIN FETCH c.servicio AS servi
            LEFT JOIN FETCH c.estado AS e  
            WHERE c.comprobantePK in :comprobantePKs      
                   """,
            countQuery = "SELECT COUNT(c) FROM Comprobante c WHERE c.comprobantePK in :comprobantePKs  ")
    List<Comprobante> getComprobantesCuenta(@Param("comprobantePKs") List<ComprobantePK> comprobantePKs);

//    Encontrar comprobante por  ComprobantePK
    @Query("""
            SELECT c FROM Comprobante AS c 
                       LEFT JOIN FETCH c.comprobantePK AS cPK
                       JOIN FETCH c.detalleComprobante AS dtc
                       LEFT JOIN FETCH c.sucursal AS suc
                       LEFT JOIN FETCH suc.ciudad ciud
                       LEFT JOIN FETCH c.puntoExpedicion AS pe
                       LEFT JOIN FETCH c.tipoFactura AS tf
                       LEFT JOIN FETCH c.condicionVenta AS cv
                       LEFT JOIN FETCH c.usuario AS usu
                       LEFT JOIN FETCH c.cobrador AS cob
                       LEFT JOIN FETCH c.servicio AS servi
                       LEFT JOIN FETCH servi.categoria AS cat
                       LEFT JOIN FETCH c.estado AS e  
           WHERE c.comprobantePK=?1
           """)
    Comprobante getComprobante(ComprobantePK comprobantePK);

//   Generar numero de comprobantes
    @Query("""
           SELECT MAX(cPK.numeroComprobante) AS numeroComprobante FROM Comprobante c 
           LEFT JOIN  c.comprobantePK AS cPK
           WHERE cPK.codigoSucursal= ?1
           AND cPK.codigoPuntoExpedicion= ?2 
           AND cPK.codigoTipoFactura= ?3 
           AND cPK.codigoSerie= ?4 """)
    Integer getNumeroComprobante(Integer codigoSucursal, Integer codigoPuntoExpedicion, Integer codigoTipoFactura, Integer codigoSerie);

}
