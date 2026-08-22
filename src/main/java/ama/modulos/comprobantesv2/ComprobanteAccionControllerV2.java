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
import ama.dao.CategoriaDao;
import ama.dao.CondicionVentaDao;
import ama.dao.MetodoPagoDao;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.UsuarioSistema;
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
import net.sf.jasperreports.engine.JasperCompileManager;
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

    public ComprobanteAccionControllerV2(ComprobanteJpaRepositoryV2 comprobantes,
            CobradorJpaRepositoryV2 cobradores, EstadoJpaRepositoryV2 estados,
            CategoriaDao categorias, CondicionVentaDao condicionesVenta, MetodoPagoDao metodosPago,
            DetallePagoJpaRepositoryV2 detallesPago, HttpSession session, DataSource dataSource,
            MontoEnLetrasService montoEnLetras) {
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
    }

    @PostMapping("/detalle")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<ComprobanteDetalleV2> detalle(@RequestBody @Valid ComprobanteAccionV2 solicitud) {
        return ResponseEntity.ok(aDetalle(buscarAutorizado(solicitud)));
    }

    @GetMapping("/ticket")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> ticket(ComprobanteAccionV2 solicitud,
            @RequestParam(name = "formato", defaultValue = "58mm") String formato) {
        try {
            Comprobante comprobante = buscarAutorizado(solicitud);
            String rutaReporte = switch (formato.toLowerCase()) {
                case "58mm" -> "reportes/comprobanteTicket.jrxml";
                case "80mm" -> "reportes/comprobanteTicket80mm.jrxml";
                case "a4" -> "reportes/comprobanteA4.jrxml";
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
            try (InputStream jasper = new ClassPathResource(rutaReporte).getInputStream();
                    var conexion = dataSource.getConnection()) {
                JasperReport reporte = JasperCompileManager.compileReport(jasper);
                var impresion = JasperFillManager.fillReport(reporte, parametros, conexion);
                byte[] pdf = JasperExportManager.exportReportToPdf(impresion);
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
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','ROOT')")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> editar(@RequestBody @Valid ComprobanteAccionV2 solicitud) {
        if (solicitud.getFechaPago() == null || solicitud.getCodigoEstado() == null
                || solicitud.getCodigoCobrador() == null || solicitud.getCodigoCategoria() == null
                || solicitud.getCodigoCondicionVenta() == null || solicitud.getCantidadPago() == null
                || solicitud.getRecargo() == null
                || solicitud.getTotalImporte() == null || solicitud.getSaldo() == null
                || solicitud.getPagos() == null || solicitud.getPagos().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Complete todos los campos obligatorios del comprobante."));
        }
        UsuarioSistema usuario = usuarioSesion();
        Comprobante comprobante = buscarAutorizado(solicitud);
        if (esAnulado(comprobante)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", "No se puede editar un comprobante anulado."));
        }
        Estado estado = estados.findById(solicitud.getCodigoEstado()).orElse(null);
        if (estado == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El estado seleccionado no es válido."));
        }
        if (!Objects.equals(estado.getCodigoEstado(), 1)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje",
                    "Para cambiar el estado a ANULADO utilice la acción Anular; así también se revierte la fecha desde."));
        }
        Cobrador cobrador = cobradores.findById(solicitud.getCodigoCobrador()).orElse(null);
        if (cobrador == null || cobrador.getSucursal() == null
                || cobrador.getEstado() == null || !Objects.equals(cobrador.getEstado().getCodigoEstado(), 1)
                || !Objects.equals(cobrador.getSucursal().getCodigoSucursal(), usuario.getSucursal().getCodigoSucursal())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Seleccione un cobrador activo de su sucursal."));
        }
        Categoria categoria = categorias.findById(solicitud.getCodigoCategoria()).orElse(null);
        if (categoria == null || categoria.getSucursal() == null
                || !Objects.equals(categoria.getSucursal().getCodigoSucursal(), usuario.getSucursal().getCodigoSucursal())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La categoria seleccionada no pertenece a su sucursal."));
        }
        CondicionVenta condicionVenta = condicionesVenta.findById(solicitud.getCodigoCondicionVenta()).orElse(null);
        if (condicionVenta == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La condicion de venta seleccionada no es valida."));
        }
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
        if (totalPagos.compareTo(solicitud.getTotalImporte()) != 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La suma de los medios de pago debe coincidir con el importe total."));
        }
        comprobante.setRazonSocial(limpiar(solicitud.getRazonSocial()));
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
        comprobantes.save(comprobante);
        actualizarDetallePago(comprobante, solicitud.getPagos(), metodosSeleccionados);
        return ResponseEntity.ok(Map.of("mensaje", "Comprobante actualizado correctamente."));
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
                c.getPuntoExpedicion().getNombrePuntoExpedicion(), c.getSerie().getSerie(),
                c.getComprobantePK().getNumeroComprobante(), c.getServicio().getCuentaCorriente(),
                c.getRazonSocial(), c.getUsuario().getNumeroDocumento(), c.getFechaEmision(), c.getFechaPago(),
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
