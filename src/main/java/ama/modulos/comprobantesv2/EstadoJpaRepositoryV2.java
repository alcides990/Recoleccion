package ama.modulos.comprobantesv2;

import ama.dominio.Estado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoJpaRepositoryV2 extends JpaRepository<Estado, Integer> {

    List<Estado> findByCodigoEstadoNotOrderByEstadoAsc(Integer codigoEstado);

    List<Estado> findByCodigoEstadoInOrderByCodigoEstadoAsc(List<Integer> codigosEstado);
}
