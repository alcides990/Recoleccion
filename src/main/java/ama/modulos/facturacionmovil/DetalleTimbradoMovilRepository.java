package ama.modulos.facturacionmovil;

import ama.dominio.Timbrado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetalleTimbradoMovilRepository extends JpaRepository<Timbrado, Integer> {

    @Query(value = """
            SELECT dt.codigo_timbrado, t.numero_timbrado,
                   COALESCE(dt.codigo_serie, 0), COALESCE(s.serie, ''), dt.modo_emision,
                   dt.numero_desde, dt.numero_hasta
              FROM detalle_timbrado dt
              JOIN timbrados t ON t.codigo_timbrado = dt.codigo_timbrado
              LEFT JOIN series s ON s.codigo_serie = dt.codigo_serie
             WHERE dt.codigo_sucursal = :sucursal
               AND dt.codigo_punto_expedicion = :punto
               AND UPPER(dt.modo_emision) = :modoEmision
               AND dt.codigo_estado = 1
               AND t.codigo_estado = 1
               AND t.fecha_inicio <= CURRENT_DATE
               AND t.fecha_fin >= CURRENT_DATE
             ORDER BY t.numero_timbrado DESC, s.serie
            """, nativeQuery = true)
    List<Object[]> buscarActivos(@Param("sucursal") Integer sucursal,
            @Param("punto") Integer punto,
            @Param("modoEmision") String modoEmision);
}
