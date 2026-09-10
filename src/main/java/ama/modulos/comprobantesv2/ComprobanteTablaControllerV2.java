package ama.modulos.comprobantesv2;

import ama.dominio.UsuarioSistema;
import ama.dominio.Comprobante;
import ama.dominio.ComprobantePK;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import ama.servicio.CategoriaService;
import ama.servicio.CondicionVentaService;
import ama.servicio.MetodoPagoService;

@Controller
@RequestMapping("/comprobantes-v2")
public class ComprobanteTablaControllerV2 {
    private final ComprobanteJpaRepositoryV2 comprobantes;
    private final CobradorJpaRepositoryV2 cobradores;
    private final EstadoJpaRepositoryV2 estados;
    private final CategoriaService categorias;
    private final CondicionVentaService condicionesVenta;
    private final MetodoPagoService metodosPago;
    private final HttpSession session;
    public ComprobanteTablaControllerV2(ComprobanteJpaRepositoryV2 comprobantes,
            CobradorJpaRepositoryV2 cobradores, EstadoJpaRepositoryV2 estados,
            CategoriaService categorias, CondicionVentaService condicionesVenta,
            MetodoPagoService metodosPago, HttpSession session) {
        this.comprobantes = comprobantes;
        this.cobradores = cobradores;
        this.estados = estados;
        this.categorias = categorias;
        this.condicionesVenta = condicionesVenta;
        this.metodosPago = metodosPago;
        this.session = session;
    }
    @GetMapping public String pagina(Model model) {
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuario == null || usuario.getSucursal() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        model.addAttribute("titulo", "Comprobantes - V2");
        model.addAttribute("cobradoresEdicion",
                cobradores.findBySucursal_CodigoSucursalAndEstado_CodigoEstadoOrderByNombreAscApellidoAsc(
                        usuario.getSucursal().getCodigoSucursal(), 1));
        model.addAttribute("estadosEdicion", estados.findByCodigoEstadoInOrderByCodigoEstadoAsc(List.of(1, 3)));
        model.addAttribute("categoriasEdicion", categorias.listar(usuario.getSucursal()));
        model.addAttribute("condicionesVentaEdicion", condicionesVenta.listar());
        model.addAttribute("metodosPagoEdicion", metodosPago.listar());
        return "comprobantesv2/lista";
    }
    @PostMapping("/tabla") @ResponseBody
    public ResponseEntity<DataTableResponseV2<ComprobanteFilaV2>> datos(
            @RequestParam int draw, @RequestParam(defaultValue = "0") int start, @RequestParam(defaultValue = "10") int length,
            @RequestParam(name = "search[value]", required = false) String busqueda,
            @RequestParam(name = "order[0][column]", defaultValue = "5") int columna,
            @RequestParam(name = "order[0][dir]", defaultValue = "desc") String direccion,
            @RequestParam(required = false) Integer puntoExpedicion, @RequestParam(required = false) Integer serie,
            @RequestParam(required = false) Integer numero, @RequestParam(required = false) Integer estado,
            @RequestParam(required = false) String cuenta, @RequestParam(required = false) String documento,
            @RequestParam(required = false) String nombre, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        if (usuario == null || usuario.getSucursal() == null) return ResponseEntity.status(401).build();
        ComprobanteFiltroV2 filtro = new ComprobanteFiltroV2();
        filtro.setSucursal(usuario.getSucursal().getCodigoSucursal()); filtro.setPuntoExpedicion(puntoExpedicion); filtro.setSerie(serie); filtro.setNumero(numero); filtro.setEstado(estado); filtro.setCuenta(cuenta); filtro.setDocumento(documento); filtro.setNombre(nombre); filtro.setDesde(desde); filtro.setHasta(hasta);
        if (busqueda != null && !busqueda.isBlank()) filtro.setNombre(busqueda);
        filtro.setOffset(Math.max(start, 0)); filtro.setLimit(Math.min(Math.max(length, 1), 100)); filtro.setOrden(orden(columna)); filtro.setDireccion(direccion);
        Sort.Direction sentido = "asc".equalsIgnoreCase(direccion) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Specification<Comprobante> especificacion = ComprobanteSpecificationsV2.filtrar(filtro);
        var pagina = comprobantes.findAll(especificacion, PageRequest.of(filtro.getOffset() / filtro.getLimit(), filtro.getLimit(), Sort.by(sentido, filtro.getOrden())));
        Specification<Comprobante> alcanceSucursal = (root, query, cb) -> cb.equal(root.get("comprobantePK").get("puntoExpedicionPK").get("codigoSucursal"), filtro.getSucursal());
        return ResponseEntity.ok(new DataTableResponseV2<>(draw, comprobantes.count(alcanceSucursal), pagina.getTotalElements(), pagina.getContent().stream().map(this::fila).toList()));
    }
    private ComprobanteFilaV2 fila(Comprobante comprobante) {
        ComprobantePK clave = comprobante.getComprobantePK();
        return new ComprobanteFilaV2(
                clave.getPuntoExpedicionPK().getCodigoSucursal(),
                clave.getPuntoExpedicionPK().getCodigoPuntoExpedicion(),
                clave.getCodigoTipoComprobante(),
                clave.getCodigoSerie(),
                clave.getNumeroComprobante(),
                FormateadorNumeroComprobante.numeroFiscal(comprobante),
                FormateadorNumeroComprobante.serieFiscal(comprobante),
                comprobante.getTipoComprobante().getNombreTipoComprobante(),
                comprobante.getPuntoExpedicion().getNombrePuntoExpedicion(),
                comprobante.getServicio().getCuentaCorriente(),
                comprobante.getRazonSocial(),
                comprobante.getFechaPago(),
                BigDecimal.valueOf(comprobante.getTarifa()),
                comprobante.getCantidadPago(),
                BigDecimal.valueOf(comprobante.getRecargo()),
                BigDecimal.valueOf(comprobante.getTotalImporte()),
                BigDecimal.valueOf(comprobante.getSaldo()),
                comprobante.getEstado().getEstado());
    }
    private String orden(int columna) { return switch (columna) { case 0 -> "tipoComprobante.nombreTipoComprobante"; case 1 -> "puntoExpedicion.nombrePuntoExpedicion"; case 2 -> "comprobantePK.numeroComprobante"; case 3 -> "servicio.cuentaCorriente"; case 4 -> "razonSocial"; case 6 -> "totalImporte"; case 7 -> "estado.estado"; default -> "fechaPago"; }; }
}
