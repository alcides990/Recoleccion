package ama.modulos.comprobantesv2;

import ama.dominio.UsuarioSistema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/comprobantes-v2/auditoria")
@PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
public class AuditoriaComprobanteController {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String ESTABLECIMIENTO_FISCAL = """
            COALESCE(
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(a.datos_comprobante,
                    '$.establecimientoFiscal')), 'null'),
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(a.datos_anteriores,
                    '$.establecimientoFiscal')), 'null'),
                NULLIF(c.establecimiento_fiscal, ''),
                LPAD(a.codigo_sucursal, 3, '0'))
            """;
    private static final String PUNTO_EXPEDICION_FISCAL = """
            COALESCE(
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(a.datos_comprobante,
                    '$.puntoExpedicionFiscal')), 'null'),
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(a.datos_anteriores,
                    '$.puntoExpedicionFiscal')), 'null'),
                NULLIF(c.punto_expedicion_fiscal, ''),
                LPAD(a.codigo_punto_expedicion, 3, '0'))
            """;

    private static final List<String> ACCIONES = List.of(
            "EMISION", "MODIFICACION", "IMPRESION", "REIMPRESION", "ANULACION",
            "INTENTO_MODIFICACION");

    private final JdbcTemplate jdbc;
    private final HttpSession session;

    public AuditoriaComprobanteController(JdbcTemplate jdbc, HttpSession session) {
        this.jdbc = jdbc;
        this.session = session;
    }

    @GetMapping
    public String pagina(Model model) {
        UsuarioSistema usuarioSesion = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuarioSesion == null || usuarioSesion.getSucursal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        model.addAttribute("titulo", "Auditoría de comprobantes");
        model.addAttribute("acciones", ACCIONES);
        return "comprobantesv2/auditoria";
    }

    @PostMapping("/tabla")
    @ResponseBody
    public ResponseEntity<DataTableResponseV2<AuditoriaComprobanteFila>> tabla(
            @RequestParam int draw,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "25") int length,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(name = "order[0][column]", defaultValue = "0") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "desc") String direccion,
            @RequestParam(required = false) Integer numero,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String establecimiento,
            @RequestParam(required = false) String puntoExpedicion) {
        UsuarioSistema usuarioSesion = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuarioSesion == null || usuarioSesion.getSucursal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String accionNormalizada = accion == null ? "" : accion.trim().toUpperCase();
        if (!accionNormalizada.isEmpty() && !ACCIONES.contains(accionNormalizada)) {
            return ResponseEntity.badRequest().build();
        }
        int limite = Math.min(Math.max(length, 1), 100);
        int desplazamiento = Math.max(start, 0);
        Integer sucursal = usuarioSesion.getSucursal().getCodigoSucursal();
        FiltroSql filtro = filtro(sucursal, numero, accionNormalizada, establecimiento,
                puntoExpedicion, busqueda);
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auditoria_comprobantes WHERE codigo_sucursal = ?",
                Long.class, sucursal);
        long filtrados = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM auditoria_comprobantes a
                  LEFT JOIN usuarios_sistema us
                         ON us.codigo_usuario_sistema = a.codigo_usuario_sistema
                  LEFT JOIN comprobantes c
                         ON c.numero_comprobante = a.numero_comprobante
                        AND c.codigo_punto_expedicion = a.codigo_punto_expedicion
                        AND c.codigo_sucursal = a.codigo_sucursal
                        AND c.codigo_tipo_comprobante = a.codigo_tipo_comprobante
                        AND c.codigo_serie = a.codigo_serie
                """ + filtro.sql(), Long.class, filtro.parametros().toArray());

        String orden = columnaOrden(columna);
        String sentido = "asc".equalsIgnoreCase(direccion) ? "ASC" : "DESC";
        List<Object> parametros = new ArrayList<>(filtro.parametros());
        parametros.add(limite);
        parametros.add(desplazamiento);
        String consulta = """
            SELECT a.codigo_auditoria, a.fecha_evento, a.accion,
                   CONCAT(%s, '-', %s, '-',
                          LPAD(a.numero_comprobante, 7, '0')) documento,
                   COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.datos_comprobante,
                            '$.numeroTimbradoFiscal')), '') timbrado,
                   COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.datos_comprobante,
                            '$.serieFiscal')), CAST(a.codigo_serie AS CHAR)) serie,
                   COALESCE(us.usuario, CONCAT('Usuario ', a.codigo_usuario_sistema), 'Sistema') usuario,
                   COALESCE(a.motivo, '') motivo
              FROM auditoria_comprobantes a
              LEFT JOIN usuarios_sistema us
                     ON us.codigo_usuario_sistema = a.codigo_usuario_sistema
              LEFT JOIN comprobantes c
                     ON c.numero_comprobante = a.numero_comprobante
                    AND c.codigo_punto_expedicion = a.codigo_punto_expedicion
                    AND c.codigo_sucursal = a.codigo_sucursal
                    AND c.codigo_tipo_comprobante = a.codigo_tipo_comprobante
                    AND c.codigo_serie = a.codigo_serie
            """.formatted(ESTABLECIMIENTO_FISCAL, PUNTO_EXPEDICION_FISCAL);
        List<AuditoriaComprobanteFila> eventos = jdbc.query(
                consulta + filtro.sql() + " ORDER BY " + orden + " " + sentido
                + ", a.codigo_auditoria DESC LIMIT ? OFFSET ?",
                (rs, fila) -> new AuditoriaComprobanteFila(
                rs.getLong("codigo_auditoria"),
                timestamp(rs.getTimestamp("fecha_evento")),
                rs.getString("accion"),
                rs.getString("documento"),
                rs.getString("timbrado"),
                rs.getString("serie"),
                rs.getString("usuario"),
                rs.getString("motivo")), parametros.toArray());
        return ResponseEntity.ok(new DataTableResponseV2<>(draw, total, filtrados, eventos));
    }

    @PostMapping("/detalle")
    @ResponseBody
    public ResponseEntity<AuditoriaComprobanteComparacion> detalle(@RequestParam Long codigo) {
        UsuarioSistema usuarioSesion = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuarioSesion == null || usuarioSesion.getSucursal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<AuditoriaComprobanteComparacion> resultados = jdbc.query("""
            SELECT codigo_auditoria, accion, datos_anteriores, datos_comprobante
              FROM auditoria_comprobantes
             WHERE codigo_auditoria = ? AND codigo_sucursal = ?
            """, (rs, fila) -> new AuditoriaComprobanteComparacion(
                rs.getLong("codigo_auditoria"), rs.getString("accion"),
                describirRelaciones(rs.getString("datos_anteriores"),
                        usuarioSesion.getSucursal().getCodigoSucursal()),
                describirRelaciones(rs.getString("datos_comprobante"),
                        usuarioSesion.getSucursal().getCodigoSucursal())),
                codigo, usuarioSesion.getSucursal().getCodigoSucursal());
        return resultados.isEmpty() ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(resultados.get(0));
    }

    String describirRelaciones(String datosJson, Integer sucursal) {
        if (datosJson == null || datosJson.isBlank()) return datosJson;
        try {
            JsonNode raiz = JSON.readTree(datosJson);
            if (!(raiz instanceof ObjectNode datos)) return datosJson;
            reemplazar(datos, "usuario", """
                    SELECT TRIM(CONCAT_WS(' ', NULLIF(nombre, ''), NULLIF(apellido, '')))
                      FROM usuarios
                     WHERE codigo_usuario = ? AND codigo_sucursal = ?
                     LIMIT 1
                    """, sucursal);
            reemplazar(datos, "categoria", """
                    SELECT categoria FROM categorias
                     WHERE codigo_categoria = ? AND codigo_sucursal = ?
                     LIMIT 1
                    """, sucursal);
            reemplazar(datos, "cobrador", """
                    SELECT TRIM(CONCAT_WS(' ', NULLIF(nombre, ''), NULLIF(apellido, '')))
                      FROM cobradores
                     WHERE codigo_cobrador = ? AND codigo_sucursal = ?
                     LIMIT 1
                    """, sucursal);
            reemplazar(datos, "condicionVenta", """
                    SELECT condicion_venta FROM condiciones_venta
                     WHERE codigo_condicion_venta = ?
                     LIMIT 1
                    """);
            reemplazar(datos, "estado", """
                    SELECT estado FROM estados
                     WHERE codigo_estado = ?
                     LIMIT 1
                    """);
            describirPagos(datos);
            return JSON.writeValueAsString(datos);
        } catch (JsonProcessingException | RuntimeException excepcion) {
            return datosJson;
        }
    }

    private void reemplazar(ObjectNode datos, String campo, String consulta,
            Object... parametrosAdicionales) {
        Integer codigo = codigo(datos.get(campo));
        if (codigo == null) return;
        Object[] parametros = new Object[parametrosAdicionales.length + 1];
        parametros[0] = codigo;
        System.arraycopy(parametrosAdicionales, 0, parametros, 1,
                parametrosAdicionales.length);
        List<String> valores = jdbc.query(consulta,
                (rs, fila) -> rs.getString(1), parametros);
        if (!valores.isEmpty() && valores.get(0) != null
                && !valores.get(0).isBlank()) {
            datos.put(campo, valores.get(0));
        }
    }

    private void describirPagos(ObjectNode datos) {
        JsonNode pagos = datos.get("pagos");
        if (!(pagos instanceof ArrayNode lista)) return;
        for (JsonNode pago : lista) {
            if (!(pago instanceof ObjectNode detalle)) continue;
            Integer codigo = codigo(detalle.get("codigoMetodoPago"));
            if (codigo == null) continue;
            List<String> valores = jdbc.query("""
                    SELECT metodo_pago FROM metodos_pago
                     WHERE codigo_metodo_pago = ?
                     LIMIT 1
                    """, (rs, fila) -> rs.getString(1), codigo);
            if (!valores.isEmpty() && valores.get(0) != null
                    && !valores.get(0).isBlank()) {
                detalle.put("medioPago", valores.get(0));
                detalle.remove("codigoMetodoPago");
            }
        }
    }

    private Integer codigo(JsonNode valor) {
        if (valor == null || valor.isNull()) return null;
        if (valor.canConvertToInt()) return valor.intValue();
        if (valor.isTextual() && valor.textValue().matches("\\d+")) {
            return Integer.valueOf(valor.textValue());
        }
        return null;
    }

    private FiltroSql filtro(Integer sucursal, Integer numero, String accion,
            String establecimiento, String puntoExpedicion, String busqueda) {
        StringBuilder sql = new StringBuilder(" WHERE a.codigo_sucursal = ?");
        List<Object> parametros = new ArrayList<>();
        parametros.add(sucursal);
        if (numero != null) {
            sql.append(" AND a.numero_comprobante = ?");
            parametros.add(numero);
        }
        if (!accion.isEmpty()) {
            sql.append(" AND a.accion = ?");
            parametros.add(accion);
        }
        if (establecimiento != null && !establecimiento.isBlank()) {
            sql.append(" AND ").append(ESTABLECIMIENTO_FISCAL).append(" = ?");
            parametros.add(normalizarCodigoFiscal(establecimiento));
        }
        if (puntoExpedicion != null && !puntoExpedicion.isBlank()) {
            sql.append(" AND ").append(PUNTO_EXPEDICION_FISCAL).append(" = ?");
            parametros.add(normalizarCodigoFiscal(puntoExpedicion));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            String patron = "%" + busqueda.trim() + "%";
            sql.append(" AND (CAST(a.numero_comprobante AS CHAR) LIKE ?")
                    .append(" OR a.accion LIKE ?")
                    .append(" OR COALESCE(us.usuario, '') LIKE ?")
                    .append(" OR COALESCE(a.motivo, '') LIKE ?")
                    .append(" OR ").append(ESTABLECIMIENTO_FISCAL).append(" LIKE ?")
                    .append(" OR ").append(PUNTO_EXPEDICION_FISCAL).append(" LIKE ?")
                    .append(" OR CONCAT(").append(ESTABLECIMIENTO_FISCAL)
                    .append(", '-', ").append(PUNTO_EXPEDICION_FISCAL)
                    .append(", '-', LPAD(a.numero_comprobante, 7, '0')) LIKE ?")
                    .append(" OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(")
                    .append("a.datos_comprobante, '$.numeroTimbradoFiscal')), '') LIKE ?)");
            for (int i = 0; i < 8; i++) parametros.add(patron);
        }
        return new FiltroSql(sql.toString(), parametros);
    }

    private String columnaOrden(int columna) {
        return switch (columna) {
            case 1 -> "a.accion";
            case 2 -> "a.numero_comprobante";
            case 3 -> "timbrado";
            case 4 -> "a.codigo_serie";
            case 5 -> "usuario";
            case 6 -> "a.motivo";
            default -> "a.fecha_evento";
        };
    }

    private LocalDateTime timestamp(Timestamp valor) {
        return valor == null ? null : valor.toLocalDateTime();
    }

    private String normalizarCodigoFiscal(String valor) {
        String limpio = valor.trim();
        if (limpio.matches("\\d{1,3}")) {
            return String.format("%03d", Integer.parseInt(limpio));
        }
        return limpio;
    }

    private record FiltroSql(String sql, List<Object> parametros) {
    }
}
