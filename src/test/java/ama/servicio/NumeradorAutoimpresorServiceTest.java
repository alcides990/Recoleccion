package ama.servicio;

import ama.dominio.DetalleTimbrado;
import ama.dominio.DetalleTimbradoPK;
import ama.dominio.Serie;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.jdbc.core.JdbcTemplate;

class NumeradorAutoimpresorServiceTest {

    @Test
    void reservaElSiguienteNumeroDelRango() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        prepararConsultas(jdbc, 50);
        NumeradorAutoimpresorService servicio = new NumeradorAutoimpresorService(jdbc);

        assertEquals(51, servicio.reservar(detalle(1, 100), 1));
    }

    @Test
    void rechazaCuandoElRangoEstaAgotado() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        prepararConsultas(jdbc, 10);
        NumeradorAutoimpresorService servicio = new NumeradorAutoimpresorService(jdbc);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.reservar(detalle(1, 10), 1));
        assertEquals("Se agotó el rango autorizado del autoimpresor (1 a 10).", error.getMessage());
    }

    @Test
    void reservaSinSerieFiscalUsandoLaClaveTecnicaCero() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        prepararConsultas(jdbc, 20);
        NumeradorAutoimpresorService servicio = new NumeradorAutoimpresorService(jdbc);
        DetalleTimbrado detalle = detalle(1, 100);
        detalle.setSerie(null);

        assertEquals(21, servicio.reservar(detalle, 1));
    }

    private DetalleTimbrado detalle(int desde, int hasta) {
        DetalleTimbrado detalle = new DetalleTimbrado();
        detalle.setDetalleTimbradoPK(new DetalleTimbradoPK(2, 1, 1));
        detalle.setSerie(new Serie(2));
        detalle.setModoEmision("AUTOIMPRESOR");
        detalle.setNumeroDesde(desde);
        detalle.setNumeroHasta(hasta);
        return detalle;
    }

    private void prepararConsultas(JdbcTemplate jdbc, int ultimo) {
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                any(), any(), any(), any())).thenReturn(ultimo);
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                any(), any(), any(), any(), any())).thenReturn(ultimo);
    }
}
