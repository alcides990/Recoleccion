package ama.controladorMVC;

import ama.DTO.ComprobanteDTO;
import ama.DTO.DetallePagoDTO;
import ama.dominio.*;
import ama.dao.DetalleTimbradoDao;
import ama.facturaelectronica.FacturaElectronicaService;
import ama.servicio.*;
import ama.utilerias.TableResponse;
import ama.utilerias.PageRender;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private TipoComprobanteService servicioTipoFactura;
    @Autowired
    private PuntoExpedicionService servicioPuntoExpedicion;
    @Autowired
    private CondicionVentaService servicioCondicionVenta;
    @Autowired
    private MetodoPagoService metodoPagoService;
    @Autowired
    private SerieService serieService;
    @Autowired
    private CobradorService cobradorService;
    @Autowired
    private ComisionService comisionService;
    @Autowired
    private EstadoService estadoService;
    @Autowired
    private TipoComprobanteService tipoComprobanteService;
    @Autowired
    private HttpSession httpSession;
    @Autowired
    private DetalleTimbradoDao detalleTimbradoDao;
    @Autowired
    private FacturaElectronicaService facturaElectronicaService;

    @GetMapping("/listar")
    public String listar(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantidadRegistro", defaultValue = "10") int cantidadRegistro,
            Model modelo) {
        if (cantidadRegistro > 100) {
            throw new RuntimeException(cantidadRegistro + " registros excede lo permitido!!");
        }
        modelo.addAttribute("titulo", "Comprobates");
        Sucursal sucursal = getSucursalSession();
        modelo.addAttribute("sucursal", sucursal);
        modelo.addAttribute("puntosExpedicion", servicioPuntoExpedicion.listar(sucursal));
        modelo.addAttribute("series", serieService.listar());
        modelo.addAttribute("cobradores", cobradorService.listarIsEstadoActivo(sucursal));
        modelo.addAttribute("estados", estadoService.findByEstadoIn(Arrays.asList("ACTIVO", "ANULADO")));
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
            @RequestParam(name = "numeroPagina", defaultValue = "0") int page,
            @RequestParam(name = "cantidadRegistro", defaultValue = "10") int cantidadRegistro,
            @RequestParam(name = "codigoSucursal", defaultValue = "") Integer codigoSucursal,
            @RequestParam(name = "codigoPuntoExpedicion", defaultValue = "") Integer codigoPuntoExpedicion,
            @RequestParam(name = "numeroComprobante", defaultValue = "") Integer numeroComprobante,
            @RequestParam(name = "codigoSerie", defaultValue = "") Integer codigoSerie,
            Model modelo) {
        Pageable pageable = PageRequest.of(page, cantidadRegistro);
        ComprobantePK comprobantePK = new ComprobantePK();
        PuntoExpedicionPK puntoExpedicionPK = new PuntoExpedicionPK(codigoSucursal, codigoPuntoExpedicion);
        comprobantePK.setPuntoExpedicionPK(puntoExpedicionPK);
        comprobantePK.setCodigoSerie(codigoSerie);
        if (cantidadRegistro > 100) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cantidad de registro excede lo permitido!!");
        }
        if (numeroComprobante != null) {
            comprobantePK.setNumeroComprobante(numeroComprobante);
        }
        Page<Comprobante> comprobantes = comprobanteService.filtrar(pageable, comprobantePK);
        TableResponse<ComprobanteDTO> dataTableResponse = new TableResponse<>();
        if (comprobantes != null) {
            List<ComprobanteDTO> comprobantesDTOs = new ArrayList<>();
            comprobantes.forEach(comprobante -> {
                ComprobanteDTO comprobanteDTO = new ComprobanteDTO();
                Servicio servicio = comprobante.getServicio();
                Usuario usuario = comprobante.getUsuario();
                comprobanteDTO.setCodigoPuntoExpedicion(comprobante.getPuntoExpedicion().getPuntoExpedicionPK().getCodigoPuntoExpedicion());
                comprobanteDTO.setPuntoExpedicion(comprobante.getPuntoExpedicion().getNombrePuntoExpedicion());
                comprobanteDTO.setTipoComprobante(comprobante.getTipoComprobante());
                comprobanteDTO.setSerie(comprobante.getSerie());
                comprobanteDTO.setNumeroComprobante(comprobante.getComprobantePK().getNumeroComprobante());
                comprobanteDTO.setFechaPago(comprobante.getFechaPago());
                comprobanteDTO.setCantidadPago(comprobante.getCantidadPago());
                comprobanteDTO.setEstado(comprobante.getEstado());
                comprobanteDTO.setCuentaCorriente(servicio.getCuentaCorriente());
                comprobanteDTO.setNumeroDocumento(usuario.getNumeroDocumento());
                String nombreUsuario = servicio.getUsuario().getNombre() + " " + servicio.getUsuario().getApellido();
                comprobanteDTO.setNombreUsuario(nombreUsuario);
                comprobanteDTO.setPeriodoPago(comprobante.getPeriodoPago());
                comprobanteDTO.setTarifa(comprobante.getTarifa());
                comprobanteDTO.setRecargo(comprobante.getRecargo());
                comprobanteDTO.setImporte(comprobante.getTotalImporte());
                comprobantesDTOs.add(comprobanteDTO);

            });
            PageRender pageRender = new PageRender("/comprobante/filtrar", comprobantes);

            dataTableResponse.setPage(pageRender);
            dataTableResponse.setData(comprobantesDTOs);
        }
        return ResponseEntity.ok(dataTableResponse);
    }

    @PostMapping("/listar/pagina")
    public @ResponseBody
    Page<Servicio> listarServicios(@RequestBody Paginador paginador) {
        Pageable pageable = PageRequest.of(paginador.getNumeroPagina(), paginador.getCatidadRegistro());
        var servicios = servicioService.buscar(pageable, paginador.getFiltro());

        return servicios;
    }

    @GetMapping("/facturaManual")
    public String facturaManual(Model modelo) {
        modelo.addAttribute("tiposComprobante", tipoComprobanteService.listar());
        cargarDatosComprobante(modelo);

        return "comprobantes/facturaManual";
    }

    @GetMapping("/facturaElectronica")
    public String facturaElectronica(Model modelo) {
        modelo.addAttribute("tiposComprobante", tipoComprobanteService.listar());

        cargarDatosComprobante(modelo);
        return "comprobantes/facturaManual";
    }

    private void cargarDatosComprobante(Model modelo) {
        modelo.addAttribute("titulo", "Comprobate");
        modelo.addAttribute("condicionesVenta", servicioCondicionVenta.listar());
        modelo.addAttribute("servicio", new Servicio());

        modelo.addAttribute("cobrador", cobradorService.listarIsEstadoActivo(getSucursalSession()));

        modelo.addAttribute("categoria", new Categoria());

        modelo.addAttribute("comprobante", new Comprobante());

        modelo.addAttribute("usuario", new Usuario());

        modelo.addAttribute("estadoCuenta", new EstadoCuenta());

        modelo.addAttribute("metodosPago", metodoPagoService.listar());
        modelo.addAttribute("comisiones", comisionService.listar());

        modelo.addAttribute("sucursal", getSucursalSession());

        modelo.addAttribute("puntoExpedicion", servicioPuntoExpedicion.isMayorCero(getSucursalSession()));

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
        servicio.setEstadoCuenta(getEstadoCuenta(servicio.getCuentaCorriente()));
        return ResponseEntity.ok(servicio);

    }

    @PostMapping("/guardar")
    @Transactional
    @ResponseBody
    public ResponseEntity<?> guardarComprobante(@RequestBody ComprobanteGuardar comprobanteRequest) {
        if (comprobanteRequest.getCodigoComision() == null) {
            return ResponseEntity.badRequest().body("Seleccione una comision.");
        }
        Comision comision = comisionService.encontrar(new Comision(comprobanteRequest.getCodigoComision()));
        if (comision == null) {
            return ResponseEntity.badRequest().body("La comision seleccionada no es valida.");
        }
        PuntoExpedicionPK puntoExpedicionPK = PuntoExpedicionPK.builder()
                .codigoSucursal(getSucursalSession().getCodigoSucursal())
                .codigoPuntoExpedicion(comprobanteRequest.getCodigoPuntoExpedicion())
                .build();
        if (comprobanteRequest.getCodigoTimbrado() == null || comprobanteRequest.getCodigoSerie() == null) {
            return ResponseEntity.badRequest().body("Seleccione un timbrado y serie para el punto de expedición.");
        }
        DetalleTimbradoPK detalleTimbradoPK = new DetalleTimbradoPK(
                comprobanteRequest.getCodigoTimbrado(), comprobanteRequest.getCodigoPuntoExpedicion(),
                getSucursalSession().getCodigoSucursal());
        DetalleTimbrado detalleTimbrado = detalleTimbradoDao.findById(detalleTimbradoPK).orElse(null);
        if (detalleTimbrado == null
                || detalleTimbrado.getEstado() == null || detalleTimbrado.getEstado().getCodigoEstado() != 1
                || detalleTimbrado.getSerie() == null
                || !detalleTimbrado.getSerie().getCodigoSerie().equals(comprobanteRequest.getCodigoSerie())) {
            return ResponseEntity.badRequest().body("El timbrado y la serie no están habilitados para el punto de expedición.");
        }
        Timbrado timbradoVigente = detalleTimbrado.getTimbrado();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioVigencia = timbradoVigente.getFechaInicio() == null ? null
                : java.time.Instant.ofEpochMilli(timbradoVigente.getFechaInicio().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate finVigencia = timbradoVigente.getFechaFin() == null ? null
                : java.time.Instant.ofEpochMilli(timbradoVigente.getFechaFin().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        if (timbradoVigente.getEstado() == null || timbradoVigente.getEstado().getCodigoEstado() != 1
                || inicioVigencia == null || finVigencia == null
                || hoy.isBefore(inicioVigencia) || hoy.isAfter(finVigencia)) {
            return ResponseEntity.badRequest().body("El timbrado seleccionado no está activo o está fuera de vigencia.");
        }
        ComprobantePK comprobantePK = ComprobantePK.builder()
                .numeroComprobante(comprobanteRequest.getNumeroComprobante())
                .codigoTipoComprobante(comprobanteRequest.getCodigoTipoComprobante())
                .puntoExpedicionPK(puntoExpedicionPK)
                .codigoSerie(comprobanteRequest.getCodigoSerie())
                .build();
        Optional<Comprobante> comprobanteOP = comprobanteService.findById(comprobantePK);
        if (comprobanteOP.isPresent()) {
            Comprobante comprobante = comprobanteOP.get();
            if (comprobantePK.equals(comprobante.getComprobantePK())) {
                Servicio servicio = comprobante.getServicio();
                String nombreUsuario = servicio.getUsuario().getNombre() + " " + servicio.getUsuario().getApellido();
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Este comprobante ya fue registrado en Fecha: " + comprobante.getFechaPago()
                                + " con la Cuentacorriente: " + servicio.getCuentaCorriente()
                                + " a nombre de " + nombreUsuario);
            }
        }
        if (comprobanteRequest.getDetallePago() == null || comprobanteRequest.getDetallePago().isEmpty()) {
            return ResponseEntity.badRequest().body("Agregue al menos un medio de pago.");
        }
        Map<Integer, DetallePago> pagosConsolidados = new LinkedHashMap<>();
        for (DetallePago pago : comprobanteRequest.getDetallePago()) {
            if (pago.getDetallePagoPK() == null || pago.getDetallePagoPK().getCodigoMetodoPago() == null
                    || pago.getImporte() == null || pago.getImporte() <= 0) {
                return ResponseEntity.badRequest().body("Existe un medio de pago incompleto o inválido.");
            }
            Integer codigoMetodo = pago.getDetallePagoPK().getCodigoMetodoPago();
            DetallePago existente = pagosConsolidados.get(codigoMetodo);
            if (existente == null) {
                pagosConsolidados.put(codigoMetodo, pago);
            } else {
                existente.setImporte(existente.getImporte() + pago.getImporte());
            }
        }
        comprobanteRequest.setDetallePago(new ArrayList<>(pagosConsolidados.values()));

        double totalImporte = comprobanteRequest.getDetallePago()
                .stream()
                .mapToDouble(DetallePago::getImporte)
                .sum();

        comprobanteRequest.setDetallePago(
                comprobanteRequest.getDetallePago()
                        .stream()
                        .map(detallePago -> {
                            DetallePagoPK detallePagoPK = DetallePagoPK.builder()
                                    .codigoMetodoPago(detallePago.getDetallePagoPK().getCodigoMetodoPago())
                                    .puntoExpedicionPK(puntoExpedicionPK)
                                    .numeroComprobante(comprobantePK.getNumeroComprobante())
                                    .codigoTiopoComprobante(comprobantePK.getCodigoTipoComprobante())
                                    .codigoSerie(comprobantePK.getCodigoSerie())
                                    .build();

                            detallePago.setDetallePagoPK(detallePagoPK);
                            return detallePago;
                        })
                        .toList()
        );
        comprobanteRequest.setParametro(getParametro());
        EstadoCuenta estadoCuentaActual = getEstadoCuenta(comprobanteRequest.getCuentaCorriente());
        comprobanteRequest.setPagoHasta(estadoCuentaActual.getPagoHasta());
        CalcularPago datosPagopago = new CalcularPago(comprobanteRequest);
        datosPagopago.setTotalImporte(totalImporte);

        Comprobante comprobante = Comprobante.builder()
                .comprobantePK(comprobantePK)
                .razonSocial(comprobanteRequest.getRazonSocial())
                .cobrador(new Cobrador(comprobanteRequest.getCodigoCobrador()))
                .comision(comision)
                .condicionVenta(new CondicionVenta(comprobanteRequest.getCodigoCondicionVenta()))
                .categoria(new Categoria(comprobanteRequest.getCodigoCategoria()))
                .tarifa(comprobanteRequest.getTarifa())
                .cantidadDeuda(comprobanteRequest.getCantidadDeuda())
                .cantidadPago(comprobanteRequest.getCantidadPago())
                .recargo(comprobanteRequest.getRecargoPago())
                .saldo(datosPagopago.getSaldo())
                .totalImporte(datosPagopago.getTotalImporte())
                .fechaPago(comprobanteRequest.getFechaPago())
                .periodoPago(datosPagopago.getPeriodoPago())
                .pagoHasta(datosPagopago.getPagoHasta())
                .servicio(new Servicio(comprobanteRequest.getCuentaCorriente()))
                .usuario(new Usuario(comprobanteRequest.getCodigoUsuario()))
                .estado(
                        comprobanteRequest.getCantidadPago() == 0 ? new Estado(3) : new Estado(1)
                )
                .timbrado(new Timbrado(comprobanteRequest.getCodigoTimbrado()))
                .usuarioSistema(getUserSession())
                .detallePago(comprobanteRequest.getDetallePago())
                .build();
        if (validarComprobate(comprobante).getStatusCode() != HttpStatus.OK) {
            return validarComprobate(comprobante);
        }
        try {
            comprobanteService.guardar(comprobante);
            facturaElectronicaService.registrar(comprobante);
            return ResponseEntity.ok("Comprobante Guardada Correctamente !!!");
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

        if (!violaciones.isEmpty()) {
            Map<String, String> mensajesError = new HashMap<>();
            violaciones.forEach(violacion -> {
                mensajesError.put("Error", violacion.getMessageTemplate());
            });
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensajesError);
        }
        return ResponseEntity.ok("valido");
    }

    @PutMapping("/guardar")
    public ResponseEntity<?> moficicarComprobante(@RequestBody Comprobante comprobanteReques) {
        Comprobante comprobante = comprobanteService.getComprobante(comprobanteReques.getComprobantePK());
        if (comprobanteReques.getEstado() != null
                && comprobanteReques.getEstado().getCodigoEstado() == 3
                && comprobante.getEstado().getCodigoEstado() != 3) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    "Utilice la acción Anular para revertir también la fecha desde de la cuenta");
        }
        comprobante.setCobrador(comprobanteReques.getCobrador());
        comprobante.setFechaPago(comprobanteReques.getFechaPago());
        comprobante.setEstado(comprobanteReques.getEstado());
        comprobante.setObs(comprobanteReques.getObs());
        comprobanteService.guardar(comprobante);
        return ResponseEntity.ok("Comprobante modificada correctamente!!");
    }

    @PutMapping("/anular")
    @Transactional
    public ResponseEntity<?> anulalComprobante(@RequestBody Comprobante comprobanteRequest) {
        Comprobante comprobante = comprobanteService.getComprobante(comprobanteRequest.getComprobantePK());
        if (comprobante.getEstado().getCodigoEstado() == 3) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Comprobante ya se encuentra anulada");
        }
        Optional<Comprobante> ultimoComprobanteCuentaActivo = comprobanteService
                .getUltimoComprobanteCuentaActivo(comprobante.getServicio().getCuentaCorriente());

        if (!(comprobante.getComprobantePK().getNumeroComprobante() == ultimoComprobanteCuentaActivo.get()
                .getComprobantePK().getNumeroComprobante())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    "Solo se puede anular el ultimo comprobante activo de la cuenta corriente");
        }

        comprobante.setEstado(new Estado(3));
        comprobante.setObs(comprobanteRequest.getObs());

        comprobanteService.anular(comprobante);
        return ResponseEntity.ok("Comprobante anulada correctamente!!");
    }

    @ResponseBody
    @PostMapping("/getComprobante")
    public ResponseEntity<?> getComprobante(@RequestBody ComprobantePK comprobantePK) {
        log.info(comprobantePK.toString());
        Comprobante comprobante = comprobanteService.getComprobante(comprobantePK);
        ComprobanteDTO comprobanteDTO = new ComprobanteDTO();
        Servicio servicio = comprobante.getServicio();
        Usuario usuario = comprobante.getUsuario();
        String sucursal = comprobante.getPuntoExpedicion().getSucursal().getNombreSucursal() + " " + comprobante.getPuntoExpedicion().getSucursal().getCiudad().getNombreCiudad();
        comprobanteDTO.setSucursal(sucursal);
        comprobanteDTO.setCodigoSucursal(comprobante.getPuntoExpedicion().getSucursal().getCodigoSucursal());
        comprobanteDTO.setCodigoPuntoExpedicion(comprobante.getPuntoExpedicion().getPuntoExpedicionPK().getCodigoPuntoExpedicion());
        comprobanteDTO.setPuntoExpedicion(comprobante.getPuntoExpedicion().getNombrePuntoExpedicion());
        comprobanteDTO.setTipoComprobante(comprobante.getTipoComprobante());
        comprobanteDTO.setCondicionVenta(comprobante.getCondicionVenta());
        comprobanteDTO.setSerie(comprobante.getSerie());
        comprobanteDTO.setNumeroComprobante(comprobante.getComprobantePK().getNumeroComprobante());
        comprobanteDTO.setFechaPago(comprobante.getFechaPago());
        comprobanteDTO.setTarifa(comprobante.getTarifa());
        comprobanteDTO.setEstado(comprobante.getEstado());
        comprobanteDTO.setCuentaCorriente(servicio.getCuentaCorriente());
        comprobanteDTO.setNumeroDocumento(usuario.getNumeroDocumento());
        String nombreUsuario = comprobante.getRazonSocial();
        comprobanteDTO.setNombreUsuario(nombreUsuario);
        comprobanteDTO.setCategoria(servicio.getCategoria());
        comprobanteDTO.setCobrador(comprobante.getCobrador());
        comprobanteDTO.setPeriodoPago(comprobante.getPeriodoPago());
        comprobanteDTO.setCantidadPago(comprobante.getCantidadPago());
        comprobanteDTO.setSaldo(comprobante.getSaldo());
        comprobanteDTO.setRecargo(comprobante.getRecargo());
        comprobanteDTO.setMotivoAnulacion(comprobante.getObs());

        comprobante.getDetallePago().forEach(dtp -> {
            DetallePagoDTO detallePagoDTO = new DetallePagoDTO();
            detallePagoDTO.setMetodoPago(dtp.getMetodoPago().getMetodoPago());
            detallePagoDTO.setImporte(dtp.getImporte());
            comprobanteDTO.DetallePagoAdd(detallePagoDTO);
        });

        comprobanteDTO.setImporte(comprobante.getTotalImporte());
        return ResponseEntity.ok(comprobanteDTO);

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

    private EstadoCuenta getEstadoCuenta(String cuentaCorriente) {

        Servicio servicio = servicioService.encontrar(cuentaCorriente);
        List<Object[]> datos = comprobanteService.getEstadoCuentaMovil(
                cuentaCorriente, getSucursalSession().getCodigoSucursal());
        LocalDate pagoHasta = null;
        Double saldo = 0.0;
        if (datos.isEmpty()) {
            pagoHasta = servicio.getFechaInicio();
        } else {
            for (Object[] resul : datos) {
                pagoHasta = YearMonth.parse(
                        (String) resul[0], DateTimeFormatter.ofPattern("MM-yyyy"))
                        .atDay(1);
                saldo = (Double) resul[1];

            }
        }
        EstadoCuenta estadoCuenta = new EstadoCuenta(
                servicio.getCategoria().getTarifa(),
                getParametro(),
                pagoHasta);
        estadoCuenta.setSaldoAnterior(saldo);
        if (!datos.isEmpty() && datos.get(0)[2] instanceof Number) {
            estadoCuenta.setCantidadDeuda(((Number) datos.get(0)[2]).intValue());
        }
        return estadoCuenta;
    }

    private Parametro getParametro() {
        return parametroService.encontrar(getUserSession().getSucursal());
    }

}
