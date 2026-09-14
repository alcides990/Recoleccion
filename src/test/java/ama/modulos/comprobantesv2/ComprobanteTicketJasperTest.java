package ama.modulos.comprobantesv2;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ComprobanteTicketJasperTest {

    @Test
    void compilaElTicketTermico() throws Exception {
        compilar("reportes/comprobanteTicket.jrxml");
        cargarCompilado("reportes/comprobanteTicket.jasper");
    }

    @Test
    void compilaElTicket80mm() throws Exception {
        compilar("reportes/comprobanteTicket80mm.jrxml");
        cargarCompilado("reportes/comprobanteTicket80mm.jasper");
    }

    @Test
    void compilaElComprobanteA4() throws Exception {
        compilar("reportes/comprobanteA4.jrxml");
        cargarCompilado("reportes/comprobanteA4.jasper");
    }

    private void compilar(String reporte) throws Exception {
        try (var jrxml = new ClassPathResource(reporte).getInputStream()) {
            assertNotNull(JasperCompileManager.compileReport(jrxml));
        }
    }

    private void cargarCompilado(String reporte) throws Exception {
        try (var jasper = new ClassPathResource(reporte).getInputStream()) {
            assertNotNull((JasperReport) JRLoader.loadObject(jasper));
        }
    }
}
