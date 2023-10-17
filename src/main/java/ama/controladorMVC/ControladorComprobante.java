package ama.controladorMVC;

import ama.DTO.ComprobanteDTO;
import ama.dominio.*;
import ama.servicio.*;
import ama.utilerias.PageRender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/comprobante")
public class ControladorComprobante {

    @Autowired
    private ServicioServicio servicioServicio;
    @Autowired
    private ServicioCategoria servicioCategoria;

    @Autowired
    private ServicioSucursal servicioSucursal;

    @Autowired
    private ServicioEstado servicioEstado;

    @Autowired
    private ServicioComprobante servicioComprobate;
    @Autowired
    private ServicioDetalleComprobante servicioDetalleComprobante;
    @Autowired
    private ServicioTipoFactura servicioTipoFactura;
    @Autowired
    private ServicioPuntoExpedicion servicioPuntoExpedicion;
    @Autowired
    private ServicioCondicionVenta servicioCondicionVenta;

    @Autowired
    private DataSource dataSource;

    List<String> errores = new ArrayList<>();

    @GetMapping("/listar")
    public String listar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantElemento", defaultValue = "10") int cantElemento,
            Model modelo) {
        modelo.addAttribute("titulo", "Comprobates");
        Pageable pageable = PageRequest.of(page, cantElemento);
        Page<Comprobante> comprobantes = servicioComprobate.listar(pageable);
        modelo.addAttribute("comprobantes", comprobantes);
        PageRender pageRender = new PageRender("/comprobante/listar", comprobantes);
        modelo.addAttribute("page", pageRender);
        modelo.addAttribute("cantElemento", cantElemento);
        return "comprobantes/comprobantes";
    }

    @ResponseBody
    @PostMapping("/filtrar")
    public ResponseEntity<?> filtrar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantElemento", defaultValue = "5") int cantElemento,
            @RequestParam(name = "filtro", defaultValue = "") String filtro, Model modelo) {
        Pageable pageable = PageRequest.of(page, cantElemento);

        Page<Comprobante> comprobantes = servicioComprobate.filtrar(pageable, filtro);
        List<ComprobanteDTO> comprobantesDTOs = new ArrayList<>();
        comprobantes.forEach(comprobante -> {
            ComprobanteDTO comprobanteDTO = new ComprobanteDTO();
            Servicio servicio = comprobante.getServicio();
            Usuario usuario = comprobante.getUsuario();
            List<DetalleComprobante> detalleComprobante = comprobante.getDetalleComprobante();
            comprobanteDTO.setSucursal(comprobante.getSucursal());
            comprobanteDTO.setPuntoExpedicion(comprobante.getPuntoExpedicion());
            comprobanteDTO.setTipoFactura(comprobante.getTipoFactura());
            comprobanteDTO.setCodigoSerie(comprobante.getSerie().getCodigoSerie());
            comprobanteDTO.setNumeroComprobante(comprobante.getComprobantePK().getNumeroComprobante());
            comprobanteDTO.setFechaPago(comprobante.getFechaPago());
            comprobanteDTO.setTarifa(comprobante.getTarifa());
            comprobanteDTO.setEstado(comprobante.getEstado().getEstado());
            comprobanteDTO.setCuentaCorriente(servicio.getCuentaCorriente());
            comprobanteDTO.setNumeroDocumento(usuario.getNumeroDocumento());
            String nombreUsuario = servicio.getUsuario().getNombre() + " " + servicio.getUsuario().getApellido();
            comprobanteDTO.setNombreUsuario(nombreUsuario);
            detalleComprobante.forEach(dtc -> {
                comprobanteDTO.setPeriodoPago(dtc.getPeriodoPago());
                comprobanteDTO.setCantidadPago(dtc.getCantidadPago());
                comprobanteDTO.setRecargo(dtc.getRecargo());
                comprobanteDTO.setImporte(dtc.getImporte());
            });
            comprobantesDTOs.add(comprobanteDTO);

        });
        return ResponseEntity.ok(comprobantesDTOs);
    }

    @PostMapping("/listar/pagina")
    public @ResponseBody
    Page<Servicio> listarServicios(@RequestBody Paginador paginador) {
        // el primer parametro corresponde al numero de pagina y el segundo cantidad der
        // registro por pagina
        Pageable pageable = PageRequest.of(paginador.getNumeroPagina(), paginador.getCatidadRegistro());
        var servicios = servicioServicio.buscar(pageable, paginador.getFiltro());
        // log.info("resultado: " + paginador);

        return servicios;
    }

    @GetMapping("/facturaManual")
    public String facturaManual(Model modelo) {
        modelo.addAttribute("titulo", "Comprobate");

        var servicio = new Servicio();
        modelo.addAttribute("servicio", servicio);

        var categoria = new Categoria();
        modelo.addAttribute("categoria", categoria);

        var comprobante = new Comprobante();
        modelo.addAttribute("comprobante", comprobante);

        var usuario = new Usuario();
        modelo.addAttribute("usuario", usuario);

        var estadoCuenta = new EstadoCuenta();
        modelo.addAttribute("estadoCuenta", estadoCuenta);

        var sucursal = servicioSucursal.listar();
        modelo.addAttribute("sucursal", sucursal);

        var puntoExpedicion = servicioPuntoExpedicion.listar();
        modelo.addAttribute("puntoExpedicion", puntoExpedicion);

        List<TipoFactura> tiposFactura = new ArrayList<>();
        tiposFactura.add(new TipoFactura(2, "MANUAL"));
        modelo.addAttribute("tiposFactura", tiposFactura);

        var condicionVenta = servicioCondicionVenta.listar();
        modelo.addAttribute("condicionVenta", condicionVenta);

        return "comprobantes/facturaManual";
    }

    @GetMapping("/facturaElectronica")
    public String facturaElectronica(Model modelo) {
        modelo.addAttribute("titulo", "Comprobate");

        var servicio = new Servicio();
        modelo.addAttribute("servicio", servicio);

        var cobrador = new Cobrador();
        modelo.addAttribute("cobrador", cobrador);

        var categoria = new Categoria();
        modelo.addAttribute("categoria", categoria);

        var comprobante = new Comprobante();
        modelo.addAttribute("comprobante", comprobante);

        var usuario = new Usuario();
        modelo.addAttribute("usuario", usuario);

        var estadoCuenta = new EstadoCuenta();
        modelo.addAttribute("estadoCuenta", estadoCuenta);

        var sucursal = servicioSucursal.listar();
        modelo.addAttribute("sucursal", sucursal);

        var puntoExpedicion = servicioPuntoExpedicion.listar();
        modelo.addAttribute("puntoExpedicion", puntoExpedicion);

        List<TipoFactura> tiposFactura = new ArrayList<>();
        tiposFactura.add(new TipoFactura(1, "ELECTRONICA"));
        modelo.addAttribute("tiposFactura", tiposFactura);

        var condicionVenta = servicioCondicionVenta.listar();
        modelo.addAttribute("condicionVenta", condicionVenta);

        return "comprobantes/facturaElectronica";
    }

    @PostMapping("/facturar")
    @ResponseBody
    public ResponseEntity<?> getServiciosCuenta(@RequestBody @Valid Servicio servicio, BindingResult result) {
        if (result.hasErrors()) {
            StringBuilder mensajeError = new StringBuilder();
            result.getAllErrors().forEach(error -> {
                mensajeError.append("Error: " + error.getDefaultMessage());
            });
            return ResponseEntity.status(HttpStatus.CONFLICT).body(" " + mensajeError);
        }
        Servicio servicioRecuperada = servicioServicio.encontrar(servicio.getCuentaCorriente());

        if (servicioRecuperada == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Registro no encontrado!!");
        }
        servicio = servicioRecuperada;
        Categoria categoria = servicio.getCategoria();

        Manzana manzana = servicio.getManzana();
        Cobrador cobrador = manzana.getCobrador();
        servicio.setCobrador(cobrador);

        // CARGAR DATOS PARA CARCULAR ESTADO DE CUENTA
        int cantidadPagado = servicioDetalleComprobante.getCantidadPago(servicio.getCuentaCorriente()); // recuperar
        // cantidad de
        // pagos

        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setPeriodoPagado(cantidadPagado);
        estadoCuenta.setFechaInicio(servicio.getFechaInicio());
        estadoCuenta.setTarifa(categoria.getTarifa());
        estadoCuenta.getSubTotal();
        estadoCuenta.getTotalDeuda();
        estadoCuenta.getPagoHasta();
        servicio.setEstadoCuenta(estadoCuenta);
        return ResponseEntity.ok(servicio);
    }

    @GetMapping("/facturar/{cuentaCorriente}")
    public String getServiciosCuenta(Servicio servicio, Comprobante comprobante, Model modelo) {
        modelo.addAttribute("titulo", "Comprobate");
        
        servicio = servicioServicio.encontrar(servicio.getCuentaCorriente());
        modelo.addAttribute("servicio", servicio);

        var comprobantePK = new ComprobantePK();
        modelo.addAttribute("comprobantePK", comprobantePK);

        
        modelo.addAttribute("comprobante", comprobante);

        modelo.addAttribute("usuario", servicio.getUsuario());

        Categoria categoria = servicio.getCategoria();
        modelo.addAttribute("categoria", servicio.getCategoria());
        
        Manzana manzana = servicio.getManzana();
        Cobrador cobrador = manzana.getCobrador();
        modelo.addAttribute("cobrador", cobrador);
        // CARGAR DATOS PARA CARCULAR ESTADO DE CUENTA
        int cantidadPagado = servicioDetalleComprobante.getCantidadPago(servicio.getCuentaCorriente());
        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setPeriodoPagado(cantidadPagado);
        estadoCuenta.setFechaInicio(servicio.getFechaInicio());
        estadoCuenta.setTarifa(categoria.getTarifa());
        estadoCuenta.getSubTotal();
        estadoCuenta.getTotalDeuda();
        estadoCuenta.getPagoHasta();
        modelo.addAttribute("estadoCuenta", estadoCuenta);

        Sucursal sucursal = servicio.getManzana().getSucursal();
        modelo.addAttribute("sucursal", sucursal);

        Ciudad ciudad = manzana.getSucursal().getCiudad();
        modelo.addAttribute("ciudad", ciudad);

        var puntoExpedicion = servicioPuntoExpedicion.listar();
        modelo.addAttribute("puntoExpedicion", puntoExpedicion);

        var tiposFactura = servicioTipoFactura.listar();
        modelo.addAttribute("tiposFactura", tiposFactura);

        var condicionVenta = servicioCondicionVenta.listar();
        modelo.addAttribute("condicionVenta", condicionVenta);

        return "comprobantes/facturaManual";
    }

    @PostMapping("/guardar")
    @Transactional
    @ResponseBody
    public ResponseEntity<?> guardar(
            @RequestParam String cuentaCorriente,
            @RequestParam Integer codigoSerie,
            @RequestParam Integer codigoTimbrado,
            @RequestParam Integer numeroComprobante,
            @RequestParam Integer codigoSucursal,
            @RequestParam Integer codigoPuntoExpedicion,
            @RequestParam Integer codigoTipoFactura,
            @RequestParam Integer codigoMetodoPago,
            @RequestParam Integer codigoCondicionVenta,
            @RequestParam Integer codigoUsuario,
            @RequestParam Integer codigoCobrador,
            @RequestParam Integer codigoComision,
            @RequestParam Integer cantidadPago,
            @RequestParam double recargoPago) {

        ComprobantePK comprobantePK = new ComprobantePK();
        comprobantePK.setNumeroComprobante(numeroComprobante);
        comprobantePK.setCodigoTipoFactura(codigoTipoFactura);
        comprobantePK.setCodigoSucursal(codigoSucursal);
        comprobantePK.setCodigoPuntoExpedicion(codigoPuntoExpedicion);
        comprobantePK.setCodigoSerie(codigoSerie);

        Comprobante comprobanteRecuperado = servicioComprobate.getComprobante(comprobantePK);
        if (comprobanteRecuperado != null) {
            if (comprobantePK.equals(comprobanteRecuperado.getComprobantePK())) {
                Servicio servicio = comprobanteRecuperado.getServicio();
                String nombreUsuario = servicio.getUsuario().getNombre() + " " + servicio.getUsuario().getApellido();
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Este comprobante ya fue registrado en Fecha: " + comprobanteRecuperado.getFechaPago()
                                + " con la Cuentacorriente: " + servicio.getCuentaCorriente()
                                + " a nombre de " + nombreUsuario);
            }
        }

        Servicio servicio = servicioServicio.encontrar(cuentaCorriente);
        EstadoCuenta estadoCuenta = getEstadoCuenta(
                servicio,
                cantidadPago);

        Comprobante comprobante = new Comprobante();
        comprobante.setComprobantePK(comprobantePK);
        comprobante.setCobrador(new Cobrador(codigoCobrador));
        comprobante.setCondicionVenta(new CondicionVenta(codigoComision));
        comprobante.setCantidadDeuda(estadoCuenta.getCantidadDeuda());
        comprobante.setTarifa(estadoCuenta.getTarifa());
        comprobante.setServicio(servicio);
        comprobante.setUsuario(servicio.getUsuario());
        comprobante.setEstado(new Estado(1));
        comprobante.setTimbrado(new Timbrado(codigoTimbrado)); // Dato provisorio
        comprobante.setUsuarioSistema(new UsuarioSistema(codigoUsuario)); // Dato provisorio

        DetalleComprobantePK detalleComprobantePK = getDetalleComprobantePK(
                comprobantePK,
                codigoMetodoPago);

        List<DetalleComprobante> detalleComprobante = new ArrayList<>();
        detalleComprobante.add(getDetalleComprobante(
                detalleComprobantePK,
                comprobante,
                estadoCuenta,
                recargoPago, codigoMetodoPago, codigoComision));
        comprobante.setDetalleComprobante(detalleComprobante);

        // Validación manual de los objetos
        Set<ConstraintViolation<?>> violaciones = new HashSet<>();
        Validator validador = Validation.buildDefaultValidatorFactory().getValidator();
        violaciones.addAll(validador.validate(detalleComprobante));
        violaciones.addAll(validador.validate(detalleComprobantePK));
        violaciones.addAll(validador.validate(comprobante));
        for (ConstraintViolation<?> violacion : violaciones) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: " + violacion.getMessage());
        }
        try {
            servicioComprobate.guardar(comprobante);
            return ResponseEntity.ok("Factura Guardada Correctamente !!!");
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar factura: " + e.getMostSpecificCause().getMessage());
        }

    }

    @PostMapping("/anular")
    public ResponseEntity<?> editar(
            @RequestParam Integer codigoSerie,
            @RequestParam Integer codigoTimbrado,
            @RequestParam Integer numeroComprobante,
            @RequestParam Integer codigoSucursal,
            @RequestParam Integer codigoPuntoExpedicion,
            @RequestParam Integer codigoTipoFactura,
            @RequestParam String motivoAnulacion
    ) {

        ComprobantePK comprobantePK = new ComprobantePK();
        comprobantePK.setNumeroComprobante(numeroComprobante);
        comprobantePK.setCodigoTipoFactura(codigoTipoFactura);
        comprobantePK.setCodigoSucursal(codigoSucursal);
        comprobantePK.setCodigoPuntoExpedicion(codigoPuntoExpedicion);
        comprobantePK.setCodigoSerie(codigoSerie);

        Comprobante comprobante = servicioComprobate.getComprobante(comprobantePK);
        comprobante.setEstado(new Estado(3));
        comprobante.getDetalleComprobante().get(0).setObs(motivoAnulacion);
        
        servicioComprobate.anular(comprobante);
        return ResponseEntity.ok("Comprobante anulada correctamente!!");
    }

    @ResponseBody
    @PostMapping("/getComprobante")
    public ResponseEntity<?> getComprobante(@RequestBody ComprobantePK comprobantePK) {
        Comprobante comprobante = servicioComprobate.getComprobante(comprobantePK);
        if(comprobante.getEstado().getCodigoEstado()==3){
           return ResponseEntity.status(HttpStatus.CONFLICT).body("Comprobante ya se encuentra anulada");
        }
        ComprobanteDTO comprobanteDTO = new ComprobanteDTO();
        Servicio servicio = comprobante.getServicio();
        Usuario usuario = comprobante.getUsuario();
        List<DetalleComprobante> detalleComprobante = comprobante.getDetalleComprobante();
        comprobanteDTO.setSucursal(comprobante.getSucursal());
        comprobanteDTO.setPuntoExpedicion(comprobante.getPuntoExpedicion());
        comprobanteDTO.setTipoFactura(comprobante.getTipoFactura());
        comprobanteDTO.setCondicionVenta(comprobante.getCondicionVenta());
        comprobanteDTO.setCodigoSerie(comprobante.getSerie().getCodigoSerie());
        comprobanteDTO.setNumeroComprobante(comprobante.getComprobantePK().getNumeroComprobante());
        comprobanteDTO.setFechaPago(comprobante.getFechaPago());
        comprobanteDTO.setTarifa(comprobante.getTarifa());
        comprobanteDTO.setEstado(comprobante.getEstado().getEstado());
        comprobanteDTO.setCuentaCorriente(servicio.getCuentaCorriente());
        comprobanteDTO.setNumeroDocumento(usuario.getNumeroDocumento());
        String nombreUsuario = servicio.getUsuario().getNombre() + " " + servicio.getUsuario().getApellido();
        comprobanteDTO.setNombreUsuario(nombreUsuario);
        comprobanteDTO.setCategoria(servicio.getCategoria());
        comprobanteDTO.setCobrador(comprobante.getCobrador());
        detalleComprobante.forEach(dtc -> {
            comprobanteDTO.setPeriodoPago(dtc.getPeriodoPago());
            comprobanteDTO.setCantidadPago(dtc.getCantidadPago());
            comprobanteDTO.setRecargo(dtc.getRecargo());

            comprobanteDTO.setImporte(dtc.getImporte());
        });
        return ResponseEntity.ok(comprobanteDTO);

    }

    @GetMapping("/report1")
    @ResponseBody
    public void getRpt1(Map<String, Object> parameters,
            HttpServletResponse response,
            HttpServletRequest request) throws JRException, IOException, SQLException {
        InputStream jasperStream = new ClassPathResource("reportes/ciudades.jasper").getInputStream();
        JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource.getConnection());
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=ciudades.pdf");
        final OutputStream outputStream = response.getOutputStream();
        JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
    }

    @PostMapping("/numComprobante")
    @ResponseBody
    public Integer getNumeroComprobante(@RequestBody ComprobantePK comprobantePK) {

        return servicioComprobate.getNumeroComprobante(comprobantePK);
    }

    private EstadoCuenta getEstadoCuenta(Servicio servicio, int cantidadPago) {
        int periodoPagado = servicioDetalleComprobante.getCantidadPago(servicio.getCuentaCorriente());
        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setFechaInicio(servicio.getFechaInicio());
        estadoCuenta.setTarifa(servicio.getCategoria().getTarifa());
        estadoCuenta.setPeriodoPagado(periodoPagado);
        estadoCuenta.setCantidadPago(cantidadPago);
        return estadoCuenta;
    }

    private DetalleComprobante getDetalleComprobante(
            DetalleComprobantePK detalleComprobantePK,
            Comprobante comprobante,
            EstadoCuenta estadoCuenta,
            double recargo, int codigoMetodoPago, int codigoComision) {
        List<MetodoPago> metodosPago = new ArrayList<>();
        metodosPago.add(new MetodoPago(codigoMetodoPago));
        DetalleComprobante detalleComprobante = new DetalleComprobante();
        detalleComprobante.setDetalleComprobantePK(detalleComprobantePK);
        // detalleComprobante.setComprobante(comprobante);
        detalleComprobante.setCantidadPago(estadoCuenta.getCantidadPago());
        detalleComprobante.setRecargo(recargo);
        detalleComprobante.setImporte(estadoCuenta.getTotalImporte());
        detalleComprobante.setPeriodoPago(estadoCuenta.getPeriodoPago());
        detalleComprobante.setMetodopago(metodosPago);
        detalleComprobante.setComision(new Comision(codigoComision));
        return detalleComprobante;
    }

    private DetalleComprobantePK getDetalleComprobantePK(ComprobantePK comprobantePK, int codigoMetodoPago) {
        var detalleComprobantePK = new DetalleComprobantePK();
        detalleComprobantePK.setCodigoMetodoPago(codigoMetodoPago);
        detalleComprobantePK.setCodigoSucursal(comprobantePK.getCodigoSucursal());
        detalleComprobantePK.setCodigoPuntoExpedicion(comprobantePK.getCodigoPuntoExpedicion());
        detalleComprobantePK.setNumeroComprobante(comprobantePK.getNumeroComprobante());
        detalleComprobantePK.setCodigoTioFactura(comprobantePK.getCodigoTipoFactura());
        detalleComprobantePK.setCodigoSerie(comprobantePK.getCodigoSerie());
        return detalleComprobantePK;
    }
}
