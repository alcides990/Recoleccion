package ama.modulos.comprobantesv2;

import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ComprobanteTicketJasperTest {

    @Test
    void compilaElTicketTermico() throws Exception {
        try (var jrxml = new ClassPathResource("reportes/comprobanteTicket.jrxml").getInputStream()) {
            assertNotNull(JasperCompileManager.compileReport(jrxml));
        }
    }
}
