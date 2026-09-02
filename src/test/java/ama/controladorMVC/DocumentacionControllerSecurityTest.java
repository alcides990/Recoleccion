package ama.controladorMVC;

import ama.dominio.UsuarioSistema;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentacionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void manualDisponibleParaTodoUsuarioAutenticado() throws Exception {
        MockHttpSession sesion = sesionConRol("SECRETARIO");
        mockMvc.perform(get("/documentacion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Manual de usuario")))
                .andExpect(content().string(not(containsString("Documentación técnica"))));
        mockMvc.perform(get("/documentacion/manual/pdf").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void administradorNoPuedeAccederALaDocumentacionTecnica() throws Exception {
        mockMvc.perform(get("/documentacion/tecnica/pdf").session(sesionConRol("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rootPuedeVerYDescargarLaDocumentacionTecnica() throws Exception {
        MockHttpSession sesion = sesionConRol("ROOT");
        mockMvc.perform(get("/documentacion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Documentación técnica")));
        mockMvc.perform(get("/documentacion/tecnica/pdf").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    private MockHttpSession sesionConRol(String rol) {
        List<SimpleGrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority(rol));
        User principal = new User("usuario-prueba", "N/A", autoridades);
        UsernamePasswordAuthenticationToken autenticacion =
                new UsernamePasswordAuthenticationToken(principal, "N/A", autoridades);
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("usuarioSistema", new UsuarioSistema(1));
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(autenticacion));
        return sesion;
    }
}
