package ama.api.controladores;

import ama.DTO.ZonaDTO;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
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
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ama.servicio.CobradorService;
import ama.servicio.ZonaService;
import ama.servicio.CiudadService;
import ama.utilerias.ReportGenerator;
import ama.utilerias.Reporte;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/reporte")
public class ReportController {

    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private ZonaService servicioZona;
    @Autowired
    private CobradorService servicioCobrador;
    @Autowired
    private HttpSession httpSession;
    @Autowired
    private DataSource dataSource;

    @GetMapping
    public String getReporte(Model model) {
        var cobrador = servicioCobrador.listarIsEstadoActivo(getSucursalSession());
        model.addAttribute("cobradores", cobrador);

     List<ZonaDTO> zona = servicioZona.listar()
        .stream()
        .map(z -> ZonaDTO.builder()
                .codigoZona(z.getCodigoZona())
                .nombreZona(z.getNombreZona())
                .cobrador(z.getCobrador())
                .build())
        .toList();

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
            ContentDisposition conetenDisposicion = ContentDisposition.builder("inline").filename("ciudades.pdf")
                    .build();
            // set the PDF format
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("inline").build());
            // headers.setContentDispositionFormData("filename", "ciudades.pdf");
            // create the report in PDF format
            return new ResponseEntity<>(JasperExportManager.exportReportToPdf(jasperPrint), headers, HttpStatus.OK);

        } catch (IOException | SQLException | JRException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> getReporte(Reporte reporte) throws JRException, IOException, SQLException {
        Connection conexion = null;
        HttpHeaders header = new HttpHeaders();
        try {
            conexion = dataSource.getConnection();
            InputStream jasperStream = new ClassPathResource(reporte.getRuta()).getInputStream();
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reporte.getParametros(), conexion);
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
            if (jasperPrint.getPages().isEmpty()) {
                Map<String, String> mensaje = new HashMap<>();
                mensaje.put("mensaje", "Reporte no tiene pagina para mostrar!!");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mensaje);
            } else {
                header.setContentType(MediaType.APPLICATION_PDF);
                ContentDisposition conetenDisposicion = ContentDisposition.builder("inline")
                        .filename(reporte.getNombre() + ".pdf").build();
                header.setContentDisposition(conetenDisposicion);
                header.setContentLength(pdfBytes.length);
                return new ResponseEntity<>(pdfBytes, header, HttpStatus.OK);
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
            @RequestParam("hasta") Integer hasta) throws JRException, IOException, SQLException {
        Map<String, Object> parametro = new HashMap<>();
        parametro.put("codigoCobrador", codigoCobrador);
        parametro.put("codigoZona", codigoZona);
        parametro.put("desde", desde);
        parametro.put("hasta", hasta);
        Reporte reporte = Reporte.builder()
                .conexion(dataSource.getConnection())
                .nombre("Detalle Zona")
                .ruta("reportes/detalleZona.jasper")
                .parametros(parametro)
                .build();
        return new ReportGenerator().getReporte(reporte);
    }

    @PostMapping("/detalle_manzana")
    public ResponseEntity<?> reporteDetalleManzana(
            @RequestParam("manzana") Integer manzana,
            @RequestParam("codigoZona") Integer codigoZona,
            @RequestParam("desde") Integer desde,
            @RequestParam("hasta") Integer hasta) throws JRException, IOException, SQLException {
        Map<String, Object> parametro = new HashMap<>();
        parametro.put("codigoSucursal", getSucursalSession().getCodigoSucursal());
        parametro.put("codigoZona", codigoZona);
        parametro.put("manzana", manzana);
        parametro.put("desde", desde);
        parametro.put("hasta", hasta);
        Reporte reporte = Reporte.builder()
                .conexion(dataSource.getConnection())
                .nombre("Detalle Manzana")
                .ruta("reportes/detalleManzana.jasper")
                .parametros(parametro)
                .build();
        return new ReportGenerator().getReporte(reporte);
    }

    @PostMapping("/ingresos")
    public ResponseEntity<?> reporteIngresosPorZona(
            @RequestParam("grupo") String grupo,
            @RequestParam("ingresos-desde") String desde,
            @RequestParam("ingresos-hasta") String hasta) throws JRException, IOException, SQLException {
        Map<String, Object> parametro = new HashMap<>();
        String nombe = "Ingresos por zona";
        parametro.put("desde", desde);
        parametro.put("hasta", hasta);
        String ruta = "";
        if (grupo.equals("zona")) {
            ruta = "reportes/ingresosPorZona.jasper";
        } else if (grupo.equals("cob")) {
            ruta = "reportes/ingresosPorCobrador.jasper";
        }
        Reporte reporte = Reporte.builder()
                .conexion(dataSource.getConnection())
                .nombre(nombe)
                .ruta(ruta)
                .parametros(parametro)
                .build();
        return new ReportGenerator().getReporte(reporte);
    }

    @GetMapping("/imprimir")
    @ResponseBody
    public void imorimir(Map<String, Object> parameters, HttpServletResponse response)
            throws JRException, IOException, SQLException {
        InputStream jasperStream = new ClassPathResource("reportes/ciudades.jasper").getInputStream();
        JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource.getConnection());
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=ciudades.pdf");
        final OutputStream outputStream = response.getOutputStream();
        JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
        // JasperPrintManager.printReport(jasperPrint, false);
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal getSucursalSession() {
        return getUserSession().getSucursal();
    }
}
