package ama.servicio;

import ama.dominio.DetalleTimbrado;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumeradorAutoimpresorService {

    private static final int MAXIMO_NUMERO_FISCAL = 9_999_999;
    private final JdbcTemplate jdbc;

    public NumeradorAutoimpresorService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Reserva el siguiente número dentro de la transacción que emitirá el
     * comprobante. El bloqueo FOR UPDATE impide que dos cajas obtengan el mismo
     * número. Si la emisión se revierte, el avance del numerador también.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int reservar(DetalleTimbrado detalle, Integer codigoTipoComprobante) {
        if (detalle == null || !detalle.esAutoimpresor()) {
            throw new IllegalArgumentException("El timbrado seleccionado no está configurado como autoimpresor.");
        }
        int desde = detalle.getNumeroDesde() == null ? 1 : detalle.getNumeroDesde();
        int hasta = detalle.getNumeroHasta() == null ? MAXIMO_NUMERO_FISCAL : detalle.getNumeroHasta();
        var id = detalle.getDetalleTimbradoPK();
        int serie = detalle.getSerie() == null ? 0 : detalle.getSerie().getCodigoSerie();

        Integer maximoExistente = jdbc.queryForObject("""
            SELECT COALESCE(MAX(numero_comprobante), 0)
              FROM comprobantes
             WHERE codigo_sucursal = ?
               AND codigo_punto_expedicion = ?
               AND codigo_tipo_comprobante = ?
               AND codigo_serie = ?
            """, Integer.class, id.getCodigoSucursal(), id.getCodigoPuntoExpedicion(),
                codigoTipoComprobante, serie);
        int inicial = Math.max(desde - 1, maximoExistente == null ? 0 : maximoExistente);

        jdbc.update("""
            INSERT INTO numeradores_autoimpresor
                (codigo_timbrado, codigo_punto_expedicion, codigo_sucursal,
                 codigo_tipo_comprobante, codigo_serie, ultimo_numero)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE ultimo_numero = ultimo_numero
            """, id.getCodigoTimbrado(), id.getCodigoPuntoExpedicion(), id.getCodigoSucursal(),
                codigoTipoComprobante, serie, inicial);

        Integer ultimo = jdbc.queryForObject("""
            SELECT ultimo_numero
              FROM numeradores_autoimpresor
             WHERE codigo_timbrado = ? AND codigo_punto_expedicion = ?
               AND codigo_sucursal = ? AND codigo_tipo_comprobante = ?
               AND codigo_serie = ?
             FOR UPDATE
            """, Integer.class, id.getCodigoTimbrado(), id.getCodigoPuntoExpedicion(),
                id.getCodigoSucursal(), codigoTipoComprobante, serie);
        int siguiente = Math.max(desde, (ultimo == null ? inicial : ultimo) + 1);
        if (siguiente > hasta || siguiente > MAXIMO_NUMERO_FISCAL) {
            throw new IllegalStateException("Se agotó el rango autorizado del autoimpresor ("
                    + desde + " a " + hasta + ").");
        }
        jdbc.update("""
            UPDATE numeradores_autoimpresor
               SET ultimo_numero = ?
             WHERE codigo_timbrado = ? AND codigo_punto_expedicion = ?
               AND codigo_sucursal = ? AND codigo_tipo_comprobante = ?
               AND codigo_serie = ?
            """, siguiente, id.getCodigoTimbrado(), id.getCodigoPuntoExpedicion(),
                id.getCodigoSucursal(), codigoTipoComprobante, serie);
        return siguiente;
    }
}
