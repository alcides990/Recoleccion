package ama.modulos.facturacionmovil;

import ama.dao.ServicioDao;
import ama.dominio.EstadoCuenta;
import ama.dominio.Servicio;
import ama.dominio.UsuarioSistema;
import ama.servicio.CobradorService;
import ama.servicio.ComisionService;
import ama.servicio.ComprobanteService;
import ama.servicio.CondicionVentaService;
import ama.servicio.MetodoPagoService;
import ama.servicio.ParametroService;
import ama.servicio.PuntoExpedicionService;
import ama.servicio.TipoComprobanteService;
import ama.servicio.SerieService;
import ama.servicio.TimbradoService;
import ama.servicio.UbicacionServicioService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ama.dominio.Parametro;
import ama.dominio.ComprobanteGuardar;
import ama.dominio.ComprobantePK;
import ama.dominio.DetallePago;
import ama.dominio.DetallePagoPK;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.TipoComprobante;
import ama.controladorMVC.ComprobanteController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movil/facturacion")
public class FacturacionMovilController {

    private final CobradorService cobradores;
    private final ComisionService comisiones;
    private final CondicionVentaService condiciones;
    private final MetodoPagoService metodos;
    private final PuntoExpedicionService puntos;
    private final TipoComprobanteService tipos;
    private final SerieService series;
    private final TimbradoService timbrados;
    private final ServicioDao servicios;
    private final ComprobanteService comprobantes;
    private final ParametroService parametros;
    private final DetalleTimbradoMovilRepository detallesTimbrado;
    private final HttpSession session;
    private final ComprobanteController comprobanteController;
    private final ComprobanteMovilRepository comprobantesMovil;
    private final UbicacionServicioService ubicaciones;
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MM-yyyy");

    public FacturacionMovilController(CobradorService cobradores, ComisionService comisiones,
            CondicionVentaService condiciones, MetodoPagoService metodos,
            PuntoExpedicionService puntos, TipoComprobanteService tipos, SerieService series,
            TimbradoService timbrados, ServicioDao servicios,
            ComprobanteService comprobantes, ParametroService parametros,
            DetalleTimbradoMovilRepository detallesTimbrado, HttpSession session,
            ComprobanteController comprobanteController,
            ComprobanteMovilRepository comprobantesMovil,
            UbicacionServicioService ubicaciones) {
        this.cobradores = cobradores;
        this.comisiones = comisiones;
        this.condiciones = condiciones;
        this.metodos = metodos;
        this.puntos = puntos;
        this.tipos = tipos;
        this.series = series;
        this.timbrados = timbrados;
        this.servicios = servicios;
        this.comprobantes = comprobantes;
        this.parametros = parametros;
        this.detallesTimbrado = detallesTimbrado;
        this.session = session;
        this.comprobanteController = comprobanteController;
        this.comprobantesMovil = comprobantesMovil;
        this.ubicaciones = ubicaciones;
    }

    @GetMapping("/catalogos")
    @Transactional(readOnly = true)
    public ResponseEntity<?> catalogos(CsrfToken csrf, Authentication authentication) {
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuario == null || usuario.getSucursal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Sesión vencida."));
        }
        var sucursal = usuario.getSucursal();
        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("csrf", csrf.getToken());
        salida.put("roles", authentication.getAuthorities().stream()
                .map(x -> x.getAuthority()).sorted().toList());
        salida.put("sucursal", Map.of("codigo", sucursal.getCodigoSucursal(), "nombre", sucursal.getNombreSucursal()));
        salida.put("cobradores", cobradores.listarIsEstadoActivo(sucursal).stream()
                .map(x -> Map.of("codigo", x.getCodigoCobrador(), "nombre", x.getNombreCompleto())).toList());
        salida.put("comisiones", comisiones.listar().stream()
                .map(x -> Map.of("codigo", x.getCodigoComision(), "nombre", x.getComision())).toList());
        salida.put("condiciones", condiciones.listar().stream()
                .map(x -> Map.of("codigo", x.getCodigoCondicionVenta(), "nombre", x.getCondicionVenta())).toList());
        salida.put("metodosPago", metodos.listar().stream()
                .map(x -> Map.of("codigo", x.getCodigoMetodoPago(), "nombre", x.getMetodoPago())).toList());
        salida.put("puntos", puntos.isMayorCero(sucursal).stream()
                .map(x -> Map.of("codigo", x.getPuntoExpedicionPK().getCodigoPuntoExpedicion(),
                "nombre", x.getNombrePuntoExpedicion())).toList());
        salida.put("tipos", tipos.listar().stream()
                .map(x -> Map.of("codigo", x.getCodigoTipoComprobante(), "nombre", x.getNombreTipoComprobante())).toList());
        salida.put("series", series.listar().stream()
                .map(x -> Map.of("codigo", x.getCodigoSerie(), "nombre", x.getSerie())).toList());
        salida.put("timbrados", timbrados.listar().stream()
                .filter(x -> x.getEstado() != null && x.getEstado().getCodigoEstado() == 1)
                .filter(x -> x.getEmpresa() != null && sucursal.getEmpresa() != null
                && x.getEmpresa().getCodigoEmpresa().equals(sucursal.getEmpresa().getCodigoEmpresa()))
                .map(x -> Map.of("codigo", x.getCodigoTimbrado(), "nombre", String.valueOf(x.getNumeroTimbrado()))).toList());
        return ResponseEntity.ok(salida);
    }

    @GetMapping("/servicios")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> buscarServicios(@RequestParam String tipo,
            @RequestParam String valor) {
        UsuarioSistema usuario = usuarioSession();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Sesión vencida."));
        }
        String filtro = valor == null ? "" : valor.trim();
        if (filtro.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Ingrese un valor para buscar."));
        }
        Integer sucursal = usuario.getSucursal().getCodigoSucursal();
        List<Servicio> encontrados;
        if ("manzana".equalsIgnoreCase(tipo)) {
            try {
                encontrados = servicios.buscarActivosPorManzana(sucursal,
                        Integer.valueOf(filtro), PageRequest.of(0, 100));
            } catch (NumberFormatException ex) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "La manzana debe ser numérica."));
            }
        } else if ("nombre".equalsIgnoreCase(tipo)) {
            encontrados = servicios.buscarActivosPorNombre(sucursal, filtro, PageRequest.of(0, 100));
        } else if ("cuenta".equalsIgnoreCase(tipo)) {
            encontrados = servicios.buscarPorSucursal(PageRequest.of(0, 20), sucursal, filtro)
                    .getContent().stream()
                    .filter(servicio -> servicio.getEstado() != null
                    && servicio.getEstado().getCodigoEstado() == 1)
                    .toList();
        } else {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Tipo de búsqueda inválido."));
        }

        List<String> cuentas = encontrados.stream().map(Servicio::getCuentaCorriente).toList();
        Map<String, Object[]> resumenPorCuenta = new LinkedHashMap<>();
        if (!cuentas.isEmpty()) {
            for (Object[] resumen : servicios.resumirEstadosMovil(
                    cuentas, usuario.getSucursal().getCodigoSucursal())) {
                resumenPorCuenta.put(String.valueOf(resumen[0]), resumen);
            }
        }
        Parametro parametro = parametros.encontrar(usuario.getSucursal());
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Servicio servicio : encontrados) {
            Object[] resumen = resumenPorCuenta.get(servicio.getCuentaCorriente());
            LocalDate pagoDesde = servicio.getFechaInicio();
            double saldo = 0;
            if (resumen != null) {
                if (resumen[1] != null) {
                    pagoDesde = YearMonth.parse(String.valueOf(resumen[1]), MES_ANO).atDay(1);
                }
                if (resumen[2] instanceof Number) {
                    saldo = ((Number) resumen[2]).doubleValue();
                }
            }
            EstadoCuenta estado = new EstadoCuenta(servicio.getCategoria().getTarifa(), parametro, pagoDesde);
            estado.setSaldoAnterior(saldo);
            if (resumen != null && resumen[3] instanceof Number) {
                estado.setCantidadDeuda(((Number) resumen[3]).intValue());
            }
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("cuenta", servicio.getCuentaCorriente());
            fila.put("nombre", (servicio.getUsuario().getNombre() + " "
                    + (servicio.getUsuario().getApellido() == null ? "" : servicio.getUsuario().getApellido())).trim());
            fila.put("documento", servicio.getUsuario().getNumeroDocumento());
            fila.put("manzana", servicio.getManzana().getManzanaPK().getNumeroManzana());
            fila.put("categoria", servicio.getCategoria().getNombreCategoria());
            fila.put("tarifa", servicio.getCategoria().getTarifa());
            fila.put("pagoDesde", estado.getPagoHasta() == null ? "" : estado.getPagoHasta().format(MES_ANO));
            fila.put("cantidadDeuda", estado.getCantidadDeuda());
            fila.put("saldoAnterior", estado.getSaldoAnterior());
            fila.put("totalDeuda", estado.getTotalDeuda());
            filas.add(fila);
        }
        return ResponseEntity.ok(filas);
    }

    @GetMapping("/detalles-timbrado")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR','SECRETARIO')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detallesTimbrado(@RequestParam Integer codigoPuntoExpedicion,
            @RequestParam Integer codigoTipoComprobante) {
        UsuarioSistema usuario = usuarioSession();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Sesión vencida."));
        }
        TipoComprobante tipo = tipos.encontrar(new TipoComprobante(codigoTipoComprobante));
        if (tipo == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El tipo de comprobante no existe."));
        }
        String modoEmision = esAutoimpresor(tipo) ? "AUTOIMPRESOR" : "MANUAL";
        var filas = detallesTimbrado.buscarActivos(
                usuario.getSucursal().getCodigoSucursal(), codigoPuntoExpedicion, modoEmision);
        return ResponseEntity.ok(filas.stream().map(x -> {
            String serie = String.valueOf(x[3]);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("codigoTimbrado", ((Number) x[0]).intValue());
            fila.put("numeroTimbrado", String.valueOf(x[1]));
            fila.put("codigoSerie", ((Number) x[2]).intValue());
            fila.put("serie", serie);
            fila.put("modoEmision", String.valueOf(x[4]));
            fila.put("numeroDesde", ((Number) x[5]).intValue());
            fila.put("numeroHasta", ((Number) x[6]).intValue());
            fila.put("nombre", "Timbrado " + x[1]
                    + (serie.isBlank() ? " · Sin serie" : " · Serie " + serie)
                    + ("AUTOIMPRESOR".equals(String.valueOf(x[4]))
                            ? " · Autoimpresor" : " · Manual"));
            return fila;
        }).toList());
    }

    private boolean esAutoimpresor(TipoComprobante tipo) {
        return tipo.getNombreTipoComprobante() != null
                && tipo.getNombreTipoComprobante().trim().toUpperCase().contains("AUTOIMPRESOR");
    }

    @GetMapping("/numero-siguiente")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> numeroSiguiente(@RequestParam Integer codigoPuntoExpedicion,
            @RequestParam Integer codigoTipoComprobante, @RequestParam Integer codigoSerie) {
        UsuarioSistema usuario = usuarioSession();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Sesión vencida."));
        PuntoExpedicionPK punto = PuntoExpedicionPK.builder()
                .codigoSucursal(usuario.getSucursal().getCodigoSucursal())
                .codigoPuntoExpedicion(codigoPuntoExpedicion).build();
        ComprobantePK clave = ComprobantePK.builder().puntoExpedicionPK(punto)
                .codigoTipoComprobante(codigoTipoComprobante).codigoSerie(codigoSerie).build();
        return ResponseEntity.ok(Map.of("numeroComprobante", comprobantes.getNumeroComprobante(clave)));
    }

    @GetMapping("/estado-cuenta")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR','SECRETARIO')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> estadoCuenta(@RequestParam String cuentaCorriente) {
        UsuarioSistema usuario = usuarioSession();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Sesión vencida."));
        }
        Servicio servicio = servicios.encontrar(cuentaCorriente == null ? "" : cuentaCorriente.trim());
        if (servicio == null || servicio.getSucursal() == null
                || !servicio.getSucursal().getCodigoSucursal().equals(usuario.getSucursal().getCodigoSucursal())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Cuenta no encontrada en su sucursal."));
        }
        EstadoCuenta estado = calcularEstado(servicio, usuario);
        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("cuenta", servicio.getCuentaCorriente());
        salida.put("nombre", (servicio.getUsuario().getNombre() + " "
                + (servicio.getUsuario().getApellido() == null ? "" : servicio.getUsuario().getApellido())).trim());
        salida.put("documento", servicio.getUsuario().getNumeroDocumento());
        salida.put("categoria", servicio.getCategoria().getNombreCategoria());
        salida.put("tarifa", estado.getTarifa());
        salida.put("pagoDesde", estado.getPagoHasta() == null ? "" : estado.getPagoHasta().format(MES_ANO));
        salida.put("cantidadDeuda", estado.getCantidadDeuda());
        salida.put("saldoAnterior", estado.getSaldoAnterior());
        salida.put("recargo", estado.getRecargo());
        salida.put("totalDeuda", estado.getTotalDeuda());
        return ResponseEntity.ok(salida);
    }

    @GetMapping("/ubicacion")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> consultarUbicacion(@RequestParam String cuentaCorriente) {
        UsuarioSistema usuario = usuarioSession();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Sesión vencida."));
        }
        Servicio servicio = servicioDeSucursal(cuentaCorriente, usuario);
        if (servicio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "Cuenta no encontrada en su sucursal."));
        }
        return ResponseEntity.ok(ubicaciones.consultar(servicio.getCuentaCorriente()));
    }

    @PostMapping("/ubicacion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> guardarUbicacion(@RequestBody UbicacionMovilRequest entrada) {
        UsuarioSistema usuario = usuarioSession();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Sesión vencida."));
        }
        if (entrada == null || entrada.cuentaCorriente() == null
                || entrada.cuentaCorriente().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "Seleccione una cuenta corriente."));
        }
        Servicio servicio = servicioDeSucursal(entrada.cuentaCorriente(), usuario);
        if (servicio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "Cuenta no encontrada en su sucursal."));
        }
        try {
            return ResponseEntity.ok(ubicaciones.guardar(
                    servicio.getCuentaCorriente(), entrada.latitud(), entrada.longitud(),
                    entrada.precisionMetros(), entrada.metodo(),
                    usuario.getCodigoUsuarioSistema(), "APP"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", ex.getMessage()));
        }
    }

    @PostMapping("/emitir")
    @PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR','SUPERVISOR','SECRETARIO')")
    @Transactional
    public ResponseEntity<?> emitir(@RequestBody EmitirFacturaMovilRequest entrada) {
        UsuarioSistema usuario = usuarioSession();
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Sesión vencida."));
        }
        if (entrada.getCuentaCorriente() == null || entrada.getCuentaCorriente().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Seleccione una cuenta corriente."));
        }
        Servicio servicio = servicios.encontrar(entrada.getCuentaCorriente());
        if (servicio == null || servicio.getSucursal() == null
                || !servicio.getSucursal().getCodigoSucursal().equals(usuario.getSucursal().getCodigoSucursal())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Servicio no encontrado en su sucursal."));
        }
        if (entrada.getCantidadPago() == null || entrada.getCantidadPago() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La cantidad de meses debe ser mayor que cero."));
        }
        if (entrada.getCodigoPuntoExpedicion() == null || entrada.getCodigoTimbrado() == null
                || entrada.getCodigoSerie() == null || entrada.getCodigoTipoComprobante() == null
                || entrada.getCodigoCobrador() == null || entrada.getCodigoComision() == null
                || entrada.getCodigoCondicionVenta() == null
                || ((entrada.getPagos() == null || entrada.getPagos().isEmpty()) && entrada.getCodigoMetodoPago() == null)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Complete todos los datos de facturación."));
        }

        EstadoCuenta estado = calcularEstado(servicio, usuario);
        // Una cuenta al día también puede abonar períodos por adelantado. La fecha
        // base ya representa el próximo período a pagar y CalcularPago la avanza
        // según cantidadPago. Nunca persistimos una cantidad de deuda negativa.
        int deuda = Math.max(0, estado.getCantidadDeuda());

        PuntoExpedicionPK punto = PuntoExpedicionPK.builder()
                .codigoSucursal(usuario.getSucursal().getCodigoSucursal())
                .codigoPuntoExpedicion(entrada.getCodigoPuntoExpedicion()).build();
        ComprobantePK clave = ComprobantePK.builder().puntoExpedicionPK(punto)
                .codigoTipoComprobante(entrada.getCodigoTipoComprobante())
                .codigoSerie(entrada.getCodigoSerie()).build();
        var tipoSeleccionado = tipos.listar().stream()
                .filter(x -> x.getCodigoTipoComprobante().equals(entrada.getCodigoTipoComprobante()))
                .findFirst().orElse(null);
        if (tipoSeleccionado == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El tipo de comprobante seleccionado no existe."));
        }
        boolean facturaManual = "FACTURA MANUAL".equalsIgnoreCase(tipoSeleccionado.getNombreTipoComprobante().trim());
        Integer numero = facturaManual ? entrada.getNumeroComprobante() : comprobantes.getNumeroComprobante(clave);
        if (numero == null || numero <= 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Ingrese un número válido para la factura manual."));
        }

        double recargo = entrada.getRecargo() == null ? estado.getRecargo() : entrada.getRecargo();
        if (recargo < 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El recargo no puede ser negativo."));
        }
        double total = entrada.getCantidadPago() * estado.getTarifa() + recargo - estado.getSaldoAnterior();
        // Todo cobro de al menos un período debe cubrir como mínimo una tarifa.
        total = Math.max(estado.getTarifa(), total);
        if (total <= 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El total calculado no es válido."));
        }
        Map<Integer, Double> importesPorMetodo = new LinkedHashMap<>();
        if (entrada.getPagos() != null) {
            for (EmitirFacturaMovilRequest.PagoMovilRequest pago : entrada.getPagos()) {
                if (pago.getCodigoMetodoPago() == null || pago.getImporte() == null || pago.getImporte() <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("mensaje", "Existe un medio de pago incompleto o inválido."));
                }
                importesPorMetodo.merge(pago.getCodigoMetodoPago(), pago.getImporte(), Double::sum);
            }
        }
        if (importesPorMetodo.isEmpty()) importesPorMetodo.put(entrada.getCodigoMetodoPago(), total);
        double totalCobrado = importesPorMetodo.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalCobrado + 0.001 < total) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La suma de los medios de pago no cubre el monto a pagar."));
        }
        List<DetallePago> pagos = importesPorMetodo.entrySet().stream().map(x -> DetallePago.builder()
                .detallePagoPK(DetallePagoPK.builder().codigoMetodoPago(x.getKey()).build())
                .importe(x.getValue()).build()).toList();
        String razonSocial = (servicio.getUsuario().getNombre() + " "
                + (servicio.getUsuario().getApellido() == null ? "" : servicio.getUsuario().getApellido())).trim();
        ComprobanteGuardar comprobante = ComprobanteGuardar.builder()
                .cuentaCorriente(servicio.getCuentaCorriente()).razonSocial(razonSocial)
                .codigoUsuario(servicio.getUsuario().getCodigoUsuario())
                .codigoSerie(entrada.getCodigoSerie()).codigoTimbrado(entrada.getCodigoTimbrado())
                .numeroComprobante(numero).codigoPuntoExpedicion(entrada.getCodigoPuntoExpedicion())
                .codigoTipoComprobante(entrada.getCodigoTipoComprobante())
                .codigoCobrador(entrada.getCodigoCobrador()).codigoComision(entrada.getCodigoComision())
                .codigoCondicionVenta(entrada.getCodigoCondicionVenta()).fechaPago(LocalDate.now())
                .pagoHasta(estado.getPagoHasta()).cantidadDeuda(deuda).cantidadPago(entrada.getCantidadPago())
                .codigoCategoria(servicio.getCategoria().getCodigoCategoria()).tarifa(estado.getTarifa())
                .recargoPago(recargo).saldoAnterior(estado.getSaldoAnterior()).totalImporte(totalCobrado)
                .detallePago(pagos).build();

        ResponseEntity<?> guardado = comprobanteController.guardarComprobante(comprobante);
        if (!guardado.getStatusCode().is2xxSuccessful()) return guardado;
        return ResponseEntity.ok(Map.of(
                "mensaje", "Comprobante guardado correctamente.",
                "numeroComprobante", comprobante.getNumeroComprobante(),
                "codigoSucursal", usuario.getSucursal().getCodigoSucursal(),
                "codigoPuntoExpedicion", entrada.getCodigoPuntoExpedicion(),
                "codigoTipoComprobante", entrada.getCodigoTipoComprobante(),
                "codigoSerie", entrada.getCodigoSerie(),
                "total", total,
                "totalCobrado", totalCobrado,
                "saldoFavor", Math.max(0, totalCobrado - total),
                "cuentaCorriente", servicio.getCuentaCorriente()));
    }

    @GetMapping("/mis-comprobantes")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<?> misComprobantes() {
        UsuarioSistema usuario = usuarioSession();
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Sesión vencida."));
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Object[] x : comprobantesMovil.ultimosDelUsuario(usuario.getCodigoUsuarioSistema(),
                usuario.getSucursal().getCodigoSucursal())) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("numero", x[0]); fila.put("fecha", x[1]); fila.put("cuenta", x[2]);
            fila.put("cliente", x[3]); fila.put("totalCobrado", x[4]); fila.put("saldoFavor", x[5]);
            fila.put("punto", x[6]); fila.put("serie", x[7]); fila.put("tipo", x[8]);
            fila.put("periodo", x[9] == null ? "" : x[9]); fila.put("cantidadPago", x[10]);
            fila.put("anulado", x[11] != null && ((Number) x[11]).intValue() == 3);
            fila.put("codigoPunto", x[12]); fila.put("codigoSerie", x[13]);
            fila.put("codigoSucursal", usuario.getSucursal().getCodigoSucursal());
            salida.add(fila);
        }
        return ResponseEntity.ok(salida);
    }

    private EstadoCuenta calcularEstado(Servicio servicio, UsuarioSistema usuario) {
        List<Object[]> datos = comprobantes.getEstadoCuentaMovil(servicio.getCuentaCorriente(),
                usuario.getSucursal().getCodigoSucursal());
        LocalDate pagoDesde = servicio.getFechaInicio();
        double saldo = 0;
        for (Object[] fila : datos) {
            if (fila[0] != null) pagoDesde = YearMonth.parse(String.valueOf(fila[0]), MES_ANO).atDay(1);
            if (fila[1] instanceof Number) saldo = ((Number) fila[1]).doubleValue();
        }
        EstadoCuenta estado = new EstadoCuenta(servicio.getCategoria().getTarifa(),
                parametros.encontrar(usuario.getSucursal()), pagoDesde);
        estado.setSaldoAnterior(saldo);
        if (!datos.isEmpty() && datos.get(0)[2] instanceof Number) {
            estado.setCantidadDeuda(((Number) datos.get(0)[2]).intValue());
        }
        // Fuerza el cálculo antes de consultar recargo/subtotales.
        estado.getCantidadDeuda();
        return estado;
    }

    private UsuarioSistema usuarioSession() {
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        return usuario == null || usuario.getSucursal() == null ? null : usuario;
    }

    private Servicio servicioDeSucursal(String cuentaCorriente, UsuarioSistema usuario) {
        Servicio servicio = servicios.encontrar(
                cuentaCorriente == null ? "" : cuentaCorriente.trim());
        return servicio != null && servicio.getSucursal() != null
                && servicio.getSucursal().getCodigoSucursal().equals(
                        usuario.getSucursal().getCodigoSucursal()) ? servicio : null;
    }

    public record UbicacionMovilRequest(
            String cuentaCorriente,
            BigDecimal latitud,
            BigDecimal longitud,
            BigDecimal precisionMetros,
            String metodo) {}

}
