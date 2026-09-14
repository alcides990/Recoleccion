package ama.controladorMVC;

import ama.dao.DetalleTimbradoDao;
import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.Estado;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Timbrado;
import ama.dominio.UsuarioSistema;
import ama.servicio.AuditoriaComprobanteService;
import ama.servicio.ComprobanteService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComprobanteControllerAuditoriaTest {

    @Test
    void cadaEdicionAgregaUnEventoAlHistorial() {
        ComprobanteController controller = new ComprobanteController();
        ComprobanteService comprobantes = mock(ComprobanteService.class);
        AuditoriaComprobanteService auditoria = mock(AuditoriaComprobanteService.class);
        DetalleTimbradoDao detallesTimbrado = mock(DetalleTimbradoDao.class);
        HttpSession session = mock(HttpSession.class);

        ReflectionTestUtils.setField(controller, "comprobanteService", comprobantes);
        ReflectionTestUtils.setField(controller, "auditoriaComprobanteService", auditoria);
        ReflectionTestUtils.setField(controller, "detalleTimbradoDao", detallesTimbrado);
        ReflectionTestUtils.setField(controller, "httpSession", session);

        ComprobantePK clave = new ComprobantePK(125,
                new PuntoExpedicionPK(1, 2), 1, 1);
        Comprobante almacenado = new Comprobante();
        almacenado.setComprobantePK(clave);
        almacenado.setTimbrado(new Timbrado(10));
        almacenado.setEstado(new Estado(1));

        Comprobante solicitud = new Comprobante();
        solicitud.setComprobantePK(clave);
        solicitud.setEstado(new Estado(1));
        solicitud.setFechaPago(LocalDate.of(2026, 8, 31));
        solicitud.setObs("Primera modificación");

        UsuarioSistema usuario = new UsuarioSistema(7);
        when(session.getAttribute("usuarioSistema")).thenReturn(usuario);
        when(comprobantes.getComprobante(clave)).thenReturn(almacenado);
        when(detallesTimbrado.findById(any())).thenReturn(Optional.empty());
        when(auditoria.capturarFoto(clave)).thenReturn("{\"version\":1}", "{\"version\":2}");

        controller.moficicarComprobante(solicitud);
        solicitud.setObs("Segunda modificación");
        controller.moficicarComprobante(solicitud);

        verify(auditoria, times(2)).capturarFoto(clave);
        verify(auditoria).registrarModificacion(eq(clave), eq(7), any(), eq("{\"version\":1}"));
        verify(auditoria).registrarModificacion(eq(clave), eq(7), any(), eq("{\"version\":2}"));
        verify(comprobantes, times(2)).guardar(almacenado);
    }
}
