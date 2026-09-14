package ama.controladorMVC;

import ama.dominio.Manzana;
import ama.dominio.Servicio;
import ama.dominio.Sucursal;
import ama.dominio.Usuario;
import ama.servicio.AuditoriaEntidadService;
import ama.servicio.ManzanaService;
import ama.servicio.ServicioService;
import ama.servicio.UsuarioService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class ServicioObservacionControllerTest {

    private ServicioController controller;
    private ServicioService servicioService;
    private ManzanaService manzanaService;
    private AuditoriaEntidadService auditoriaService;
    private UsuarioService usuarioService;

    @BeforeEach
    void configurar() {
        controller = new ServicioController();
        servicioService = mock(ServicioService.class);
        manzanaService = mock(ManzanaService.class);
        auditoriaService = mock(AuditoriaEntidadService.class);
        usuarioService = mock(UsuarioService.class);
        ReflectionTestUtils.setField(controller, "servicioServicio", servicioService);
        ReflectionTestUtils.setField(controller, "servicioManzana", manzanaService);
        ReflectionTestUtils.setField(controller, "auditoriaEntidad", auditoriaService);
        ReflectionTestUtils.setField(controller, "servicioUsuario", usuarioService);
    }

    @Test
    void conservaLaObservacionCuandoUnClienteAntiguoNoLaEnvia() {
        Servicio existente = new Servicio("31-0062-02");
        existente.setObservacion("Referencia histórica que debe conservarse");

        Servicio modificacion = new Servicio("31-0062-02");
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigoSucursal(1);
        existente.setSucursal(sucursal);
        modificacion.setSucursal(sucursal);
        modificacion.setObservacion(null);
        Usuario usuario = new Usuario(10);
        usuario.setSucursal(sucursal);
        modificacion.setUsuario(usuario);

        when(servicioService.encontrar("31-0062-02", 1)).thenReturn(existente);
        when(usuarioService.encontrar(any())).thenReturn(usuario);
        when(manzanaService.encontrar(any())).thenReturn(mock(Manzana.class));
        when(auditoriaService.cuenta(any())).thenReturn(Map.of());

        var respuesta = controller.guardar(modificacion, "editar");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Referencia histórica que debe conservarse", modificacion.getObservacion());
        verify(servicioService).guardar(modificacion);
    }

    @Test
    void rechazaServicioNuevoSinUsuarioSeleccionadoConMensajeEspecifico() {
        Servicio nuevo = new Servicio("31-0062-02");
        nuevo.setSucursal(new Sucursal(1));
        when(servicioService.encontrar("31-0062-02", 1)).thenReturn(null);
        when(auditoriaService.cuenta(null)).thenReturn(Map.of());

        var respuesta = controller.guardar(nuevo, "agregar");

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("Seleccione un usuario registrado antes de guardar el servicio.",
                respuesta.getBody());
    }
}
