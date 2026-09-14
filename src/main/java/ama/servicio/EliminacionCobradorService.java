package ama.servicio;

import ama.dominio.Cobrador;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminacionCobradorService {

    private final JdbcTemplate jdbcTemplate;
    private final CobradorService cobradorService;

    public EliminacionCobradorService(JdbcTemplate jdbcTemplate,
            CobradorService cobradorService) {
        this.jdbcTemplate = jdbcTemplate;
        this.cobradorService = cobradorService;
    }

    @Transactional
    public void eliminar(Cobrador cobrador) {
        Integer codigo = cobrador.getCodigoCobrador();
        List<String> relaciones = new ArrayList<>();
        agregarRelacion(relaciones, "comprobantes", contar("comprobantes", codigo));
        agregarRelacion(relaciones, "zonas", contar("zonas", codigo));
        agregarRelacion(relaciones, "recorridos", contar("recorridos_cobrador", codigo));
        agregarRelacion(relaciones, "dispositivos", contar("dispositivos_cobrador", codigo));

        if (!relaciones.isEmpty()) {
            throw new CobradorConRegistrosRelacionadosException(relaciones);
        }
        cobradorService.eliminar(cobrador);
    }

    private int contar(String tabla, Integer codigoCobrador) {
        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tabla + " WHERE codigo_cobrador = ?",
                Integer.class, codigoCobrador);
        return cantidad == null ? 0 : cantidad;
    }

    private void agregarRelacion(List<String> relaciones, String nombre, int cantidad) {
        if (cantidad > 0) {
            relaciones.add(nombre + " (" + cantidad + ")");
        }
    }

    public static class CobradorConRegistrosRelacionadosException extends RuntimeException {

        public CobradorConRegistrosRelacionadosException(List<String> relaciones) {
            super("Tiene registros relacionados: " + String.join(", ", relaciones) + ".");
        }
    }
}
