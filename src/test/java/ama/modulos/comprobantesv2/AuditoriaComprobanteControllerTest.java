package ama.modulos.comprobantesv2;

import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
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

        var respuesta = controlador.tabla(7, 0, 10, "prueba", 0, "desc", null, "");

        assertTrue(respuesta.getStatusCode().is2xxSuccessful());
        assertNotNull(respuesta.getBody());
        assertTrue(respuesta.getBody().getRecordsFiltered()
                <= respuesta.getBody().getRecordsTotal());
    }
}
