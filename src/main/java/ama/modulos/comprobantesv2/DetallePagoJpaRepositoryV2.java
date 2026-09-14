package ama.modulos.comprobantesv2;

import ama.dominio.DetallePago;
import ama.dominio.DetallePagoPK;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePagoJpaRepositoryV2 extends JpaRepository<DetallePago, DetallePagoPK> {
}
