package ama.dao;

import ama.dominio.PuntoExpedicion;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface PuntoExpedicionDao extends CrudRepository<PuntoExpedicion, Integer> {

    @Query("""
           SELECT p FROM PuntoExpedicion p 
           INNER JOIN FETCH p.sucursal
           INNER JOIN FETCH p.estado
           """)
    List<PuntoExpedicion> getPuntosExpedicion();

    @Query("SELECT MAX(p.codigoPuntoExpedicion) as codigoPuntoExpedicon FROM PuntoExpedicion p ")
    Integer getCodigoPuntoExpedicion();
}
