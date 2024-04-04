package ama.utilerias;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class ReportGenerator {

    @Autowired
    private DataSource dataSource;

    public ReportGenerator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ResponseEntity<?> getReporte(Reporte reporte, HttpServletResponse response
    ) {
        Connection conexion = null;
        try {
             HttpHeaders header = new HttpHeaders();
            conexion = dataSource.getConnection();
            InputStream jasperStream = new ClassPathResource(reporte.getRuta()).getInputStream();
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reporte.getParametros(), conexion);
            if (jasperPrint.getPages().isEmpty()) {
                response.setContentType("application/json");
                return ResponseEntity.internalServerError().body(" No hay pagina para mostrar!!");
            } else {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "inline; filename=" + reporte.getNombre());
                final OutputStream outputStream = response.getOutputStream();
                JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
            }
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        } catch (SQLException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }  
           finally {
            try {
                conexion.close();
            } catch (SQLException ex) {
                Logger.getLogger(ReportGenerator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ResponseEntity.notFound().build();
    }

}
