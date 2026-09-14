package ama.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class UbicacionServicioServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final UbicacionServicioService servicio =
            new UbicacionServicioService(jdbcTemplate);

    @Test
    void rechazaLatitudFueraDelRangoAntesDeAccederALaBase() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardar("31-0001-01", new BigDecimal("90.0000001"),
                        new BigDecimal("-54.1234567"), null, "GPS", 1, 1));

        assertEquals("La latitud debe estar entre -90 y 90", error.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rechazaLongitudFueraDelRangoAntesDeAccederALaBase() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardar("31-0001-01", new BigDecimal("-24.1234567"),
                        new BigDecimal("180.0000001"), null, "MANUAL", 1, 1));

        assertEquals("La longitud debe estar entre -180 y 180", error.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rechazaMetodoDesconocidoAntesDeAccederALaBase() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardar("31-0001-01", new BigDecimal("-24.1234567"),
                        new BigDecimal("-54.1234567"), null, "OTRO", 1, 1));

        assertEquals("El método de ubicación no es válido", error.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rechazaOrigenDesconocidoAntesDeAccederALaBase() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardar("31-0001-01", new BigDecimal("-24.1234567"),
                        new BigDecimal("-54.1234567"), null, "GPS", 1, 1, "OTRO"));

        assertEquals("El origen de la ubicación no es válido", error.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }
}
