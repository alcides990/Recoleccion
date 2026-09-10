package ama.modulos.comprobantesv2;

import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import ama.dominio.Cobrador;
import ama.dominio.Categoria;
import ama.dominio.CondicionVenta;
import ama.dominio.Estado;
import ama.dominio.DetallePago;
import ama.dominio.DetallePagoPK;
import ama.dominio.MetodoPago;
import ama.dominio.Servicio;
import ama.dao.CategoriaDao;
import ama.dao.CondicionVentaDao;
import ama.dao.MetodoPagoDao;
import ama.dao.DetalleTimbradoDao;
import ama.dao.UsuarioDao;
import ama.dominio.DetalleTimbradoPK;
import ama.servicio.AuditoriaComprobanteService;
import ama.servicio.ServicioService;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.UsuarioSistema;
import ama.dominio.Usuario;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.io.InputStream;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/comprobantes-v2")
public class ComprobanteAccionControllerV2 {

    private static final Logger LOG = LoggerFactory.getLogger(ComprobanteAccionControllerV2.class);

    private final ComprobanteJpaRepositoryV2 comprobantes;
    private final CobradorJpaRepositoryV2 cobradores;
    private final EstadoJpaRepositoryV2 estados;
    private final CategoriaDao categorias;
    private final CondicionVentaDao condicionesVenta;
    private final MetodoPagoDao metodosPago;
    private final DetallePagoJpaRepositoryV2 detallesPago;
    private final HttpSession session;
    private final DataSource dataSource;
    private final MontoEnLetrasService montoEnLetras;
    private final DetalleTimbradoDao detallesTimbrado;
    private final AuditoriaComprobanteService auditoria;
    private final ServicioService servicios;
    private final UsuarioDao usuarios;

    public ComprobanteAccionControllerV2(ComprobanteJpaRepositoryV2 comprobantes,
            CobradorJpaRepositoryV2 cobradores, EstadoJpaRepositoryV2 estados,
            CategoriaDao categorias, CondicionVentaDao condicionesVenta, MetodoPagoDao metodosPago,
            DetallePagoJpaRepositoryV2 detallesPago, HttpSession session, DataSource dataSource,
            MontoEnLetrasService montoEnLetras, DetalleTimbradoDao detallesTimbrado,
            AuditoriaComprobanteService auditoria, ServicioService servicios, UsuarioDao usuarios) {
        this.comprobantes = comprobantes;
        this.cobradores = cobradores;
        this.estados = estados;
        this.categorias = categorias;
        this.condicionesVenta = condicionesVenta;
        this.metodosPago = metodosPago;
        this.detallesPago = detallesPago;
        this.session = session;
        this.dataSource = dataSource;
        this.montoEnLetras = montoEnLetras;
        this.detallesTimbrado = detallesTimbrado;
        this.auditoria = auditoria;
        this.servicios = servicios;
        this.usuarios = usuarios;
    }

    @GetMapping("/cuenta-edicion")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ROOT','SUPERVISOR')")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> cuentaEdicion(@RequestParam String cuentaCorriente) {
        UsuarioSistema usuarioSesion = usuarioSesion();
        Servicio servicio = buscarServicioEdicion(cuentaCorriente, usuarioSesion);
        var respuesta = new HashMap<String, Object>();
        respuesta.put("cuentaCorriente", servicio.getCuentaCorriente());
        respuesta.put("codigoUsuario", servicio.getUsuario().getCodigoUsuario());
        respuesta.put("documento", servicio.getUsuario().getNumeroDocumento());
        respuesta.put("razonSocial", nombreCompleto(servicio));
        respuesta.put("codigoCategoria", servicio.getCategoria().getCodigoCategoria());
        respuesta.put("tarifa", servicio.getCategoria().getTarifa());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/detalle")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<ComprobanteDetalleV2> detalle(@RequestBody @Valid ComprobanteAccionV2 solicitud) {
        return ResponseEntity.ok(aDetalle(buscarAutorizado(solicitud)));
    }

    @GetMapping("/ticket")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> ticket(ComprobanteAccionV2 solicitud,
            @RequestParam(name = "formato", defaultValue = "58mm") String formato) {
        try {
            Comprobante comprobante = buscarAutorizado(solicitud);
            String rutaReporte = switch (formato.toLowerCase()) {
                case "58mm" -> "reportes/comprobanteTicket.jasper";
                case "80mm" -> "reportes/comprobanteTicket80mm.jasper";
                case "a4" -> "reportes/comprobanteA4.jasper";
                default -> null;
            };
            if (rutaReporte == null) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "Formato de comprobante no válido."));
            }
            var parametros = new HashMap<String, Object>();
            parametros.put("codigoSucursal", solicitud.getCodigoSucursal());
            parametros.put("codigoPuntoExpedicion", solicitud.getCodigoPuntoExpedicion());
            parametros.put("codigoTipoComprobante", solicitud.getCodigoTipoComprobante());
            parametros.put("codigoSerie", solicitud.getCodigoSerie());
            parametros.put("numeroComprobante", solicitud.getNumeroComprobante());
            parametros.put("montoLetras", montoEnLetras.convertir(BigDecimal.valueOf(comprobante.getTotalImporte())));
            boolean primeraImpresion = auditoria.esPrimeraImpresion(comprobante.getComprobantePK());
            parametros.put("ejemplar", primeraImpresion ? "ORIGINAL" : "COPIA - REIMPRESIÓN");
            try (InputStream jasper = new ClassPathResource(rutaReporte).getInputStream();
                    var conexion = dataSource.getConnection()) {
                JasperReport reporte = (JasperReport) JRLoader.loadObject(jasper);
                var impresion = JasperFillManager.fillReport(reporte, parametros, conexion);
                byte[] pdf = JasperExportManager.exportReportToPdf(impresion);
                auditoria.registrarImpresion(comprobante.getComprobantePK(),
                        usuarioSesion().getCodigoUsuarioSistema(), formato.toLowerCase());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentDisposition(ContentDisposition.inline()
                        .filename("comprobante-" + formato.toLowerCase() + "-"
                                + solicitud.getNumeroComprobante() + ".pdf").build());
                return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
                        .contentLength(pdf.length).body(pdf);
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("No fue posible generar el ticket {}-{}-{}-{}-{}",
                    solicitud.getCodigoSucursal(), solicitud.getCodigoPuntoExpedicion(),
                    solicitud.getCodigoTipoComprobante(), solicitud.getCodigoSerie(),
                    solicitud.getNumeroComprobante(), ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "mensaje", "No fue posible generar el ticket: " + mensajeError(ex)));
        }
    }

    @PutMapping("/editar")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ROOT','SUPERVISOR')")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> editar(@RequestBody @Valid ComprobanteAccionV2 solicitud) {
        if (solicitud.getFechaPago() == null || solicitud.getCodigoEstado() == null
                || solicitud.getCodigoCobrador() == null || solicitud.getCodigoCategoria() == null
                || solicitud.getCodigoCondicionVenta() == null || solicitud.getCantidadPago() == null
                || solicitud.getRecargo() == null
                || solicitud.getTotalImporte() == null || solicitud.getSaldo() == null
                || solicitud.getPagos() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Complete todos los campos obligatorios del comprobante."));
        }
        UsuarioSistema usuario = usuarioSesion();
        Comprobante comprobante = buscarAutorizado(solicitud);
        ComprobantePK claveAnterior = comprobante.getComprobantePK();
        String cuentaAnterior = comprobante.getServicio().getCuentaCorriente();
        String cuentaSolicitada = limpiar(solicitud.getCuentaCorriente());
        if (cuentaSolicitada == null) {
            cuentaSolicitada = cuentaAnterior;
        }
        boolean cambiaCuenta = !Objects.equals(cuentaAnterior, cuentaSolicitada);
        if (cambiaCuenta && !esFacturaManual(comprobante)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "La cuenta corriente solo puede modificarse cuando el tipo de comprobante es FACTURA MANUAL."));
        }
        Servicio servicioDestino = cambiaCuenta ? buscarServicioEdicion(cuentaSolicitada, usuario) : comprobante.getServicio();
        Integer numeroNuevo = solicitud.getNuevoNumeroComprobante() == null
                ? claveAnterior.getNumeroComprobante() : solicitud.getNuevoNumeroComprobante();
        boolean cambiaNumero = !Objects.equals(claveAnterior.getNumeroComprobante(), numeroNuevo);
        if (cambiaNumero && !esFacturaManual(comprobante)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "El número solo puede modificarse cuando el tipo de comprobante es FACTURA MANUAL."));
        }
        ComprobantePK claveNueva = new ComprobantePK(numeroNuevo,
                claveAnterior.getPuntoExpedicionPK(), claveAnterior.getCodigoTipoComprobante(),
                claveAnterior.getCodigoSerie());
        if (cambiaNumero && comprobantes.existsById(claveNueva)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "Ya existe un comprobante con el nuevo número para la misma sucursal, punto, tipo y serie."));
        }
        if (esAutoimpresor(comprobante)) {
            auditoria.registrar("INTENTO_MODIFICACION", comprobante.getComprobantePK(),
                    usuario.getCodigoUsuarioSistema(), "Edición rechazada por inalterabilidad fiscal");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                    "Un comprobante autoimpreso emitido es inalterable. Utilice anulación o nota de crédito/débito."));
        }
        boolean comprobanteAnulado = esAnulado(comprobante);
        if (comprobanteAnulado && !puedeEditarAnulados()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "No se puede editar un comprobante anulado."));
        }
        Estado estado = estados.findById(solicitud.getCodigoEstado()).orElse(null);
        if (estado == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El estado seleccionado no es válido."));
        }
        boolean estadoActivo = Objects.equals(estado.getCodigoEstado(), 1);
        boolean estadoAnulado = Objects.equals(estado.getCodigoEstado(), 3);
        if (comprobanteAnulado && !estadoActivo && !estadoAnulado) {
            return ResponseEntity.badRequest().body(Map.of("mensaje",
                    "Seleccione ACTIVO o ANULADO para el comprobante."));
        }
        if (!comprobanteAnulado && !estadoActivo) {
            return ResponseEntity.badRequest().body(Map.of("mensaje",
                    "Para cambiar el estado a ANULADO utilice la acción Anular; así también se revierte la fecha desde."));
        }
        if (estadoActivo && solicitud.getCantidadPago() == 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje",
                    "Un comprobante activo debe tener una cantidad de pago mayor que cero."));
        }
        if (estadoActivo && solicitud.getPagos().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje",
                    "Agregue al menos un medio de pago para activar el comprobante."));
        }
        Cobrador cobrador = cobradores.findById(solicitud.getCodigoCobrador()).orElse(null);
        if (cobrador == null || cobrador.getSucursal() == null
                || cobrador.getEstado() == null || !Objects.equals(cobrador.getEstado().getCodigoEstado(), 1)
                || !Objects.equals(cobrador.getSucursal().getCodigoSucursal(), usuario.getSucursal().getCodigoSucursal())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Seleccione un cobrador activo de su sucursal."));
        }
        Categoria categoria = cambiaCuenta ? servicioDestino.getCategoria()
                : categorias.findById(solicitud.getCodigoCategoria()).orElse(null);
        if (categoria == null || categoria.getSucursal() == null
                || !Objects.equals(categoria.getSucursal().getCodigoSucursal(), usuario.getSucursal().getCodigoSucursal())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La categoria seleccionada no pertenece a su sucursal."));
        }
        CondicionVenta condicionVenta = condicionesVenta.findById(solicitud.getCodigoCondicionVenta()).orElse(null);
        if (condicionVenta == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La condicion de venta seleccionada no es valida."));
        }
        Usuario usuarioComprobante = buscarUsuarioEdicion(solicitud.getCodigoUsuario(), comprobante, usuario);
        var codigosMetodo = new HashSet<Integer>();
        var metodosSeleccionados = new ArrayList<MetodoPago>();
        BigDecimal totalPagos = BigDecimal.ZERO;
        for (DetallePagoV2 pago : solicitud.getPagos()) {
            if (pago.getCodigoMetodoPago() == null || pago.getImporte() == null
                    || pago.getImporte().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "Cada medio de pago debe tener un importe mayor a cero."));
            }
            if (!codigosMetodo.add(pago.getCodigoMetodoPago())) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "No puede repetir el mismo medio de pago."));
            }
            MetodoPago metodo = metodosPago.findById(pago.getCodigoMetodoPago()).orElse(null);
            if (metodo == null) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "Uno de los medios de pago no es valido."));
            }
            metodosSeleccionados.add(metodo);
            totalPagos = totalPagos.add(pago.getImporte());
        }
        if (!solicitud.getPagos().isEmpty()
                && totalPagos.compareTo(solicitud.getTotalImporte()) != 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La suma de los medios de pago debe coincidir con el importe total."));
        }
        String datosAnteriores = auditoria.capturarFoto(comprobante.getComprobantePK());
        if (cambiaCuenta) {
            comprobante.setServicio(servicioDestino);
        }
        comprobante.setUsuario(usuarioComprobante);
        comprobante.setRazonSocial(nombreCompleto(usuarioComprobante));
        comprobante.setFechaPago(solicitud.getFechaPago());
        comprobante.setPagoHasta(solicitud.getPagoDesde());
        comprobante.setPeriodoPago(limpiar(solicitud.getPeriodoPago()));
        comprobante.setCantidadDeuda(solicitud.getCantidadDeuda());
        comprobante.setCantidadPago(solicitud.getCantidadPago());
        comprobante.setTarifa(categoria.getTarifa());
        comprobante.setRecargo(solicitud.getRecargo().doubleValue());
        comprobante.setTotalImporte(solicitud.getTotalImporte().doubleValue());
        comprobante.setSaldo(solicitud.getSaldo().doubleValue());
        comprobante.setEstado(estado);
        comprobante.setCobrador(cobrador);
        comprobante.setCategoria(categoria);
        comprobante.setCondicionVenta(condicionVenta);
        comprobante.setObs(limpiar(solicitud.getObservacion()));
        actualizarDetallePago(comprobante, solicitud.getPagos(), metodosSeleccionados);
        comprobantes.saveAndFlush(comprobante);
        detallesPago.flush();
        if (cambiaNumero) {
            int actualizados = comprobantes.actualizarNumero(claveAnterior.getNumeroComprobante(), numeroNuevo,
                    claveAnterior.getPuntoExpedicionPK().getCodigoPuntoExpedicion(),
                    claveAnterior.getPuntoExpedicionPK().getCodigoSucursal(),
                    claveAnterior.getCodigoTipoComprobante(), claveAnterior.getCodigoSerie());
            if (actualizados != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No fue posible actualizar el número del comprobante.");
            }
            auditoria.renumerarHistorial(claveAnterior, numeroNuevo);
        }
        auditoria.registrarModificacion(claveNueva,
                usuario.getCodigoUsuarioSistema(), "Modificación desde la gestión de comprobantes",
                datosAnteriores);
        return ResponseEntity.ok(Map.of("mensaje", "Comprobante actualizado correctamente.",
                "numeroComprobante", String.valueOf(numeroNuevo)));
    }

    @PutMapping("/anular")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> anular(@RequestBody @Valid ComprobanteAccionV2 solicitud) {
        Comprobante comprobante = buscarAutorizado(solicitud);
        if (esAnulado(comprobante)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "El comprobante ya se encuentra anulado."));
        }
        var ultimoActivo = comprobantes.findFirstByServicio_CuentaCorrienteAndEstado_CodigoEstadoOrderByFechaEmisionDesc(
                comprobante.getServicio().getCuentaCorriente(), 1);
        if (ultimoActivo.isEmpty() || !mismoComprobante(comprobante.getComprobantePK(), ultimoActivo.get().getComprobantePK())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "Solo se puede anular el último comprobante activo de la cuenta corriente."));
        }
        comprobante.setEstado(new Estado(3));
        comprobante.setObs(limpiar(solicitud.getObservacion()));
        comprobantes.save(comprobante);
        auditoria.registrar("ANULACION", comprobante.getComprobantePK(),
                usuarioSesion().getCodigoUsuarioSistema(), solicitud.getObservacion());
        return ResponseEntity.ok(Map.of("mensaje", "Comprobante anulado correctamente."));
    }

    private Comprobante buscarAutorizado(ComprobanteAccionV2 solicitud) {
        UsuarioSistema usuario = usuarioSesion();
        if (!Objects.equals(usuario.getSucursal().getCodigoSucursal(), solicitud.getCodigoSucursal())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ComprobantePK clave = new ComprobantePK(solicitud.getNumeroComprobante(),
                new PuntoExpedicionPK(solicitud.getCodigoSucursal(), solicitud.getCodigoPuntoExpedicion()),
                solicitud.getCodigoTipoComprobante(), solicitud.getCodigoSerie());
        return comprobantes.findById(clave).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private boolean esFacturaManual(Comprobante comprobante) {
        if (comprobante.getTipoComprobante() == null
                || comprobante.getTipoComprobante().getNombreTipoComprobante() == null) {
            return false;
        }
        return "FACTURA MANUAL".equalsIgnoreCase(
                comprobante.getTipoComprobante().getNombreTipoComprobante().trim().replaceAll("\\s+", " "));
    }

    private Servicio buscarServicioEdicion(String cuentaCorriente, UsuarioSistema usuarioSesion) {
        String cuenta = limpiar(cuentaCorriente);
        if (cuenta == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingrese una cuenta corriente.");
        }
        Servicio servicio = servicios.encontrar(cuenta);
        if (servicio == null || servicio.getSucursal() == null
                || servicio.getUsuario() == null || servicio.getCategoria() == null
                || !Objects.equals(servicio.getSucursal().getCodigoSucursal(),
                        usuarioSesion.getSucursal().getCodigoSucursal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cuenta corriente no existe o no pertenece a su sucursal.");
        }
        if (servicio.getEstado() == null || !Objects.equals(servicio.getEstado().getCodigoEstado(), 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuenta corriente no esta activa.");
        }
        return servicio;
    }

    private String nombreCompleto(Servicio servicio) {
        return nombreCompleto(servicio.getUsuario());
    }

    private String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String apellido = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? usuario.getNumeroDocumento() : completo;
    }

    private Usuario buscarUsuarioEdicion(Integer codigoUsuario, Comprobante comprobante,
            UsuarioSistema usuarioSesion) {
        if (codigoUsuario == null || Objects.equals(codigoUsuario, comprobante.getUsuario().getCodigoUsuario())) {
            return comprobante.getUsuario();
        }
        Usuario usuario = usuarios.findById(codigoUsuario).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario seleccionado no existe."));
        if (usuario.getSucursal() == null || !Objects.equals(usuario.getSucursal().getCodigoSucursal(),
                usuarioSesion.getSucursal().getCodigoSucursal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario seleccionado no pertenece a su sucursal.");
        }
        return usuario;
    }

    private UsuarioSistema usuarioSesion() {
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuario == null || usuario.getSucursal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return usuario;
    }

    private ComprobanteDetalleV2 aDetalle(Comprobante c) {
        var pagos = c.getDetallePago() == null ? Collections.<DetallePagoV2>emptyList()
                : c.getDetallePago().stream().map(p -> new DetallePagoV2(
                        p.getMetodoPago().getCodigoMetodoPago(), p.getMetodoPago().getMetodoPago(),
                        BigDecimal.valueOf(p.getImporte()))).toList();
        return new ComprobanteDetalleV2(c.getTipoComprobante().getNombreTipoComprobante(),
                c.getPuntoExpedicion().getNombrePuntoExpedicion(), FormateadorNumeroComprobante.serieFiscal(c),
                c.getComprobantePK().getNumeroComprobante(), FormateadorNumeroComprobante.numeroFiscal(c),
                c.getServicio().getCuentaCorriente(),
                c.getUsuario().getCodigoUsuario(), c.getRazonSocial(), c.getUsuario().getNumeroDocumento(), c.getFechaEmision(), c.getFechaPago(),
                c.getPagoHasta(), c.getPeriodoPago(), c.getCantidadDeuda(), c.getCantidadPago(),
                BigDecimal.valueOf(c.getTarifa()), BigDecimal.valueOf(c.getRecargo()),
                BigDecimal.valueOf(c.getTotalImporte()), BigDecimal.valueOf(c.getSaldo()), c.getEstado().getEstado(),
                c.getEstado().getCodigoEstado(), c.getCobrador().getNombreCompleto(), c.getCobrador().getCodigoCobrador(),
                c.getCategoria().getNombreCategoria(), c.getCategoria().getCodigoCategoria(),
                c.getCondicionVenta().getCondicionVenta(), c.getCondicionVenta().getCodigoCondicionVenta(),
                c.getObs(), pagos);
    }

    private void actualizarDetallePago(Comprobante comprobante, List<DetallePagoV2> pagos,
            List<MetodoPago> metodosSeleccionados) {
        Map<Integer, DetallePago> anterioresPorMetodo = new HashMap<>();
        for (DetallePago anterior : comprobante.getDetallePago()) {
            anterioresPorMetodo.put(anterior.getDetallePagoPK().getCodigoMetodoPago(), anterior);
        }

        var actualizados = new ArrayList<DetallePago>();
        for (int i = 0; i < pagos.size(); i++) {
            DetallePagoV2 pago = pagos.get(i);
            DetallePago detalle = anterioresPorMetodo.remove(pago.getCodigoMetodoPago());
            if (detalle != null) {
                detalle.setImporte(pago.getImporte().doubleValue());
                detalle.setMetodoPago(metodosSeleccionados.get(i));
                actualizados.add(detalle);
                continue;
            }

            DetallePagoPK clave = DetallePagoPK.builder()
                    .codigoMetodoPago(pago.getCodigoMetodoPago())
                    .puntoExpedicionPK(comprobante.getComprobantePK().getPuntoExpedicionPK())
                    .numeroComprobante(comprobante.getComprobantePK().getNumeroComprobante())
                    .codigoTiopoComprobante(comprobante.getComprobantePK().getCodigoTipoComprobante())
                    .codigoSerie(comprobante.getComprobantePK().getCodigoSerie())
                    .build();
            DetallePago nuevo = DetallePago.builder().detallePagoPK(clave)
                    .importe(pago.getImporte().doubleValue()).metodoPago(metodosSeleccionados.get(i)).build();
            actualizados.add(detallesPago.save(nuevo));
        }

        var eliminados = new ArrayList<>(anterioresPorMetodo.values());
        comprobante.getDetallePago().clear();
        comprobante.getDetallePago().addAll(actualizados);
        detallesPago.deleteAll(eliminados);
    }

    private boolean esAnulado(Comprobante comprobante) {
        return comprobante.getEstado() != null && Objects.equals(comprobante.getEstado().getCodigoEstado(), 3);
    }

    private boolean puedeEditarAnulados() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .map(autoridad -> autoridad.getAuthority())
                .anyMatch(autoridad -> "ROOT".equals(autoridad) || "ADMINISTRADOR".equals(autoridad));
    }

    private boolean esUltimoActivo(Comprobante comprobante) {
        var ultimoActivo = comprobantes.findFirstByServicio_CuentaCorrienteAndEstado_CodigoEstadoOrderByFechaEmisionDesc(
                comprobante.getServicio().getCuentaCorriente(), 1);
        return ultimoActivo.isPresent()
                && mismoComprobante(comprobante.getComprobantePK(), ultimoActivo.get().getComprobantePK());
    }

    private boolean mismoComprobante(ComprobantePK uno, ComprobantePK otro) {
        return Objects.equals(uno.getNumeroComprobante(), otro.getNumeroComprobante())
                && Objects.equals(uno.getCodigoTipoComprobante(), otro.getCodigoTipoComprobante())
                && Objects.equals(uno.getCodigoSerie(), otro.getCodigoSerie())
                && Objects.equals(uno.getPuntoExpedicionPK().getCodigoSucursal(), otro.getPuntoExpedicionPK().getCodigoSucursal())
                && Objects.equals(uno.getPuntoExpedicionPK().getCodigoPuntoExpedicion(), otro.getPuntoExpedicionPK().getCodigoPuntoExpedicion());
    }

    private boolean esAutoimpresor(Comprobante comprobante) {
        ComprobantePK clave = comprobante.getComprobantePK();
        DetalleTimbradoPK id = new DetalleTimbradoPK(
                comprobante.getTimbrado().getCodigoTimbrado(),
                clave.getPuntoExpedicionPK().getCodigoPuntoExpedicion(),
                clave.getPuntoExpedicionPK().getCodigoSucursal());
        return detallesTimbrado.findById(id).map(x -> x.esAutoimpresor()).orElse(false);
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String mensajeError(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String mensaje = causa.getMessage();
        return causa.getClass().getSimpleName()
                + (mensaje == null || mensaje.isBlank() ? "" : ": " + mensaje);
    }

}
