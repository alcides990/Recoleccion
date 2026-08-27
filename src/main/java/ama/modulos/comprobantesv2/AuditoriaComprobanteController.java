package ama.modulos.comprobantesv2;

import ama.dominio.UsuarioSistema;
import jakarta.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class AuditoriaComprobanteController {

    private static final List<String> ACCIONES = List.of(
            "EMISION", "IMPRESION", "REIMPRESION", "ANULACION", "INTENTO_MODIFICACION");

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
            @RequestParam(required = false) String accion) {
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
        FiltroSql filtro = filtro(sucursal, numero, accionNormalizada, busqueda);
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auditoria_comprobantes WHERE codigo_sucursal = ?",
                Long.class, sucursal);
        long filtrados = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM auditoria_comprobantes a
                  LEFT JOIN usuarios_sistema us
                         ON us.codigo_usuario_sistema = a.codigo_usuario_sistema
                """ + filtro.sql(), Long.class, filtro.parametros().toArray());

        String orden = columnaOrden(columna);
        String sentido = "asc".equalsIgnoreCase(direccion) ? "ASC" : "DESC";
        List<Object> parametros = new ArrayList<>(filtro.parametros());
        parametros.add(limite);
        parametros.add(desplazamiento);
        List<AuditoriaComprobanteFila> eventos = jdbc.query("""
            SELECT a.codigo_auditoria, a.fecha_evento, a.accion,
                   CONCAT(LPAD(a.codigo_sucursal, 3, '0'), '-',
                          LPAD(a.codigo_punto_expedicion, 3, '0'), '-',
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
            """ + filtro.sql() + " ORDER BY " + orden + " " + sentido
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

    private FiltroSql filtro(Integer sucursal, Integer numero, String accion, String busqueda) {
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
        if (busqueda != null && !busqueda.isBlank()) {
            String patron = "%" + busqueda.trim() + "%";
            sql.append("""
                 AND (CAST(a.numero_comprobante AS CHAR) LIKE ?
                      OR a.accion LIKE ?
                      OR COALESCE(us.usuario, '') LIKE ?
                      OR COALESCE(a.motivo, '') LIKE ?
                      OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(a.datos_comprobante,
                                  '$.numeroTimbradoFiscal')), '') LIKE ?)
                """);
            for (int i = 0; i < 5; i++) parametros.add(patron);
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

    private record FiltroSql(String sql, List<Object> parametros) {
    }
}
