package ama.api.controladores;

import ama.servicio.ServicioCiudad;
import ama.servicio.ServicioCobrador;
import ama.servicio.ServicioZona;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/reporte")
public class controladorInforme {

    @Autowired
    private ServicioCiudad servicioCiudad;
    @Autowired
    private ServicioZona servicioZona;
    @Autowired
    private ServicioCobrador servicioCobrador;
    @Autowired
    private DataSource dataSource;

    @GetMapping
    public String getReporte(Model model) {
        var cobrador = servicioCobrador.listar();
        model.addAttribute("cobradores", cobrador);

        var zona = servicioZona.listar();
        model.addAttribute("zonas", zona);

        return "reportes/reporte";
    }

    @PostMapping("/ciudad")
    public ResponseEntity<byte[]> getCiudadReport(
            Map<String, Object> parameters) throws SQLException {
        try (
                 Connection conexion = dataSource.getConnection()) {
            InputStream jasperStream = new ClassPathResource("reportes/ciudades.jasper").getInputStream();
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conexion);

            HttpHeaders headers = new HttpHeaders();
            ContentDisposition conetenDisposicion = ContentDisposition.builder("inline").filename("ciudades.pdf").build();
            //set the PDF format
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("inline").build());
//            headers.setContentDispositionFormData("filename", "ciudades.pdf");
            //create the report in PDF format
            return new ResponseEntity<>(JasperExportManager.exportReportToPdf(jasperPrint), headers, HttpStatus.OK);

        } catch (IOException | SQLException | JRException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> getReporte(
            Map<String, Object> parameters,
            String rutaReporte,
            String nombreReporte
    ) throws JRException, IOException, SQLException {
        Connection conexion = null;
        try {
            conexion = dataSource.getConnection();
            InputStream jasperStream = new ClassPathResource(rutaReporte).getInputStream();
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conexion);
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
            if (jasperPrint.getPages().isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        //                        .header("Content-Type", "application/json")
                        .body("El reporte no tiene pagina para mostrar!!");
            } else {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                ContentDisposition conetenDisposicion = ContentDisposition.builder("inline")
                        .filename(nombreReporte + ".pdf").build();
                headers.setContentDisposition(conetenDisposicion);
                headers.setContentLength(pdfBytes.length);
                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            }
        } catch (SQLException ex) {
            return new ResponseEntity("Error al Generar Informe: " + ex.getMessage(), HttpStatus.CONFLICT);
        } finally {
            conexion.close();
        }
    }

    @PostMapping("/detalle_zona")
    public ResponseEntity<?> reporteDetalleZona(
            @RequestParam("codigoZona") Integer codigoZona,
            @RequestParam("codigoCobrador") Integer codigoCobrador,
            @RequestParam("desde") Integer desde,
            @RequestParam("hasta") Integer hasta
    ) throws JRException, IOException, SQLException {
        Map<String, Object> parametro = new HashMap<>();
        String rutaReporte = "reportes/detalleZona.jasper";
        String nombeReporte = "Detalle Zona";
        parametro.put("codigoCobrador", codigoCobrador);
        parametro.put("codigoZona", codigoZona);
        parametro.put("desde", desde);
        parametro.put("hasta", hasta);
        return getReporte(parametro, rutaReporte, nombeReporte);
    }

    @PostMapping("/detalle_manzana")
    public ResponseEntity<?> reporteDetalleManzana(
            @RequestParam("codigoManzana") Integer codigoManzana,
            @RequestParam("codigoCobrador") Integer codigoCobrador,
            @RequestParam("codigoZona") Integer codigoZona,
            @RequestParam("desde") Integer desde,
            @RequestParam("hasta") Integer hasta
    ) throws JRException, IOException, SQLException {
        Map<String, Object> parametro = new HashMap<>();
        String rutaReporte = "reportes/detalleManzana.jasper";
        String nombeReporte = "Detalle Manzana";
        parametro.put("manzana", codigoManzana);
        parametro.put("codigoCobrador", codigoZona);
        parametro.put("codigoZona", codigoZona);
        parametro.put("desde", desde);
        parametro.put("hasta", hasta);
        return getReporte(parametro, rutaReporte, nombeReporte);
    }

    @PostMapping("/ingresos")
    public ResponseEntity<?> reporteIngresosPorZona(
            @RequestParam("grupo") String grupo,
            @RequestParam("ingresos-desde") String desde,
            @RequestParam("ingresos-hasta") String hasta
    ) throws JRException, IOException, SQLException {
        Map<String, Object> parametro = new HashMap<>();
        String nombeReporte = "Ingresos por zona";
        parametro.put("desde", desde);
        parametro.put("hasta", hasta);
        String rutaReporte = "";
        if (grupo.equals("zona")) {
            rutaReporte = "reportes/ingresosPorZona.jasper";
        } else if (grupo.equals("cob")) {
            rutaReporte = "reportes/ingresosPorCobrador.jasper";
        }
        return getReporte(parametro, rutaReporte, nombeReporte);
    }

    @PostMapping("/prueba")
    public String reportePrueba(){
      
        return "/reportes/visor-pdf";
    }
    
    
    @GetMapping("/imprimir")
    @ResponseBody
    public void imorimir(Map<String, Object> parameters, HttpServletResponse response) throws JRException, IOException, SQLException {
        InputStream jasperStream = new ClassPathResource("reportes/ciudades.jasper").getInputStream();
        JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource.getConnection());
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=ciudades.pdf");
        final OutputStream outputStream = response.getOutputStream();
        JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
//        JasperPrintManager.printReport(jasperPrint, false); 
    }
}
