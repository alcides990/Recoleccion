package ama.modulos.facturacionmovil;

import ama.dao.ServicioDao;
import ama.dao.UsuarioSistemaDao;
import ama.dominio.UsuarioSistema;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
@Transactional
class BusquedaServicioMovilTest {

    @Autowired
    private ServicioDao servicios;

    @Autowired
    private UsuarioSistemaDao usuariosSistema;

    @Autowired
    private FacturacionMovilController controller;

    @Test
    void buscaPorManzanaYNombre() {
        assertFalse(servicios.buscarActivosPorManzana(1, 520, PageRequest.of(0, 100)).isEmpty());
        assertFalse(servicios.buscarActivosPorNombre(1, "Paola", PageRequest.of(0, 100)).isEmpty());
    }

    @Test
    void endpointConstruyeEstadosDeCuentaParaAmbosFiltros() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("prueba", "", List.of(new SimpleGrantedAuthority("ROOT"))));
        UsuarioSistema usuario = usuariosSistema.listar(PageRequest.of(0, 1)).getContent().get(0);
        HttpSession session = new MockHttpSession();
        session.setAttribute("usuarioSistema", usuario);
        ReflectionTestUtils.setField(controller, "session", session);

        var porManzana = controller.buscarServicios("manzana", "520");
        var porNombre = controller.buscarServicios("nombre", "Paola");

        assertEquals(200, porManzana.getStatusCode().value());
        assertEquals(200, porNombre.getStatusCode().value());
        assertFalse(assertInstanceOf(List.class, porManzana.getBody()).isEmpty());
        assertFalse(assertInstanceOf(List.class, porNombre.getBody()).isEmpty());
    }
}
