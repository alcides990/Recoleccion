package ama.dominio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComprobanteGuardarFechaJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void rechazaFechaConBarras() {
        assertThrows(Exception.class, () -> mapper.readValue(
                "{\"fechaPago\":\"2026/08/31\"}", ComprobanteGuardar.class));
    }

    @Test
    void aceptaFechaIsoAnoMesDia() throws Exception {
        ComprobanteGuardar comprobante = mapper.readValue(
                "{\"fechaPago\":\"2026-08-31\"}", ComprobanteGuardar.class);

        assertEquals(LocalDate.of(2026, 8, 31), comprobante.getFechaPago());
    }
}
