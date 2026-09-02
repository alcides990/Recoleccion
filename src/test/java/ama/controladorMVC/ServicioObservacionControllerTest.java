package ama.controladorMVC;

import ama.dominio.Manzana;
import ama.dominio.Servicio;
import ama.dominio.Sucursal;
import ama.servicio.AuditoriaEntidadService;
import ama.servicio.ManzanaService;
import ama.servicio.ServicioService;
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

    @BeforeEach
    void configurar() {
        controller = new ServicioController();
        servicioService = mock(ServicioService.class);
        manzanaService = mock(ManzanaService.class);
        auditoriaService = mock(AuditoriaEntidadService.class);
        ReflectionTestUtils.setField(controller, "servicioServicio", servicioService);
        ReflectionTestUtils.setField(controller, "servicioManzana", manzanaService);
        ReflectionTestUtils.setField(controller, "auditoriaEntidad", auditoriaService);
    }

    @Test
    void conservaLaObservacionCuandoUnClienteAntiguoNoLaEnvia() {
        Servicio existente = new Servicio("31-0062-02");
        existente.setObservacion("Referencia histórica que debe conservarse");

        Servicio modificacion = new Servicio("31-0062-02");
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigoSucursal(1);
        modificacion.setSucursal(sucursal);
        modificacion.setObservacion(null);

        when(servicioService.encontrar("31-0062-02")).thenReturn(existente);
        when(manzanaService.encontrar(any())).thenReturn(mock(Manzana.class));
        when(auditoriaService.cuenta(any())).thenReturn(Map.of());

        var respuesta = controller.guardar(modificacion, "editar");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Referencia histórica que debe conservarse", modificacion.getObservacion());
        verify(servicioService).guardar(modificacion);
    }
}
