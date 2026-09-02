package ama.controladorMVC;

import ama.dominio.Ciudad;
import ama.dominio.Comision;
import ama.dominio.Empresa;
import ama.dominio.Parametro;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.servicio.ComisionService;
import ama.servicio.EmpresaServise;
import ama.servicio.ParametroService;
import ama.servicio.SucursalService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParametroVistaTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ParametroService parametros;
    @MockBean private SucursalService sucursales;
    @MockBean private EmpresaServise empresas;
    @MockBean private ComisionService comisiones;

    private Sucursal sucursal;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        empresa = new Empresa(1);
        empresa.setRuc("80000000-0");
        empresa.setRazonSocial("Empresa de prueba");
        Ciudad ciudad = new Ciudad();
        ciudad.setCodigoCiudad(1);
        ciudad.setNombreCiudad("Katuete");
        sucursal = new Sucursal(3);
        sucursal.setNombreSucursal("001");
        sucursal.setCiudad(ciudad);
        sucursal.setEmpresa(empresa);
        Comision comision = new Comision(1);
        comision.setNombreComision("Comisión normal");
        when(sucursales.encontrar(any())).thenReturn(sucursal);
        when(sucursales.listar()).thenReturn(List.of(sucursal));
        when(empresas.encontrar(any())).thenReturn(empresa);
        when(comisiones.listar()).thenReturn(List.of(comision));
    }

    @Test
    void muestraFormularioParaCrearCuandoNoEstaConfigurado() throws Exception {
        when(parametros.buscar(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/parametro/listar").param("codigoSucursal", "3")
                        .session(sesionRoot()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SIN CONFIGURAR")))
                .andExpect(content().string(containsString("Crear parámetro")))
                .andExpect(content().string(containsString("Recargo Mora")));
    }

    @Test
    void muestraFormularioParaModificarCuandoEstaConfigurado() throws Exception {
        Parametro parametro = Parametro.builder().codigoParametro(1)
                .cierrePeriodo(LocalDate.now()).recargoMora(3)
                .comision(comisiones.listar().get(0)).sucursal(sucursal).empresa(empresa).build();
        when(parametros.buscar(any())).thenReturn(Optional.of(parametro));

        mockMvc.perform(get("/parametro/listar").param("codigoSucursal", "3")
                        .session(sesionRoot()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CONFIGURADO")))
                .andExpect(content().string(containsString("Guardar cambios")))
                .andExpect(content().string(containsString("Eliminar parámetro")));
    }

    private MockHttpSession sesionRoot() {
        UsuarioSistema usuarioSistema = new UsuarioSistema(1);
        usuarioSistema.setSucursal(sucursal);
        List<SimpleGrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority("ROOT"));
        User principal = new User("root-prueba", "N/A", autoridades);
        UsernamePasswordAuthenticationToken autenticacion =
                new UsernamePasswordAuthenticationToken(principal, "N/A", autoridades);
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("usuarioSistema", usuarioSistema);
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(autenticacion));
        return sesion;
    }
}
