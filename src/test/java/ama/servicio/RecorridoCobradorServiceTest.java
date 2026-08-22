package ama.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RecorridoCobradorServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RecorridoCobradorService servicio =
            new RecorridoCobradorService(jdbcTemplate);

    @Test
    void rechazaPuntoConLatitudFueraDeRango() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarPunto(1L,
                        "9b66cf8d-af95-4a9a-9ccd-c40d08d95c72",
                        new BigDecimal("91"),
                        new BigDecimal("-54.1"), null, null,
                        LocalDateTime.now(), 1, "APP"));

        assertEquals("La latitud debe estar entre -90 y 90", error.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rechazaVelocidadNegativa() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarPunto(1L,
                        "9b66cf8d-af95-4a9a-9ccd-c40d08d95c72",
                        new BigDecimal("-24.1"),
                        new BigDecimal("-54.1"), new BigDecimal("5"),
                        new BigDecimal("-1"), LocalDateTime.now(), 1, "APP"));

        assertEquals("La velocidad no puede ser negativa", error.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rechazaIdentificadorDeSincronizacionInvalido() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarPunto(1L, "punto-1",
                        new BigDecimal("-24.1"), new BigDecimal("-54.1"),
                        null, null, LocalDateTime.now(), 1, "APP"));

        assertEquals("El identificador de sincronización no es válido",
                error.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void rootPuedeDesactivarUnDispositivo() {
        String id = "telefono-cobrador-01";
        when(jdbcTemplate.update(anyString(), eq(false), eq(id), eq(1)))
                .thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(id), eq(1)))
                .thenReturn(List.of(Map.of(
                        "idDispositivo", id,
                        "nombreDispositivo", "Teléfono 1",
                        "activo", false)));

        Map<String, Object> resultado = servicio.cambiarEstadoDispositivo(
                id, false, 1);

        assertEquals(false, resultado.get("activo"));
        verify(jdbcTemplate).update(anyString(), eq(false), eq(id), eq(1));
    }

    @Test
    void dispositivoDesactivadoNoPuedeEnviarPuntos() {
        String id = "telefono-cobrador-01";
        when(jdbcTemplate.queryForList(anyString(), eq(id), eq(1)))
                .thenReturn(List.of(Map.of(
                        "idDispositivo", id,
                        "nombreDispositivo", "Teléfono 1",
                        "activo", false)));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.registrarPuntoDispositivo(id, 1L,
                        "9b66cf8d-af95-4a9a-9ccd-c40d08d95c72",
                        new BigDecimal("-24.1"), new BigDecimal("-54.1"),
                        null, null, LocalDateTime.now(), 1));

        assertEquals("El dispositivo está desactivado por ROOT", error.getMessage());
    }
}
