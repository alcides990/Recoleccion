package ama.modulos.comprobantesv2;

import ama.dominio.Cobrador;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface CobradorJpaRepositoryV2 extends JpaRepository<Cobrador, Integer> {

    @EntityGraph(attributePaths = "estado")
    List<Cobrador> findBySucursal_CodigoSucursalOrderByNombreAscApellidoAsc(
            @Param("codigoSucursal") Integer codigoSucursal);

    @EntityGraph(attributePaths = "estado")
    List<Cobrador> findBySucursal_CodigoSucursalAndEstado_CodigoEstadoOrderByNombreAscApellidoAsc(
            @Param("codigoSucursal") Integer codigoSucursal,
            @Param("codigoEstado") Integer codigoEstado);
}
