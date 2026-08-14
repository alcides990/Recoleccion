package ama.api.controladores;

import ama.DTO.ZonaDTO;
import ama.dao.ComprobanteDao;
import ama.dao.ServicioDao;
import ama.dominio.Cobrador;
import ama.dominio.Comision;
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
import ama.servicio.SerieService;
import ama.servicio.ComisionService;
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
    @Autowired
    private ComprobanteDao comprobanteDao;
    @Autowired
    private ServicioDao servicioDao;
    @Autowired
    private SerieService serieService;
    @Autowired
    private ComisionService comisionService;

    @GetMapping
    public String getReporte(Model model) {
        Sucursal sucursal = getSucursalSession();
        var cobrador = servicioCobrador.listarIsEstadoActivo(sucursal);
        model.addAttribute("cobradores", cobrador);
        model.addAttribute("totalFacturas", comprobanteDao.contarPorSucursal(sucursal.getCodigoSucursal()));
        model.addAttribute("facturasAnuladas", comprobanteDao.contarAnuladosPorSucursal(sucursal.getCodigoSucursal()));
        model.addAttribute("totalServicios", servicioDao.contarServiciosPorSucursal(sucursal.getCodigoSucursal()));
        model.addAttribute("series", serieService.listar());
        model.addAttribute("comisiones", comisionService.listar());

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

    @GetMapping("/comisiones")
    @ResponseBody
    public ResponseEntity<?> listarComisionesReporte() {
        if (getUserSession() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(comisionService.listar().stream().map(comision -> {
            Map<String, Object> item = new HashMap<>();
            item.put("codigoComision", comision.getCodigoComision());
            item.put("nombreComision", comision.getNombreComision() == null ? "" : comision.getNombreComision());
            item.put("comision", comision.getComision() == null ? "" : comision.getComision());
            return item;
        }).toList());
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

    @PostMapping("/facturas_omitidas")
    public ResponseEntity<?> reporteFacturasOmitidas(
            @RequestParam("codigoSerie") Integer codigoSerie,
            @RequestParam("desde") Integer desde,
            @RequestParam("hasta") Integer hasta) throws SQLException {
        if (desde == null || hasta == null || desde < 0 || hasta < desde) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "El rango de comprobantes no es válido."));
        }
        if ((long) hasta - desde > 99999) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "El rango no puede superar 100.000 comprobantes."));
        }

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("codigoSucursal", getSucursalSession().getCodigoSucursal());
        parametros.put("codigoSerie", codigoSerie);
        parametros.put("desde", desde);
        parametros.put("hasta", hasta);

        Reporte reporte = Reporte.builder()
                .conexion(dataSource.getConnection())
                .nombre("Facturas omitidas")
                .ruta("reportes/facturasOmitidas.jasper")
                .parametros(parametros)
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
            @RequestParam("ingresos-hasta") String hasta,
            @RequestParam(name = "codigoCobrador", required = false, defaultValue = "0") Integer codigoCobrador,
            @RequestParam(name = "codigoSerie", required = false, defaultValue = "0") Integer codigoSerie,
            @RequestParam(name = "codigoComision", required = false, defaultValue = "0") Integer codigoComision)
            throws JRException, IOException, SQLException {
        Map<String, Object> parametro = new HashMap<>();
        String nombre = "Ingresos por zona";
        parametro.put("desde", desde);
        parametro.put("hasta", hasta);
        String ruta = "";
        if (grupo.equals("zona")) {
            ruta = "reportes/ingresosPorZona.jrxml";
        } else if (grupo.equals("cob")) {
            nombre = "Ingresos por cobrador";
            ruta = "reportes/ingresosPorCobrador.jasper";
        } else if (grupo.equals("comision")) {
            Comision comision = comisionService.encontrar(new Comision(codigoComision));
            if (comision == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("mensaje", "Seleccione una comision valida."));
            }
            nombre = "Comisiones - " + comision.getNombreComision();
            ruta = "reportes/comisionesPorCobrador.jrxml";
            parametro.put("codigoSucursal", getSucursalSession().getCodigoSucursal());
            parametro.put("codigoComision", codigoComision);
            parametro.put("comisionSeleccionada", comision.getNombreComision());
        } else if (grupo.equals("seguimiento")) {
            Cobrador cobrador = servicioCobrador.encontrar(new Cobrador(codigoCobrador));
            if (cobrador == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("mensaje", "Seleccione un cobrador válido."));
            }
            nombre = "Seguimiento de cobranza - " + cobrador.getNombreCompleto();
            ruta = "reportes/seguimientoCobrador.jasper";
            parametro.put("codigoSucursal", getSucursalSession().getCodigoSucursal());
            parametro.put("codigoCobrador", codigoCobrador);
            parametro.put("codigoSerie", codigoSerie);
            parametro.put("cobrador", cobrador.getNombreCompleto());
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "Tipo de reporte no válido."));
        }
        Reporte reporte = Reporte.builder()
                .conexion(dataSource.getConnection())
                .nombre(nombre)
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
