package ama.controladorMVC;

import ama.dominio.Empresa;
import ama.dominio.Comision;
import ama.dominio.Parametro;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.servicio.ComisionService;
import ama.servicio.EmpresaServise;
import ama.servicio.ParametroService;
import ama.servicio.SucursalService;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Optional;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametroControllerSucursalTest {

    @Mock private HttpSession sesion;
    @Mock private ParametroService parametros;
    @Mock private SucursalService sucursales;
    @Mock private EmpresaServise empresas;
    @Mock private ComisionService comisiones;
    @InjectMocks private ParametroController controlador;

    private Sucursal sucursalSesion;

    @BeforeEach
    void preparar() {
        sucursalSesion = sucursal(1);
        UsuarioSistema usuario = new UsuarioSistema(10);
        usuario.setSucursal(sucursalSesion);
        when(sesion.getAttribute("usuarioSistema")).thenReturn(usuario);
        lenient().when(empresas.encontrar(any())).thenReturn(new Empresa(1));
        lenient().when(parametros.buscar(any())).thenReturn(Optional.of(new Parametro(1)));
        lenient().when(comisiones.listar()).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administradorNoPuedeForzarOtraSucursal() {
        autenticar("ADMINISTRADOR");

        controlador.listaCategoria(99, new ExtendedModelMap());

        verify(parametros).buscar(sucursalSesion);
        verify(sucursales, never()).encontrar(any());
    }

    @Test
    void rootPuedeSeleccionarOtraSucursal() {
        autenticar("ROOT");
        Sucursal seleccionada = sucursal(2);
        when(sucursales.encontrar(new Sucursal(2))).thenReturn(seleccionada);
        when(sucursales.listar()).thenReturn(Collections.singletonList(seleccionada));

        controlador.listaCategoria(2, new ExtendedModelMap());

        verify(parametros).buscar(seleccionada);
        verify(sucursales).encontrar(new Sucursal(2));
    }

    @Test
    void muestraAltaCuandoLaSucursalTodaviaNoTieneParametro() {
        autenticar("ADMINISTRADOR");
        when(parametros.buscar(sucursalSesion)).thenReturn(Optional.empty());
        ExtendedModelMap modelo = new ExtendedModelMap();

        controlador.listaCategoria(null, modelo);

        org.junit.jupiter.api.Assertions.assertEquals(false, modelo.get("existeParametro"));
        Parametro parametro = (Parametro) modelo.get("parametro");
        org.junit.jupiter.api.Assertions.assertEquals(sucursalSesion, parametro.getSucursal());
    }

    @Test
    void creaParametroParaLaSucursalDeLaSesion() {
        autenticar("ADMINISTRADOR");
        when(parametros.buscar(sucursalSesion)).thenReturn(Optional.empty());
        when(parametros.generarCodigo()).thenReturn(2);
        Comision comision = new Comision(1);
        when(comisiones.encontrar(any())).thenReturn(comision);
        Parametro entrada = Parametro.builder().cierrePeriodo(LocalDate.now())
                .recargoMora(3).comision(comision).build();

        controlador.guardar(new Empresa(1), entrada, 99, new RedirectAttributesModelMap());

        org.mockito.ArgumentCaptor<Parametro> captor =
                org.mockito.ArgumentCaptor.forClass(Parametro.class);
        verify(parametros).guardar(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().getCodigoParametro());
        org.junit.jupiter.api.Assertions.assertEquals(sucursalSesion, captor.getValue().getSucursal());
    }

    @Test
    void eliminaParametroDeLaSucursalDeLaSesion() {
        autenticar("SUPERVISOR");
        Parametro existente = new Parametro(1);
        when(parametros.buscar(sucursalSesion)).thenReturn(Optional.of(existente));

        controlador.eliminar(99, new RedirectAttributesModelMap());

        verify(parametros).eliminar(existente);
        verify(sucursales, never()).encontrar(any());
    }

    private void autenticar(String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("prueba", "",
                        Collections.singletonList(new SimpleGrantedAuthority(rol))));
    }

    private Sucursal sucursal(int codigo) {
        Sucursal sucursal = new Sucursal(codigo);
        sucursal.setEmpresa(new Empresa(1));
        return sucursal;
    }
}
