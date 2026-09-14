package ama.modulos.facturacionmovil;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ComprobanteMovilRepository extends Repository<Comprobante, ComprobantePK> {
    @Query(value = """
            SELECT c.numero_comprobante, c.fecha_pago, c.cuenta_corriente,
                   c.razon_social, c.total_importe, c.saldo,
                   p.punto_expedicion, s.serie,
                   c.codigo_tipo_comprobante, c.periodo_pago,
                   c.cantidad_pago, c.codigo_estado,
                   c.codigo_punto_expedicion, c.codigo_serie
              FROM comprobantes c
              JOIN puntos_expedicion p
                ON p.codigo_punto_expedicion = c.codigo_punto_expedicion
               AND p.codigo_sucursal = c.codigo_sucursal
              JOIN series s ON s.codigo_serie = c.codigo_serie
             WHERE c.codigo_usuario_sistema = :usuario
               AND c.codigo_sucursal = :sucursal
             ORDER BY c.fecha_emision DESC, c.numero_comprobante DESC
             LIMIT 100
            """, nativeQuery = true)
    List<Object[]> ultimosDelUsuario(@Param("usuario") Integer usuario,
            @Param("sucursal") Integer sucursal);
}
