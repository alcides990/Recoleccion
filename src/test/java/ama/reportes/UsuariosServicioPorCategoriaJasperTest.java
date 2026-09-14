package ama.reportes;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "recoleccion.pruebas.reportes", matches = "true")
class UsuariosServicioPorCategoriaJasperTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void compilaYGeneraLasTresAgrupaciones() throws Exception {
        JasperReport reporte;
        try (var jasper = new ClassPathResource(
                "reportes/usuariosServicioPorCategoria.jasper").getInputStream()) {
            reporte = (JasperReport) JRLoader.loadObject(jasper);
        }
        assertNotNull(reporte);

        for (String agrupacion : new String[]{"GENERAL", "ZONA", "COBRADOR"}) {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("codigoSucursal", 1);
            parametros.put("sucursal", "004");
            parametros.put("agrupacion", agrupacion);
            parametros.put("agrupacionDescripcion", switch (agrupacion) {
                case "ZONA" -> "Por zona";
                case "COBRADOR" -> "Por cobrador";
                default -> "General";
            });

            JasperPrint impresion;
            try (Connection conexion = dataSource.getConnection()) {
                impresion = JasperFillManager.fillReport(reporte, parametros, conexion);
            }
            assertFalse(impresion.getPages().isEmpty(),
                    "La agrupación " + agrupacion + " debe producir páginas.");

            byte[] pdf = JasperExportManager.exportReportToPdf(impresion);
            assertTrue(pdf.length > 1_000);
        }
    }
}
