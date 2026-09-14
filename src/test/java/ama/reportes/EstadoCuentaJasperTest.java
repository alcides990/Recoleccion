package ama.reportes;

import java.io.InputStream;
import net.sf.jasperreports.engine.JasperCompileManager;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EstadoCuentaJasperTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "/reportes/extractoCuenta.jrxml",
        "/reportes/detalleZona.jrxml",
        "/reportes/detalleManzana.jrxml"
    })
    void compilaReporteConFuncionPagarDesde(String recurso) throws Exception {
        try (InputStream jrxml = getClass().getResourceAsStream(recurso)) {
            assertNotNull(jrxml, "No se encontró " + recurso);
            assertNotNull(JasperCompileManager.compileReport(jrxml));
        }
    }
}
