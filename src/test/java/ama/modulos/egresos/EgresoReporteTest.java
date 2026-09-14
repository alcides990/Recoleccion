package ama.modulos.egresos;

import com.lowagie.text.pdf.PdfReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EgresoReporteTest {
    private static final LocalDate DESDE = LocalDate.of(2026, 9, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 9, 30);

    @Test void generaPdfPaginadoDesdeDtosConNombresLargos() throws Exception {
        var filas = new ArrayList<EgresoDto>();
        for (long id = 1; id <= 40; id++) filas.add(gasto(id, false));
        byte[] pdf = new EgresoPdfRenderer().generar(filas, "Sucursal de prueba", DESDE, HASTA);
        var lector = new PdfReader(pdf);
        try {
            assertTrue(lector.getNumberOfPages() > 1);
        } finally {
            lector.close();
        }
        Files.createDirectories(Path.of("target/egresos-qa"));
        Files.write(Path.of("target/egresos-qa/reporte.pdf"), pdf);
    }

    @Test void excluyeAnuladosAntesDeGenerarPdf() throws Exception {
        var gastos = mock(EgresoService.class);
        var pdf = mock(EgresoPdfRenderer.class);
        EgresoDto vigente = gasto(1, false);
        when(gastos.listar(7, DESDE, HASTA)).thenReturn(List.of(vigente, gasto(2, true)));
        when(pdf.generar(List.of(vigente), "Sucursal", DESDE, HASTA)).thenReturn(new byte[]{1});
        assertArrayEquals(new byte[]{1}, new EgresoReporteService(gastos, pdf).generar(7, "Sucursal", DESDE, HASTA));
        verify(pdf).generar(List.of(vigente), "Sucursal", DESDE, HASTA);
    }

    @Test void generaFormatosDetalladoYAgrupadoPorProducto() throws Exception {
        var gasto = gasto(1, false).conDetalles(List.of(
                new LineaEgreso("Aceite", new BigDecimal("2"), new BigDecimal("10")),
                new LineaEgreso("Servicio", BigDecimal.ONE, new BigDecimal("50"))));
        var renderer = new EgresoPdfRenderer();
        assertTrue(new PdfReader(renderer.generarDetallado(List.of(gasto), "Sucursal", DESDE, HASTA)).getNumberOfPages() > 0);
        assertTrue(new PdfReader(renderer.generarPorProducto(List.of(gasto), "Sucursal", DESDE, HASTA)).getNumberOfPages() > 0);
    }

    @Test void noGeneraPdfCuandoSoloHayAnulados() {
        var gastos = mock(EgresoService.class);
        var pdf = mock(EgresoPdfRenderer.class);
        when(gastos.listar(7, DESDE, HASTA)).thenReturn(List.of(gasto(1, true)));
        var error = assertThrows(EgresoException.class,
                () -> new EgresoReporteService(gastos, pdf).generar(7, "Sucursal", DESDE, HASTA));
        assertEquals(EgresoException.Motivo.SIN_DATOS, error.getMotivo());
        verifyNoInteractions(pdf);
    }

    private EgresoDto gasto(long id, boolean anulado) {
        return new EgresoDto(id, DESDE, 1, "Mantenimiento de vehículos y servicios generales",
                "Proveedor de prueba con razón social extensa para verificar ajuste del texto",
                "001-001-0000123", "", new BigDecimal("1234567.89"), anulado, List.of());
    }
}
