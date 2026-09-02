package ama.controladorMVC;

import ama.dominio.UsuarioSistema;
import ama.modulos.comprobantesv2.DataTableResponseV2;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/auditoria-entidades")
@PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
public class AuditoriaEntidadController {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> ENTIDADES = List.of(
            "USUARIO", "CUENTA", "CATEGORIA", "USUARIO_SISTEMA");
    private static final List<String> ACCIONES = List.of("ALTA", "MODIFICACION", "ELIMINACION");
    private final JdbcTemplate jdbc;
    private final HttpSession session;

    public AuditoriaEntidadController(JdbcTemplate jdbc, HttpSession session) {
        this.jdbc = jdbc;
        this.session = session;
    }

    @GetMapping
    public String pagina(Model modelo) {
        modelo.addAttribute("titulo", "Auditoría operativa");
        modelo.addAttribute("entidades", ENTIDADES);
        modelo.addAttribute("acciones", ACCIONES);
        return "auditoria/entidades";
    }

    @PostMapping("/tabla")
    @ResponseBody
    public DataTableResponseV2<Map<String, Object>> tabla(
            @RequestParam int draw, @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(defaultValue = "") String entidad,
            @RequestParam(defaultValue = "") String accion,
            @RequestParam(defaultValue = "") String identificador,
            @RequestParam(name = "order[0][column]", defaultValue = "0") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "desc") String direccion) {
        String entidadFiltro = normalizar(entidad, ENTIDADES);
        String accionFiltro = normalizar(accion, ACCIONES);
        String identificadorFiltro = identificador == null ? "" : identificador.trim();
        String filtro = busqueda == null ? "" : busqueda.trim();
        String like = "%" + filtro + "%";
        int limite = Math.min(Math.max(length, 1), 100);
        int inicio = Math.max(start, 0);
        int sucursal = usuario().getSucursal().getCodigoSucursal();
        String[] ordenes = {"a.fecha_evento", "a.entidad", "a.accion", "a.identificador", "us.usuario"};
        String orden = ordenes[Math.max(0, Math.min(columna, ordenes.length - 1))];
        String sentido = "asc".equalsIgnoreCase(direccion) ? "ASC" : "DESC";
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auditoria_entidades WHERE codigo_sucursal = ?",
                Long.class, sucursal);
        String condiciones = """
             FROM auditoria_entidades a
             LEFT JOIN usuarios_sistema us ON us.codigo_usuario_sistema = a.codigo_usuario_sistema
            WHERE a.codigo_sucursal = ?
              AND (? = '' OR a.entidad = ?)
              AND (? = '' OR a.accion = ?)
              AND (? = '' OR a.identificador = ?)
              AND (? = '' OR a.identificador LIKE ? OR COALESCE(us.usuario, '') LIKE ?
                   OR CAST(a.datos_antes AS CHAR) LIKE ? OR CAST(a.datos_despues AS CHAR) LIKE ?)
            """;
        Object[] parametros = {sucursal, entidadFiltro, entidadFiltro,
            accionFiltro, accionFiltro, identificadorFiltro, identificadorFiltro,
            filtro, like, like, like, like};
        Long filtrado = jdbc.queryForObject("SELECT COUNT(*) " + condiciones, Long.class, parametros);
        String sql = """
            SELECT a.codigo_auditoria, a.fecha_evento, a.entidad, a.accion,
                   a.identificador, COALESCE(us.usuario, 'Sistema') usuario,
                   CAST(a.datos_antes AS CHAR) datos_antes,
                   CAST(a.datos_despues AS CHAR) datos_despues,
                   COALESCE(a.motivo, '') motivo
            """ + condiciones + " ORDER BY " + orden + " " + sentido
                + ", a.codigo_auditoria DESC LIMIT ? OFFSET ?";
        Object[] pagina = java.util.Arrays.copyOf(parametros, parametros.length + 2);
        pagina[parametros.length] = limite;
        pagina[parametros.length + 1] = inicio;
        List<Map<String, Object>> filas = jdbc.query(sql, (rs, n) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            String entidadFila = rs.getString("entidad");
            fila.put("codigo", rs.getLong("codigo_auditoria"));
            fila.put("fecha", rs.getTimestamp("fecha_evento"));
            fila.put("entidad", entidadFila);
            fila.put("accion", rs.getString("accion"));
            fila.put("identificador", rs.getString("identificador"));
            fila.put("usuario", rs.getString("usuario"));
            fila.put("antes", describirRelaciones(entidadFila,
                    rs.getString("datos_antes"), sucursal));
            fila.put("despues", describirRelaciones(entidadFila,
                    rs.getString("datos_despues"), sucursal));
            fila.put("motivo", rs.getString("motivo"));
            return fila;
        }, pagina);
        return new DataTableResponseV2<>(draw, total == null ? 0 : total,
                filtrado == null ? 0 : filtrado, filas);
    }

    String describirRelaciones(String entidad, String datosJson, Integer sucursal) {
        if (datosJson == null || datosJson.isBlank()) return datosJson;
        try {
            JsonNode raiz = JSON.readTree(datosJson);
            if (!(raiz instanceof ObjectNode datos)) return datosJson;
            switch (entidad) {
                case "USUARIO" -> describirUsuario(datos, sucursal);
                case "CUENTA" -> describirCuenta(datos, sucursal);
                case "CATEGORIA" -> reemplazar(datos, "sucursal", """
                        SELECT sucursal FROM sucursales
                         WHERE codigo_sucursal = ? LIMIT 1
                        """);
                default -> { return datosJson; }
            }
            return JSON.writeValueAsString(datos);
        } catch (JsonProcessingException | RuntimeException excepcion) {
            return datosJson;
        }
    }

    private void describirUsuario(ObjectNode datos, Integer sucursal) {
        reemplazar(datos, "tipoDocumento", """
                SELECT tipo_documento FROM tipos_documento
                 WHERE codigo_tipo_documento = ? LIMIT 1
                """);
        reemplazar(datos, "estado", """
                SELECT estado FROM estados WHERE codigo_estado = ? LIMIT 1
                """);
        reemplazar(datos, "sucursal", """
                SELECT sucursal FROM sucursales WHERE codigo_sucursal = ? LIMIT 1
                """);
    }

    private void describirCuenta(ObjectNode datos, Integer sucursal) {
        reemplazar(datos, "categoria", """
                SELECT CONCAT(categoria, ' — Gs. ',
                              REPLACE(FORMAT(COALESCE(tarifa, 0), 0), ',', '.'))
                  FROM categorias
                 WHERE codigo_categoria = ? AND codigo_sucursal = ? LIMIT 1
                """, sucursal);
        reemplazar(datos, "estado", """
                SELECT estado FROM estados WHERE codigo_estado = ? LIMIT 1
                """);
        reemplazar(datos, "usuario", """
                SELECT TRIM(CONCAT_WS(' ', NULLIF(nombre, ''), NULLIF(apellido, '')))
                  FROM usuarios
                 WHERE codigo_usuario = ? AND codigo_sucursal = ? LIMIT 1
                """, sucursal);
        reemplazar(datos, "sucursal", """
                SELECT sucursal FROM sucursales WHERE codigo_sucursal = ? LIMIT 1
                """);
        reemplazar(datos, "manzana", """
                SELECT CONCAT('Manzana ', m.codigo_manzana, ' - Zona ',
                              COALESCE(NULLIF(z.zona, ''), z.codigo_zona))
                  FROM manzanas m
                  LEFT JOIN zonas z ON z.codigo_zona = m.codigo_zona
                                   AND z.codigo_sucursal = m.codigo_sucursal
                 WHERE m.codigo_manzana = ? AND m.codigo_sucursal = ? LIMIT 1
                """, sucursal);
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

    private Integer codigo(JsonNode valor) {
        if (valor == null || valor.isNull()) return null;
        if (valor.canConvertToInt()) return valor.intValue();
        if (valor.isTextual() && valor.textValue().matches("\\d+")) {
            return Integer.valueOf(valor.textValue());
        }
        return null;
    }

    private String normalizar(String valor, List<String> permitidos) {
        String normalizado = valor == null ? "" : valor.trim().toUpperCase();
        return permitidos.contains(normalizado) ? normalizado : "";
    }

    private UsuarioSistema usuario() {
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuario == null || usuario.getSucursal() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return usuario;
    }
}
