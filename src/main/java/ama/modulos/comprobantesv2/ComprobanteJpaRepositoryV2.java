package ama.modulos.comprobantesv2;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface ComprobanteJpaRepositoryV2 extends JpaRepository<Comprobante, ComprobantePK>, JpaSpecificationExecutor<Comprobante> {
    @Override
    @EntityGraph(attributePaths = {"puntoExpedicion", "tipoComprobante", "servicio", "usuario", "estado", "cobrador", "categoria", "condicionVenta", "serie", "detallePago", "detallePago.metodoPago"})
    Optional<Comprobante> findById(ComprobantePK comprobantePK);

    Optional<Comprobante> findFirstByServicio_CuentaCorrienteAndEstado_CodigoEstadoOrderByFechaEmisionDesc(
            String cuentaCorriente, Integer codigoEstado);

    @Override
    @EntityGraph(attributePaths = {"puntoExpedicion", "tipoComprobante", "servicio", "usuario", "estado"})
    Page<Comprobante> findAll(Specification<Comprobante> specification, Pageable pageable);
}
