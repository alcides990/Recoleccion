package ama.reportes;

import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ComisionesPorCobradorJasperTest {

    @Test
    void compilaReporteFiltradoPorComision() throws Exception {
        try (var jrxml = new ClassPathResource("reportes/comisionesPorCobrador.jrxml").getInputStream()) {
            assertNotNull(JasperCompileManager.compileReport(jrxml));
        }
    }
}
