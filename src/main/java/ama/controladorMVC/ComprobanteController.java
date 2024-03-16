package ama.controladorMVC;

import ama.DTO.ComprobanteDTO;
import ama.dominio.*;
import ama.servicio.*;
import ama.utilerias.PageRender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class ComprobanteController {

    @Autowired
    private ServicioService servicioService;

    @Autowired
    private ParametroService parametroService;

    @Autowired
    private ComprobanteService comprobanteService;

    @Autowired
    private ServicioTipoFactura servicioTipoFactura;
    @Autowired
    private PuntoExpedicionService servicioPuntoExpedicion;
    @Autowired
    private CondicionVentaService servicioCondicionVenta;
    @Autowired
    private MetodoPagoService metodoPagoService;
    @Autowired
    private SerieService serieService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantidadRegistro", defaultValue = "10") int cantidadRegistro,
            Model modelo) {
        modelo.addAttribute("titulo", "Comprobates");
        Sucursal sucursal = getSucursalSession();
        modelo.addAttribute("sucursal", sucursal);
        modelo.addAttribute("puntosExpedicion", servicioPuntoExpedicion.listar(sucursal));
        modelo.addAttribute("series", serieService.listar());
        Pageable pageable = PageRequest.of(page, cantidadRegistro);
        Page<Comprobante> comprobantes = comprobanteService.listar(pageable);
        modelo.addAttribute("comprobantes", comprobantes);
        PageRender pageRender = new PageRender("/comprobante/listar", comprobantes);
        modelo.addAttribute("page", pageRender);
        modelo.addAttribute("cantidadRegistro", cantidadRegistro);
        return "comprobantes/comprobantes";
    }

    @ResponseBody
    @PostMapping("/filtrar")
    public ResponseEntity<?> filtrarComprobantes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantidadRegistro", defaultValue = "5") int cantidadRegistro,
            @RequestParam(name = "codigoSucursal", defaultValue = "") Integer codigoSucursal,
            @RequestParam(name = "codigoPuntoExpedicion", defaultValue = "") Integer codigoPuntoExpedicion,
            @RequestParam(name = "numeroComprobante", defaultValue = "") Integer numeroComprobante,
            @RequestParam(name = "codigoSerie", defaultValue = "") Integer codigoSerie,
            Model modelo) {
        Pageable pageable = PageRequest.of(page, cantidadRegistro);
        ComprobantePK comprobantePK = new ComprobantePK();
        PuntoExpedicionPK puntoExpedicionPK = new PuntoExpedicionPK(codigoSucursal, codigoPuntoExpedicion);
        comprobantePK.setPuntoExpedicionPK(puntoExpedicionPK);
        comprobantePK.setNumeroComprobante(numeroComprobante);
        comprobantePK.setCodigoSerie(codigoSerie);
        Page<Comprobante> comprobantes = comprobanteService.filtrar(pageable, comprobantePK);
        List<ComprobanteDTO> comprobantesDTOs = new ArrayList<>();
        comprobantes.forEach(comprobante -> {
            ComprobanteDTO comprobanteDTO = new ComprobanteDTO();
            Servicio servicio = comprobante.getServicio();
            Usuario usuario = comprobante.getUsuario();
            DetalleComprobante detalleComprobante = comprobante.getDetalleComprobante();
            comprobanteDTO.setSucursal(comprobante.getPuntoExpedicion().getSucursal());
            comprobanteDTO.setPuntoExpedicion(comprobante.getPuntoExpedicion());
            comprobanteDTO.setTipoFactura(comprobante.getTipoFactura());
            comprobanteDTO.setSerie(comprobante.getSerie());
            comprobanteDTO.setNumeroComprobante(comprobante.getComprobantePK().getNumeroComprobante());
            comprobanteDTO.setFechaPago(comprobante.getFechaPago());
            comprobanteDTO.setCantidadPago(comprobante.getCantidadPago());
            comprobanteDTO.setEstado(comprobante.getEstado().getEstado());
            comprobanteDTO.setCuentaCorriente(servicio.getCuentaCorriente());
            comprobanteDTO.setNumeroDocumento(usuario.getNumeroDocumento());
            String nombreUsuario = servicio.getUsuario().getNombre() + " " + servicio.getUsuario().getApellido();
            comprobanteDTO.setNombreUsuario(nombreUsuario);
            comprobanteDTO.setPeriodoPago(detalleComprobante.getPeriodoPago());
            comprobanteDTO.setTarifa(detalleComprobante.getTarifa());
            comprobanteDTO.setRecargo(detalleComprobante.getRecargo());
            comprobanteDTO.setImporte(comprobante.getTotalImporte());
            comprobantesDTOs.add(comprobanteDTO);

        });
        return ResponseEntity.ok(comprobantesDTOs);
    }

    @PostMapping("/listar/pagina")
    public @ResponseBody Page<Servicio> listarServicios(@RequestBody Paginador paginador) {
        Pageable pageable = PageRequest.of(paginador.getNumeroPagina(), paginador.getCatidadRegistro());
        var servicios = servicioService.buscar(pageable, paginador.getFiltro());

        return servicios;
    }

    @GetMapping("/facturaManual")
    public String facturaManual(Model modelo) {
        List<TipoFactura> tiposFactura = new ArrayList<>();
        tiposFactura.add(new TipoFactura(2, "MANUAL"));
        modelo.addAttribute("tiposFactura", tiposFactura);

        cargarDatosComprobante(modelo);

        return "comprobantes/facturaManual";
    }

    @GetMapping("/facturaElectronica")
    public String facturaElectronica(Model modelo) {
        List<TipoFactura> tiposFactura = new ArrayList<>();
        tiposFactura.add(new TipoFactura(1, "ELECTRONICA"));
        modelo.addAttribute("tiposFactura", tiposFactura);

        cargarDatosComprobante(modelo);
        return "comprobantes/facturaManual";
    }

    private void cargarDatosComprobante(Model modelo) {
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

        EstadoCuenta estadoCuenta = new EstadoCuenta();

        modelo.addAttribute("estadoCuenta", estadoCuenta);

        modelo.addAttribute("metodosPago", metodoPagoService.listar());

        modelo.addAttribute("sucursal", getSucursalSession());

        modelo.addAttribute("puntoExpedicion", servicioPuntoExpedicion.listar(getSucursalSession()));

        modelo.addAttribute("condicionVenta", servicioCondicionVenta.listar());
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
        Servicio servicioRecuperada = servicioService.encontrar(servicio.getCuentaCorriente());

        if (servicioRecuperada == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Registro no encontrado!!");
        }
        servicio = servicioRecuperada;
        servicio.setEstadoCuenta(getEstadoCuenta(servicio));
        return ResponseEntity.ok(servicio);

    }

    @GetMapping("/facturar/{cuentaCorriente}")
    public String getServiciosCuenta(Servicio servicio, Comprobante comprobante, Model modelo) {
        modelo.addAttribute("titulo", "Comprobate");

        cargarDatosComprobante(modelo);

        modelo.addAttribute("tiposFactura", servicioTipoFactura.listar());

        servicio = servicioService.encontrar(servicio.getCuentaCorriente());
        modelo.addAttribute("servicio", servicio);

        modelo.addAttribute("estadoCuenta", getEstadoCuenta(servicio));

        modelo.addAttribute("usuario", servicio.getUsuario());

        modelo.addAttribute("cobrador", servicio.getZona().getCobrador());

        modelo.addAttribute("categoria", servicio.getCategoria());

        return "comprobantes/facturaManual";
    }

    @PostMapping("/guardar")
    @Transactional
    @ResponseBody
    public ResponseEntity<?> guardarComprobante(@RequestBody ComprobanteGuardar comprobanteRequest) {
        PuntoExpedicionPK puntoExpedicionPK = PuntoExpedicionPK.builder()
                .codigoSucursal(getSucursalSession().getCodigoSucursal())
                .codigoPuntoExpedicion(comprobanteRequest.getCodigoPuntoExpedicion())
                .build();
        ComprobantePK comprobantePK = ComprobantePK.builder()
                .numeroComprobante(comprobanteRequest.getNumeroComprobante())
                .codigoTipoFactura(comprobanteRequest.getCodigoTipoFactura())
                .puntoExpedicionPK(puntoExpedicionPK)
                .codigoSerie(comprobanteRequest.getCodigoSerie())
                .build();
        Comprobante comprobanteRecuperado = comprobanteService.getComprobante(comprobantePK);
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
        Servicio servicio = servicioService.encontrar(comprobanteRequest.getCuentaCorriente());
        double totalImporte = 0;
        for (DetallePago detallePago : comprobanteRequest.getDetallePago()) {
            totalImporte += detallePago.getImporte();
            DetallePagoPK detallePagoPK = DetallePagoPK.builder()
                    .codigoMetodoPago(detallePago.getDetallePagoPK().getCodigoMetodoPago())
                    .puntoExpedicionPK(puntoExpedicionPK)
                    .numeroComprobante(comprobantePK.getNumeroComprobante())
                    .codigoTioFactura(comprobantePK.getCodigoTipoFactura())
                    .codigoSerie(comprobantePK.getCodigoSerie())
                    .build();
            detallePago.setDetallePagoPK(detallePagoPK);
        }
        comprobanteRequest.setServicio(servicio);
        comprobanteRequest.setParametro(getParametro());
        CalcularPago datosPagopago = new CalcularPago(comprobanteRequest);
        datosPagopago.setTotalImporte(totalImporte);
        if (totalImporte < datosPagopago.getTotalPagar()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar comprobante: Total Importe no puede ser menor que Total a Pagar");
        }

        DetalleComprobantePK detalleComprobantePK = DetalleComprobantePK.builder()
                .puntoExpedicionPK(puntoExpedicionPK)
                .numeroComprobante(comprobantePK.getNumeroComprobante())
                .codigoTioFactura(comprobantePK.getCodigoTipoFactura())
                .codigoSerie(comprobantePK.getCodigoSerie())
                .build();

        DetalleComprobante detalleComprobante = DetalleComprobante.builder()
                .detalleComprobantePK(detalleComprobantePK)
                .categoria(servicio.getCategoria())
                .tarifa(datosPagopago.getTarifa())
                .cantidadDeuda(datosPagopago.getCantidadDeuda())
                .recargo(comprobanteRequest.getRecargoPago())
                .pagoHasta(datosPagopago.getPagoHasta())
                .periodoPago(datosPagopago.getPeriodoPago())
                .saldo(datosPagopago.getSaldo())
                .comision(datosPagopago.getParametro().getComision())
                .detallePago(comprobanteRequest.getDetallePago())
                .build();
        Comprobante comprobante = Comprobante.builder()
                .comprobantePK(comprobantePK)
                .razonSocial(servicio.getUsuario().getNombre().concat(" ").concat(servicio.getUsuario().getApellido()))
                .cobrador(new Cobrador(comprobanteRequest.getCodigoCobrador()))
                .condicionVenta(new CondicionVenta(comprobanteRequest.getCodigoCondicionVenta()))
                .cantidadPago(comprobanteRequest.getCantidadPago())
                .totalImporte(datosPagopago.getTotalImporte())
                .fechaPago(comprobanteRequest.getFechaPago())
                .servicio(servicio)
                .usuario(servicio.getUsuario())
                .estado(new Estado(1))
                .timbrado(new Timbrado(comprobanteRequest.getCodigoTimbrado())) // Dato provisorio
                .usuarioSistema(getUserSession())
                .detalleComprobante(detalleComprobante)
                .build();
        if (validarComprobate(comprobante).getStatusCode() != HttpStatus.OK) {
            return validarComprobate(comprobante);
        }
        try {
            comprobanteService.guardar(comprobante);
            return ResponseEntity.ok("Factura Guardada Correctamente !!!");
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar factura: " + e.getMostSpecificCause().getMessage());
        }

    }

    private ResponseEntity<?> validarComprobate(
            Comprobante comprobante) {
        Set<ConstraintViolation<?>> violaciones = new HashSet<>();
        Validator validador = Validation.buildDefaultValidatorFactory().getValidator();
        violaciones.addAll(validador.validate(comprobante));

        violaciones.addAll(validador.validate(comprobante.getDetalleComprobante()));
        if (!violaciones.isEmpty()) {
            Map<String, String> mensajesError = new HashMap<>();
            violaciones.forEach(violacion -> {
                mensajesError.put("Error", violacion.getMessageTemplate());
            });
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensajesError);
        }
        return ResponseEntity.ok("valido");
    }

    @PostMapping("/anular")
    public ResponseEntity<?> editar(
            @RequestParam Integer codigoSerie,
            @RequestParam Integer codigoTimbrado,
            @RequestParam Integer numeroComprobante,
            @RequestParam Integer codigoPuntoExpedicion,
            @RequestParam Integer codigoTipoFactura,
            @RequestParam String motivoAnulacion) {
        PuntoExpedicionPK puntoExpedicionPK = PuntoExpedicionPK.builder()
                .codigoSucursal(getSucursalSession().getCodigoSucursal())
                .codigoPuntoExpedicion(codigoPuntoExpedicion)
                .build();
        ComprobantePK comprobantePK = ComprobantePK.builder()
                .numeroComprobante(numeroComprobante)
                .codigoTipoFactura(codigoTipoFactura)
                .puntoExpedicionPK(puntoExpedicionPK)
                .codigoSerie(codigoSerie)
                .build();

        Comprobante comprobante = comprobanteService.getComprobante(comprobantePK);
        comprobante.setEstado(new Estado(3));
        comprobante.getDetalleComprobante().setObs(motivoAnulacion);

        comprobanteService.anular(comprobante);
        return ResponseEntity.ok("Comprobante anulada correctamente!!");
    }

    @ResponseBody
    @PostMapping("/getComprobante")
    public ResponseEntity<?> getComprobante(@RequestBody ComprobantePK comprobantePK) {
        Comprobante comprobante = comprobanteService.getComprobante(comprobantePK);
        if (comprobante.getEstado().getCodigoEstado() == 3) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Comprobante ya se encuentra anulada");
        }
        Optional<Comprobante> ultimoComprobanteCuentaActivo = comprobanteService
                .getUltimoComprobanteCuentaActivo(comprobante.getServicio().getCuentaCorriente());

        if (comprobante.getComprobantePK().getNumeroComprobante() != ultimoComprobanteCuentaActivo.get()
                .getComprobantePK().getNumeroComprobante()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    "Solo se puede anular el ultimo comprobante activo de la cuenta corriente");
        }
        ComprobanteDTO comprobanteDTO = new ComprobanteDTO();
        Servicio servicio = comprobante.getServicio();
        Usuario usuario = comprobante.getUsuario();
        DetalleComprobante detalleComprobante = comprobante.getDetalleComprobante();
        comprobanteDTO.setSucursal(comprobante.getPuntoExpedicion().getSucursal());
        comprobanteDTO.setPuntoExpedicion(comprobante.getPuntoExpedicion());
        comprobanteDTO.setTipoFactura(comprobante.getTipoFactura());
        comprobanteDTO.setCondicionVenta(comprobante.getCondicionVenta());
        comprobanteDTO.setSerie(comprobante.getSerie());
        comprobanteDTO.setNumeroComprobante(comprobante.getComprobantePK().getNumeroComprobante());
        comprobanteDTO.setFechaPago(comprobante.getFechaPago());
        comprobanteDTO.setTarifa(detalleComprobante.getTarifa());
        comprobanteDTO.setEstado(comprobante.getEstado().getEstado());
        comprobanteDTO.setCuentaCorriente(servicio.getCuentaCorriente());
        comprobanteDTO.setNumeroDocumento(usuario.getNumeroDocumento());
        String nombreUsuario = comprobante.getRazonSocial();
        comprobanteDTO.setNombreUsuario(nombreUsuario);
        comprobanteDTO.setCategoria(servicio.getCategoria());
        comprobanteDTO.setCobrador(comprobante.getCobrador());
        comprobanteDTO.setPeriodoPago(detalleComprobante.getPeriodoPago());
        comprobanteDTO.setCantidadPago(comprobante.getCantidadPago());
        comprobanteDTO.setRecargo(detalleComprobante.getRecargo());

        comprobanteDTO.setImporte(comprobante.getTotalImporte());
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
        PuntoExpedicionPK puntoExpedicionPK = comprobantePK.getPuntoExpedicionPK();
        puntoExpedicionPK.setCodigoSucursal(getSucursalSession().getCodigoSucursal());
        return comprobanteService.getNumeroComprobante(comprobantePK);
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal getSucursalSession() {
        return getUserSession().getSucursal();
    }

    private EstadoCuenta getEstadoCuenta(Servicio servicio) {
        List<Object[]> datos = comprobanteService.getPagoHastaAndSaldo(servicio.getCuentaCorriente());
        LocalDate pagoHasta = null;
        Double saldo = 0.0;
        if (datos.isEmpty()) {
            pagoHasta = servicio.getFechaInicio();
        } else {
            for (Object[] resul : datos) {
                pagoHasta = (LocalDate) resul[0];
                saldo = (Double) resul[1];

            }
        }
        EstadoCuenta estadoCuenta = new EstadoCuenta(
                servicio.getCategoria().getTarifa(),
                getParametro(),
                pagoHasta);
        estadoCuenta.setSaldoAnterior(saldo);
        return estadoCuenta;
    }

    private Parametro getParametro() {
        return parametroService.encontrar(getUserSession().getSucursal());
    }

}
