package ama.dao;

import ama.dominio.DetalleTimbrado;
import ama.dominio.DetalleTimbradoPK;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleTimbradoDao extends JpaRepository<DetalleTimbrado, DetalleTimbradoPK> {

    @Override
    @EntityGraph(attributePaths = {"timbrado", "puntoExpedicion", "puntoExpedicion.sucursal", "serie", "estado"})
    List<DetalleTimbrado> findAll();

    @EntityGraph(attributePaths = {"timbrado", "puntoExpedicion", "puntoExpedicion.sucursal", "serie", "estado"})
    List<DetalleTimbrado> findByDetalleTimbradoPKCodigoSucursalOrderByDetalleTimbradoPKCodigoTimbradoAsc(Integer codigoSucursal);
}
