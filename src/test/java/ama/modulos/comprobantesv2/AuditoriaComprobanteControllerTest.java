package ama.modulos.comprobantesv2;

import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AuditoriaComprobanteControllerTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void consultaPaginadaYBusquedaGlobalEjecutanSqlValido() {
        HttpSession session = mock(HttpSession.class);
        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setSucursal(new Sucursal(1));
        when(session.getAttribute("usuarioSistema")).thenReturn(usuario);
        AuditoriaComprobanteController controlador =
                new AuditoriaComprobanteController(jdbc, session);

        var respuesta = controlador.tabla(7, 0, 10, "prueba", 0, "desc", null, "",
                null, null);

        assertTrue(respuesta.getStatusCode().is2xxSuccessful());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().getRecordsFiltered()
                <= respuesta.getBody().getRecordsTotal());

        var respuestaFiscal = controlador.tabla(8, 0, 10, null, 0, "desc", null, "",
                "4", "1");
        assertTrue(respuestaFiscal.getStatusCode().is2xxSuccessful());
        assertNotNull(respuestaFiscal.getBody());
    }

    @Test
    void detalleReemplazaClavesForaneasPorDescripciones() throws Exception {
        var referencia = jdbc.queryForMap("""
                SELECT c.codigo_usuario, TRIM(CONCAT_WS(' ', u.nombre, u.apellido)) usuario,
                       c.codigo_categoria, ca.categoria,
                       c.codigo_cobrador, TRIM(CONCAT_WS(' ', co.nombre, co.apellido)) cobrador,
                       c.codigo_condicion_venta, cv.condicion_venta,
                       c.codigo_estado, e.estado,
                       dp.codigo_metodo_pago, mp.metodo_pago
                  FROM comprobantes c
                  JOIN usuarios u ON u.codigo_usuario = c.codigo_usuario
                  JOIN categorias ca ON ca.codigo_categoria = c.codigo_categoria
                                     AND ca.codigo_sucursal = c.codigo_sucursal
                  JOIN cobradores co ON co.codigo_cobrador = c.codigo_cobrador
                                    AND co.codigo_sucursal = c.codigo_sucursal
                  JOIN condiciones_venta cv
                    ON cv.codigo_condicion_venta = c.codigo_condicion_venta
                  JOIN estados e ON e.codigo_estado = c.codigo_estado
                  JOIN detalle_pago dp
                    ON dp.numero_comprobante = c.numero_comprobante
                   AND dp.codigo_punto_expedicion = c.codigo_punto_expedicion
                   AND dp.codigo_sucursal = c.codigo_sucursal
                   AND dp.codigo_tipo_comprobante = c.codigo_tipo_comprobante
                   AND dp.codigo_serie = c.codigo_serie
                  JOIN metodos_pago mp ON mp.codigo_metodo_pago = dp.codigo_metodo_pago
                 WHERE c.codigo_sucursal = 1
                 LIMIT 1
                """);
        ObjectMapper json = new ObjectMapper();
        ObjectNode datos = json.createObjectNode();
        datos.put("usuario", ((Number) referencia.get("codigo_usuario")).intValue());
        datos.put("categoria", ((Number) referencia.get("codigo_categoria")).intValue());
        datos.put("cobrador", ((Number) referencia.get("codigo_cobrador")).intValue());
        datos.put("condicionVenta", ((Number) referencia.get("codigo_condicion_venta")).intValue());
        datos.put("estado", ((Number) referencia.get("codigo_estado")).intValue());
        ArrayNode pagos = datos.putArray("pagos");
        pagos.addObject()
                .put("codigoMetodoPago", ((Number) referencia.get("codigo_metodo_pago")).intValue())
                .put("importe", 33000);

        AuditoriaComprobanteController controlador =
                new AuditoriaComprobanteController(jdbc, mock(HttpSession.class));
        ObjectNode resultado = (ObjectNode) json.readTree(
                controlador.describirRelaciones(json.writeValueAsString(datos), 1));

        assertEquals(referencia.get("usuario"), resultado.get("usuario").asText());
        assertEquals(referencia.get("categoria"), resultado.get("categoria").asText());
        assertEquals(referencia.get("cobrador"), resultado.get("cobrador").asText());
        assertEquals(referencia.get("condicion_venta"), resultado.get("condicionVenta").asText());
        assertEquals(referencia.get("estado"), resultado.get("estado").asText());
        assertEquals(referencia.get("metodo_pago"),
                resultado.get("pagos").get(0).get("medioPago").asText());
        assertFalse(resultado.get("pagos").get(0).has("codigoMetodoPago"));
    }
}
