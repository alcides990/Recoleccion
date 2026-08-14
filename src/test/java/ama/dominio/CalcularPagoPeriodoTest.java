package ama.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CalcularPagoPeriodoTest {

    @Test
    void muestraUnSoloPeriodo() {
        CalcularPago pago = pagoDesdeAgosto(1);
        assertEquals("08-2026", pago.getPeriodoPago());
        assertEquals(LocalDate.of(2026, 9, 1), pago.getPagoHasta());
    }

    @Test
    void muestraRangoInclusivoParaTresPeriodos() {
        CalcularPago pago = pagoDesdeAgosto(3);
        assertEquals("08-2026 / 10-2026", pago.getPeriodoPago());
        assertEquals(LocalDate.of(2026, 11, 1), pago.getPagoHasta());
    }

    private CalcularPago pagoDesdeAgosto(int cantidad) {
        ComprobanteGuardar datos = new ComprobanteGuardar();
        datos.setPagoHasta(LocalDate.of(2026, 8, 1));
        datos.setCantidadPago(cantidad);
        return new CalcularPago(datos);
    }
}
