package ama.modulos.facturacionmovil;

import ama.dao.DetalleTimbradoDao;
import ama.dominio.ComprobantePK;
import ama.dominio.DetalleTimbrado;
import ama.dominio.DetalleTimbradoPK;
import ama.dominio.Sucursal;
import ama.dominio.TipoComprobante;
import ama.dominio.UsuarioSistema;
import ama.servicio.ComprobanteService;
import ama.servicio.NumeradorAutoimpresorService;
import ama.servicio.TipoComprobanteService;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NumeroSiguienteMovilTest {
    @Mock HttpSession session;
    @Mock ComprobanteService comprobantes;
    @Mock TipoComprobanteService tipos;
    @Mock DetalleTimbradoMovilRepository detallesTimbrado;
    @Mock DetalleTimbradoDao detalleTimbradoDao;
    @Mock NumeradorAutoimpresorService numeradorAutoimpresor;
    @InjectMocks FacturacionMovilController controller;
    private DetalleTimbrado detalle;

    @BeforeEach
    void preparar() {
        UsuarioSistema usuario = new UsuarioSistema(7);
        usuario.setSucursal(new Sucursal(3));
        when(session.getAttribute("usuarioSistema")).thenReturn(usuario);
        detalle = new DetalleTimbrado();
        detalle.setNumeroDesde(100);
        detalle.setNumeroHasta(999);
    }

    private void tipoYTimbrado(String nombre, String modo) {
        TipoComprobante tipo = new TipoComprobante(4);
        tipo.setNombreTipoComprobante(nombre);
        when(tipos.encontrar(any())).thenReturn(tipo);
        when(detallesTimbrado.buscarActivos(3, 2, modo))
                .thenReturn(Collections.singletonList(new Object[]{5, "12345678", 0}));
    }

    private void detalleDisponible() {
        when(detalleTimbradoDao.findById(new DetalleTimbradoPK(5, 2, 3))).thenReturn(Optional.of(detalle));
    }

    @Test
    void manualSugiereElMaximoMasUnoDentroDeLaSucursalDeSesion() {
        tipoYTimbrado("FACTURA MANUAL", "MANUAL");
        detalleDisponible();
        when(comprobantes.getNumeroComprobante(any())).thenReturn(451);
        var respuesta = controller.numeroSiguiente(2, 4, 0, 5);
        assertEquals(Map.of("numeroComprobante", 451), respuesta.getBody());
        var argumento = org.mockito.ArgumentCaptor.forClass(ComprobantePK.class);
        verify(comprobantes).getNumeroComprobante(argumento.capture());
        ComprobantePK clave = argumento.getValue();
        assertEquals(3, clave.getPuntoExpedicionPK().getCodigoSucursal());
        assertEquals(2, clave.getPuntoExpedicionPK().getCodigoPuntoExpedicion());
        assertEquals(4, clave.getCodigoTipoComprobante());
        assertEquals(0, clave.getCodigoSerie());
    }

    @Test
    void manualNoSugiereMenosQueElInicioDelTimbrado() {
        tipoYTimbrado("FACTURA MANUAL", "MANUAL");
        detalleDisponible();
        when(comprobantes.getNumeroComprobante(any())).thenReturn(1);
        assertEquals(Map.of("numeroComprobante", 100), controller.numeroSiguiente(2, 4, 0, 5).getBody());
    }

    @Test
    void manualNoSugiereUnNumeroFueraDelRango() {
        tipoYTimbrado("FACTURA MANUAL", "MANUAL");
        detalleDisponible();
        when(comprobantes.getNumeroComprobante(any())).thenReturn(1000);
        assertEquals(HttpStatus.CONFLICT, controller.numeroSiguiente(2, 4, 0, 5).getStatusCode());
    }

    @Test
    void autoimpresorConsultaElNumeradorSinReservar() {
        tipoYTimbrado("FACTURA AUTOIMPRESOR", "AUTOIMPRESOR");
        detalleDisponible();
        when(numeradorAutoimpresor.consultarSiguiente(detalle, 4)).thenReturn(501);
        assertEquals(Map.of("numeroComprobante", 501), controller.numeroSiguiente(2, 4, 0, 5).getBody());
        org.mockito.Mockito.verify(numeradorAutoimpresor, org.mockito.Mockito.never()).reservar(any(), any());
    }

    @Test
    void rechazaTimbradoNoDisponibleEnLaSucursal() {
        tipoYTimbrado("FACTURA MANUAL", "MANUAL");
        assertEquals(HttpStatus.BAD_REQUEST, controller.numeroSiguiente(2, 4, 0, 9).getStatusCode());
    }

    @Test
    void clienteAnteriorPuedeConsultarConUnSoloTimbradoDisponible() {
        tipoYTimbrado("FACTURA MANUAL", "MANUAL");
        detalleDisponible();
        when(comprobantes.getNumeroComprobante(any())).thenReturn(151);
        assertEquals(Map.of("numeroComprobante", 151), controller.numeroSiguiente(2, 4, 0, null).getBody());
    }
}
