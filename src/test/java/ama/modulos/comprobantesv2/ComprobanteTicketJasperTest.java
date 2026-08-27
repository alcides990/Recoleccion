package ama.modulos.comprobantesv2;

import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ComprobanteTicketJasperTest {

    @Test
    void compilaElTicketTermico() throws Exception {
        compilar("reportes/comprobanteTicket.jrxml");
    }

    @Test
    void compilaElTicket80mm() throws Exception {
        compilar("reportes/comprobanteTicket80mm.jrxml");
    }

    @Test
    void compilaElComprobanteA4() throws Exception {
        compilar("reportes/comprobanteA4.jrxml");
    }

    private void compilar(String reporte) throws Exception {
        try (var jrxml = new ClassPathResource(reporte).getInputStream()) {
            assertNotNull(JasperCompileManager.compileReport(jrxml));
        }
    }
}
