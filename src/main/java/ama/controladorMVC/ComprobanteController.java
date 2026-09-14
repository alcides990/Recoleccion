package ama.controladorMVC;

import ama.DTO.ComprobanteDTO;
import ama.DTO.DetallePagoDTO;
import ama.dominio.*;
import ama.dao.DetalleTimbradoDao;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/comprobante")
public class ComprobanteController {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<String> cuerpoSolicitudInvalido(HttpMessageNotReadableException excepcion) {
        log.warn("Solicitud inválida al guardar comprobante: {}", excepcion.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(
                "No se pudieron interpretar los datos. Verifique que la fecha use el formato año-mes-día "
                + "y que los campos numéricos contengan solamente números.");
    }

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
    private NumeradorAutoimpresorService numeradorAutoimpresorService;
    @Autowired
    private AuditoriaComprobanteService auditoriaComprobanteService;

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
        String filtro = paginador.getFiltro() == null ? "" : paginador.getFiltro().trim();
        var servicios = servicioService.buscarPorSucursal(
                pageable, getSucursalSession().getCodigoSucursal(), filtro);

        return servicios;
    }

    @GetMapping("/facturaManual")
    public String facturaManual(Model modelo) {
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
        Servicio servicioRecuperada = servicioService.encontrar(
                servicio.getCuentaCorriente(), getSucursalSession().getCodigoSucursal());

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
        if (comprobanteRequest.getFechaPago() == null) {
            return ResponseEntity.badRequest().body("Seleccione la fecha de pago.");
        }
        if (comprobanteRequest.getCuentaCorriente() == null
                || comprobanteRequest.getCuentaCorriente().isBlank()) {
            return ResponseEntity.badRequest().body("Seleccione una cuenta corriente.");
        }
        Servicio servicioFacturado = servicioService.encontrar(
                comprobanteRequest.getCuentaCorriente().trim(),
                getSucursalSession().getCodigoSucursal());
        if (servicioFacturado == null || servicioFacturado.getSucursal() == null
                || !getSucursalSession().getCodigoSucursal().equals(
                        servicioFacturado.getSucursal().getCodigoSucursal())) {
            return ResponseEntity.badRequest().body("La cuenta seleccionada no pertenece a la sucursal.");
        }
        if (servicioFacturado.getCategoria() == null
                || servicioFacturado.getCategoria().getTarifa() == null
                || servicioFacturado.getCategoria().getTarifa() <= 0) {
            return ResponseEntity.badRequest().body("La cuenta no tiene una categoría con tarifa válida.");
        }
        comprobanteRequest.setCuentaCorriente(servicioFacturado.getCuentaCorriente());
        comprobanteRequest.setCodigoCategoria(
                servicioFacturado.getCategoria().getCodigoCategoria());
        comprobanteRequest.setTarifa(servicioFacturado.getCategoria().getTarifa());
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
        if (comprobanteRequest.getCodigoTimbrado() == null) {
            return ResponseEntity.badRequest().body("Seleccione un timbrado para el punto de expedición.");
        }
        DetalleTimbradoPK detalleTimbradoPK = new DetalleTimbradoPK(
                comprobanteRequest.getCodigoTimbrado(), comprobanteRequest.getCodigoPuntoExpedicion(),
                getSucursalSession().getCodigoSucursal());
        DetalleTimbrado detalleTimbrado = detalleTimbradoDao.findById(detalleTimbradoPK).orElse(null);
        boolean autoimpresor = detalleTimbrado != null && detalleTimbrado.esAutoimpresor();
        TipoComprobante tipoSeleccionado = tipoComprobanteService.encontrar(
                new TipoComprobante(comprobanteRequest.getCodigoTipoComprobante()));
        if (tipoSeleccionado == null) {
            return ResponseEntity.badRequest().body("El tipo de comprobante seleccionado no existe.");
        }
        boolean tipoAutoimpresor = tipoSeleccionado.getNombreTipoComprobante() != null
                && tipoSeleccionado.getNombreTipoComprobante().trim().toUpperCase().contains("AUTOIMPRESOR");
        if (detalleTimbrado != null && tipoAutoimpresor != autoimpresor) {
            return ResponseEntity.badRequest()
                    .body("El timbrado seleccionado no corresponde al tipo de comprobante.");
        }
        Integer serieConfigurada = detalleTimbrado == null || detalleTimbrado.getSerie() == null
                ? 0 : detalleTimbrado.getSerie().getCodigoSerie();
        Integer serieSolicitada = comprobanteRequest.getCodigoSerie() == null
                ? 0 : comprobanteRequest.getCodigoSerie();
        if (detalleTimbrado == null
                || detalleTimbrado.getEstado() == null || detalleTimbrado.getEstado().getCodigoEstado() != 1
                || (!autoimpresor && detalleTimbrado.getSerie() == null)
                || !serieConfigurada.equals(serieSolicitada)) {
            return ResponseEntity.badRequest().body("El timbrado y la serie no están habilitados para el punto de expedición.");
        }
        comprobanteRequest.setCodigoSerie(serieConfigurada);
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
        if (timbradoVigente.getNumeroTimbrado() == null) {
            return ResponseEntity.badRequest().body("El timbrado seleccionado no tiene número fiscal configurado.");
        }
        String establecimientoFiscal = valorFiscal(
                detalleTimbrado.getPuntoExpedicion().getSucursal().getNombreSucursal(),
                puntoExpedicionPK.getCodigoSucursal());
        String puntoExpedicionFiscal = valorFiscal(
                detalleTimbrado.getPuntoExpedicion().getNombrePuntoExpedicion(),
                puntoExpedicionPK.getCodigoPuntoExpedicion());
        String serieFiscal = detalleTimbrado.getSerie() == null
                || detalleTimbrado.getSerie().getCodigoSerie() == 0
                ? null
                : valorFiscal(detalleTimbrado.getSerie().getSerie(),
                        comprobanteRequest.getCodigoSerie());
        if (!autoimpresor && (comprobanteRequest.getNumeroComprobante() == null
                || comprobanteRequest.getNumeroComprobante() < 1
                || comprobanteRequest.getNumeroComprobante() > 9_999_999)) {
            return ResponseEntity.badRequest().body("Ingrese un número válido para el comprobante manual.");
        }
        if (!autoimpresor && !numeroDentroDelRangoDetalle(
                comprobanteRequest.getNumeroComprobante(), detalleTimbrado)) {
            return ResponseEntity.badRequest().body("El número del comprobante está fuera del rango autorizado "
                    + "para el detalle de timbrado (" + detalleTimbrado.getNumeroDesde()
                    + " a " + detalleTimbrado.getNumeroHasta() + ").");
        }
        // En autoimpresor este valor es solamente provisional para validar el objeto.
        // El número fiscal se reserva al final, después de superar todas las validaciones.
        if (autoimpresor && (comprobanteRequest.getNumeroComprobante() == null
                || comprobanteRequest.getNumeroComprobante() < 1)) {
            comprobanteRequest.setNumeroComprobante(1);
        }
        ComprobantePK comprobantePK = ComprobantePK.builder()
                .numeroComprobante(comprobanteRequest.getNumeroComprobante())
                .codigoTipoComprobante(comprobanteRequest.getCodigoTipoComprobante())
                .puntoExpedicionPK(puntoExpedicionPK)
                .codigoSerie(comprobanteRequest.getCodigoSerie())
                .build();
        Optional<Comprobante> comprobanteOP = autoimpresor
                ? Optional.empty() : comprobanteService.findById(comprobantePK);
        if (!autoimpresor && comprobanteOP.isPresent()) {
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
        if (comprobanteRequest.getCantidadPago() == null || comprobanteRequest.getCantidadPago() < 0) {
            return ResponseEntity.badRequest().body("Ingrese una cantidad de pago válida.");
        }
        boolean comprobanteAnulado = comprobanteRequest.getCantidadPago() == 0;
        if (comprobanteAnulado) {
            // Un comprobante sin períodos pagados se registra anulado y no genera movimiento de caja.
            comprobanteRequest.setDetallePago(new ArrayList<>());
            comprobanteRequest.setRecargoPago(0D);
        } else if (comprobanteRequest.getDetallePago() == null || comprobanteRequest.getDetallePago().isEmpty()) {
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
                .cuentaCorriente(servicioFacturado.getCuentaCorriente())
                .establecimientoFiscal(establecimientoFiscal)
                .puntoExpedicionFiscal(puntoExpedicionFiscal)
                .numeroTimbradoFiscal(String.valueOf(timbradoVigente.getNumeroTimbrado()))
                .inicioVigenciaFiscal(inicioVigencia)
                .finVigenciaFiscal(finVigencia)
                .serieFiscal(serieFiscal)
                .servicio(servicioFacturado)
                .usuario(new Usuario(comprobanteRequest.getCodigoUsuario()))
                .estado(
                        comprobanteAnulado ? new Estado(3) : new Estado(1)
                )
                .timbrado(new Timbrado(comprobanteRequest.getCodigoTimbrado()))
                .usuarioSistema(getUserSession())
                .detallePago(comprobanteRequest.getDetallePago())
                .build();
        if (validarComprobate(comprobante).getStatusCode() != HttpStatus.OK) {
            return validarComprobate(comprobante);
        }
        if (autoimpresor) {
            try {
                int numeroFiscal = numeradorAutoimpresorService.reservar(
                        detalleTimbrado, comprobanteRequest.getCodigoTipoComprobante());
                comprobanteRequest.setNumeroComprobante(numeroFiscal);
                comprobantePK.setNumeroComprobante(numeroFiscal);
                comprobante.getDetallePago().forEach(pago ->
                        pago.getDetallePagoPK().setNumeroComprobante(numeroFiscal));
            } catch (IllegalArgumentException | IllegalStateException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
            }
        }
        try {
            comprobanteService.guardar(comprobante);
            auditoriaComprobanteService.registrar("EMISION", comprobantePK,
                    getUserSession().getCodigoUsuarioSistema(),
                    autoimpresor ? "Emisión autoimpresor" : "Emisión manual");
            return ResponseEntity.ok("Comprobante guardado correctamente. N.º "
                    + String.format("%07d", comprobantePK.getNumeroComprobante()));
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
    @Transactional
    public ResponseEntity<?> moficicarComprobante(@RequestBody Comprobante comprobanteReques) {
        Comprobante comprobante = comprobanteService.getComprobante(comprobanteReques.getComprobantePK());
        if (comprobante == null) {
            return ResponseEntity.notFound().build();
        }
        if (esAutoimpresor(comprobante)) {
            auditoriaComprobanteService.registrar("INTENTO_MODIFICACION", comprobante.getComprobantePK(),
                    getUserSession().getCodigoUsuarioSistema(), "Edición rechazada por inalterabilidad fiscal");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    "Un comprobante autoimpreso emitido es inalterable. Utilice anulación o nota de crédito/débito.");
        }
        if (comprobanteReques.getEstado() != null
                && comprobanteReques.getEstado().getCodigoEstado() == 3
                && comprobante.getEstado().getCodigoEstado() != 3) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    "Utilice la acción Anular para revertir también la fecha desde de la cuenta");
        }
        String datosAnteriores = auditoriaComprobanteService.capturarFoto(
                comprobante.getComprobantePK());
        comprobante.setCobrador(comprobanteReques.getCobrador());
        comprobante.setFechaPago(comprobanteReques.getFechaPago());
        comprobante.setEstado(comprobanteReques.getEstado());
        comprobante.setObs(comprobanteReques.getObs());
        comprobanteService.guardar(comprobante);
        auditoriaComprobanteService.registrarModificacion(
                comprobante.getComprobantePK(),
                getUserSession().getCodigoUsuarioSistema(),
                "Modificación desde la gestión de comprobantes",
                datosAnteriores);
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
                .getUltimoComprobanteCuentaActivo(comprobante.getServicio().getCuentaCorriente(),
                        comprobante.getComprobantePK().getPuntoExpedicionPK().getCodigoSucursal());

        if (!(comprobante.getComprobantePK().getNumeroComprobante() == ultimoComprobanteCuentaActivo.get()
                .getComprobantePK().getNumeroComprobante())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    "Solo se puede anular el ultimo comprobante activo de la cuenta corriente");
        }

        comprobante.setEstado(new Estado(3));
        comprobante.setObs(comprobanteRequest.getObs());

        comprobanteService.anular(comprobante);
        auditoriaComprobanteService.registrar("ANULACION", comprobante.getComprobantePK(),
                getUserSession().getCodigoUsuarioSistema(), comprobanteRequest.getObs());
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

    private boolean esAutoimpresor(Comprobante comprobante) {
        ComprobantePK clave = comprobante.getComprobantePK();
        DetalleTimbradoPK detalleId = new DetalleTimbradoPK(
                comprobante.getTimbrado().getCodigoTimbrado(),
                clave.getPuntoExpedicionPK().getCodigoPuntoExpedicion(),
                clave.getPuntoExpedicionPK().getCodigoSucursal());
        return detalleTimbradoDao.findById(detalleId).map(DetalleTimbrado::esAutoimpresor).orElse(false);
    }

    private boolean numeroDentroDelRangoDetalle(Integer numeroComprobante, DetalleTimbrado detalleTimbrado) {
        int desde = detalleTimbrado.getNumeroDesde() == null ? 1 : detalleTimbrado.getNumeroDesde();
        int hasta = detalleTimbrado.getNumeroHasta() == null ? 9_999_999 : detalleTimbrado.getNumeroHasta();
        return numeroComprobante >= desde && numeroComprobante <= hasta;
    }

    private String valorFiscal(String valorConfigurado, Integer codigoInterno) {
        if (valorConfigurado != null && !valorConfigurado.isBlank()) {
            return valorConfigurado.trim();
        }
        return codigoInterno == null ? null : String.format("%03d", codigoInterno);
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal getSucursalSession() {
        return getUserSession().getSucursal();
    }

    private EstadoCuenta getEstadoCuenta(String cuentaCorriente) {

        Servicio servicio = servicioService.encontrar(cuentaCorriente, getSucursalSession().getCodigoSucursal());
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
