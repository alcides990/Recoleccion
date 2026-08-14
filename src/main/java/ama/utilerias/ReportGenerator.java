package ama.utilerias;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Log4j2
public class ReportGenerator {

    public ReportGenerator() {
    }

    public ResponseEntity<?> getReporte(Reporte reporte
    ) {
        Map<String, String> mensaje = new HashMap<>();
        try {
            HttpHeaders header = new HttpHeaders();
            InputStream jasperStream = new ClassPathResource(reporte.getRuta()).getInputStream();
            JasperReport jasperReport = reporte.getRuta().endsWith(".jrxml")
                    ? JasperCompileManager.compileReport(jasperStream)
                    : (JasperReport) JRLoader.loadObject(jasperStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reporte.getParametros(), reporte.getConexion());
            if (jasperPrint.getPages().isEmpty()) {
                mensaje.put("mensaje", "Reporte no tiene pagina para mostrar!!");
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mensaje);
            } else {
                byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
                header.setContentDisposition(ContentDisposition.inline().filename(reporte.getNombre() + ".pdf").build());
                return ResponseEntity
                        .status(HttpStatus.OK)
                        .contentLength(pdfBytes.length)
                        .contentType(MediaType.APPLICATION_PDF)
                        .headers(header)
                        .body(pdfBytes);

            }
        } catch (IOException | JRException  ex) {
            log.error("Error al generar el reporte {}", reporte.getNombre(), ex);
             mensaje.put("mensaje", ex.getMessage());
            return ResponseEntity.internalServerError().body(mensaje);
        } finally {
            try {
                if( reporte.getConexion()!=null){
                reporte.getConexion().close();
                }
            } catch (SQLException ex) {
                 mensaje.put("mensaje", ex.getMessage());
                return ResponseEntity.internalServerError().body(mensaje);
            }
        }

    }

}
