package ama.controladorMVC;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ama.servicio.AuditoriaEntidadService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
class AuditoriaEntidadControllerTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuditoriaEntidadService auditoria;

    @Test
    void usuarioSistemaIncluyeRolesSinExponerClave() {
        Integer codigo = jdbc.queryForObject("""
                SELECT codigo_usuario_sistema FROM usuarios_sistema
                 WHERE codigo_usuario_sistema > 0 ORDER BY codigo_usuario_sistema LIMIT 1
                """, Integer.class);

        Map<String, Object> foto = auditoria.usuarioSistema(codigo);

        assertNotNull(foto);
        assertEquals(codigo, foto.get("codigo"));
        assertTrue(foto.containsKey("roles"));
        assertFalse(foto.containsKey("clave"));
    }

    @Test
    void cuentaMuestraDescripcionesEnLugarDeClavesForaneas() throws Exception {
        Map<String, Object> referencia = jdbc.queryForMap("""
                SELECT s.codigo_categoria,
                       CONCAT(ca.categoria, ' — Gs. ',
                              REPLACE(FORMAT(COALESCE(ca.tarifa, 0), 0), ',', '.')) categoria,
                       s.codigo_estado, e.estado,
                       s.codigo_usuario,
                       TRIM(CONCAT_WS(' ', u.nombre, u.apellido)) usuario,
                       s.codigo_sucursal, su.sucursal,
                       s.codigo_manzana,
                       CONCAT('Manzana ', m.codigo_manzana, ' - Zona ',
                              COALESCE(NULLIF(z.zona, ''), z.codigo_zona)) manzana
                  FROM servicios s
                  JOIN categorias ca ON ca.codigo_categoria = s.codigo_categoria
                                     AND ca.codigo_sucursal = s.codigo_sucursal
                  JOIN estados e ON e.codigo_estado = s.codigo_estado
                  JOIN usuarios u ON u.codigo_usuario = s.codigo_usuario
                                 AND u.codigo_sucursal = s.codigo_sucursal
                  JOIN sucursales su ON su.codigo_sucursal = s.codigo_sucursal
                  JOIN manzanas m ON m.codigo_manzana = s.codigo_manzana
                                 AND m.codigo_sucursal = s.codigo_sucursal
                  JOIN zonas z ON z.codigo_zona = m.codigo_zona
                              AND z.codigo_sucursal = m.codigo_sucursal
                 WHERE s.codigo_sucursal = 1
                 LIMIT 1
                """);
        ObjectMapper json = new ObjectMapper();
        ObjectNode datos = json.createObjectNode();
        datos.put("categoria", numero(referencia, "codigo_categoria"));
        datos.put("estado", numero(referencia, "codigo_estado"));
        datos.put("usuario", numero(referencia, "codigo_usuario"));
        datos.put("sucursal", numero(referencia, "codigo_sucursal"));
        datos.put("manzana", numero(referencia, "codigo_manzana"));

        AuditoriaEntidadController controlador = new AuditoriaEntidadController(
                jdbc, mock(HttpSession.class));
        ObjectNode resultado = (ObjectNode) json.readTree(controlador.describirRelaciones(
                "CUENTA", json.writeValueAsString(datos), 1));

        assertEquals(referencia.get("categoria"), resultado.get("categoria").asText());
        assertEquals(referencia.get("estado"), resultado.get("estado").asText());
        assertEquals(referencia.get("usuario"), resultado.get("usuario").asText());
        assertEquals(referencia.get("sucursal"), resultado.get("sucursal").asText());
        assertEquals(referencia.get("manzana"), resultado.get("manzana").asText());
        assertFalse(resultado.get("categoria").asText().matches("\\d+"));
    }

    @Test
    void usuarioMuestraTipoDocumentoEstadoYSucursal() throws Exception {
        Map<String, Object> referencia = jdbc.queryForMap("""
                SELECT u.codigo_tipo_documento, td.tipo_documento,
                       u.codigo_estado, e.estado,
                       u.codigo_sucursal, s.sucursal
                  FROM usuarios u
                  JOIN tipos_documento td
                    ON td.codigo_tipo_documento = u.codigo_tipo_documento
                  JOIN estados e ON e.codigo_estado = u.codigo_estado
                  JOIN sucursales s ON s.codigo_sucursal = u.codigo_sucursal
                 WHERE u.codigo_sucursal = 1
                 LIMIT 1
                """);
        ObjectMapper json = new ObjectMapper();
        ObjectNode datos = json.createObjectNode();
        datos.put("tipoDocumento", numero(referencia, "codigo_tipo_documento"));
        datos.put("estado", numero(referencia, "codigo_estado"));
        datos.put("sucursal", numero(referencia, "codigo_sucursal"));

        AuditoriaEntidadController controlador = new AuditoriaEntidadController(
                jdbc, mock(HttpSession.class));
        ObjectNode resultado = (ObjectNode) json.readTree(controlador.describirRelaciones(
                "USUARIO", json.writeValueAsString(datos), 1));

        assertEquals(referencia.get("tipo_documento"),
                resultado.get("tipoDocumento").asText());
        assertEquals(referencia.get("estado"), resultado.get("estado").asText());
        assertEquals(referencia.get("sucursal"), resultado.get("sucursal").asText());
    }

    private int numero(Map<String, Object> datos, String campo) {
        return ((Number) datos.get(campo)).intValue();
    }
}
