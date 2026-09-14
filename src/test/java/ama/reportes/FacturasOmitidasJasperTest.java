package ama.reportes;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturasOmitidasJasperTest {

    @Test
    void compilaElReporteDeFacturasOmitidas() throws Exception {
        try (InputStream jrxml = getClass().getClassLoader()
                .getResourceAsStream("reportes/facturasOmitidas.jrxml")) {
            assertNotNull(jrxml);
            assertNotNull(JasperCompileManager.compileReport(jrxml));
        }
    }

    @Test
    void cargaElJasperCompiladoYExportaPdf() throws Exception {
        try (InputStream jasper = getClass().getClassLoader()
                .getResourceAsStream("reportes/facturasOmitidas.jasper")) {
            assertNotNull(jasper);
            JasperReport reporte = (JasperReport) JRLoader.loadObject(jasper);
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("codigoSucursal", 1);
            parametros.put("codigoSerie", 0);
            parametros.put("desde", 1);
            parametros.put("hasta", 1);
            JasperPrint impresion = JasperFillManager.fillReport(
                    reporte, parametros, new JREmptyDataSource(1));
            byte[] pdf = JasperExportManager.exportReportToPdf(impresion);
            assertTrue(pdf.length > 0);
        }
    }
}
