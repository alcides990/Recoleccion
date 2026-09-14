package ama.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ComprobanteFechaPersistenciaTest {

    @Test
    void conservaLaFechaDePagoSeleccionadaAlPersistir() {
        LocalDate seleccionada = LocalDate.of(2026, 8, 15);
        Comprobante comprobante = Comprobante.builder()
                .fechaPago(seleccionada)
                .build();

        comprobante.getEmision();

        assertEquals(seleccionada, comprobante.getFechaPago());
        assertNotNull(comprobante.getFechaEmision());
    }

    @Test
    void completaLaFechaActualSolamenteCuandoNoFueInformada() {
        LocalDate antes = LocalDate.now();
        Comprobante comprobante = new Comprobante();

        comprobante.getEmision();

        LocalDate despues = LocalDate.now();
        assertNotNull(comprobante.getFechaPago());
        assertFalse(comprobante.getFechaPago().isBefore(antes));
        assertFalse(comprobante.getFechaPago().isAfter(despues));
    }
}
