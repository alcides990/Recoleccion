package ama.reportes;

import java.io.InputStream;
import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SaldosZonaManzanaJasperTest {

    @Test
    void compilaLosReportesDeSaldoPorZonaYManzana() throws Exception {
        compilar("reportes/detalleZona.jrxml");
        compilar("reportes/detalleManzana.jrxml");
    }

    private void compilar(String recurso) throws Exception {
        try (InputStream jrxml = getClass().getClassLoader().getResourceAsStream(recurso)) {
            assertNotNull(jrxml, "No se encontró " + recurso);
            assertNotNull(JasperCompileManager.compileReport(jrxml));
        }
    }
}
