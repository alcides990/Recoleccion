package ama.modulos.rg90;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Rg90ComprobanteRepository extends JpaRepository<Comprobante, ComprobantePK> {

    @EntityGraph(attributePaths = {
        "usuario",
        "usuario.tipoDocumento",
        "tipoComprobante",
        "condicionVenta",
        "timbrado",
        "puntoExpedicion",
        "puntoExpedicion.sucursal"
    })
    @Query("""
            select c
             from Comprobante c
             where c.comprobantePK.puntoExpedicionPK.codigoSucursal = :sucursal
               and c.estado.codigoEstado = 1
               and c.fechaPago between :desde and :hasta
               and (:tipoComprobante = 0
                    or c.comprobantePK.codigoTipoComprobante = :tipoComprobante)
             order by c.fechaPago,
                      c.comprobantePK.puntoExpedicionPK.codigoPuntoExpedicion,
                      c.comprobantePK.codigoSerie,
                      c.comprobantePK.numeroComprobante
            """)
    List<Comprobante> ventasRg90(@Param("sucursal") Integer sucursal,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("tipoComprobante") Integer tipoComprobante);
}
