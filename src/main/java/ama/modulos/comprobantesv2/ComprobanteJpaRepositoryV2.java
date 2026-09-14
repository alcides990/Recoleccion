package ama.modulos.comprobantesv2;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ComprobanteJpaRepositoryV2 extends JpaRepository<Comprobante, ComprobantePK>, JpaSpecificationExecutor<Comprobante> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        UPDATE comprobantes
           SET numero_comprobante = :numeroNuevo
         WHERE numero_comprobante = :numeroActual
           AND codigo_punto_expedicion = :punto
           AND codigo_sucursal = :sucursal
           AND codigo_tipo_comprobante = :tipo
           AND codigo_serie = :serie
        """, nativeQuery = true)
    int actualizarNumero(@Param("numeroActual") Integer numeroActual,
            @Param("numeroNuevo") Integer numeroNuevo,
            @Param("punto") Integer punto,
            @Param("sucursal") Integer sucursal,
            @Param("tipo") Integer tipo,
            @Param("serie") Integer serie);

    @Override
    @EntityGraph(attributePaths = {"puntoExpedicion", "tipoComprobante", "servicio", "usuario", "estado", "cobrador", "categoria", "condicionVenta", "serie", "detallePago", "detallePago.metodoPago"})
    Optional<Comprobante> findById(ComprobantePK comprobantePK);

    Optional<Comprobante> findFirstByServicio_CuentaCorrienteAndComprobantePK_PuntoExpedicionPK_CodigoSucursalAndEstado_CodigoEstadoOrderByFechaEmisionDesc(
            String cuentaCorriente, Integer codigoSucursal, Integer codigoEstado);

    @Override
    @EntityGraph(attributePaths = {"puntoExpedicion", "tipoComprobante", "servicio", "usuario", "estado"})
    Page<Comprobante> findAll(Specification<Comprobante> specification, Pageable pageable);
}
