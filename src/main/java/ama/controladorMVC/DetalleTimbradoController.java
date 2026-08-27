package ama.controladorMVC;

import ama.dao.DetalleTimbradoDao;
import ama.dominio.DetalleTimbrado;
import ama.dominio.DetalleTimbradoPK;
import ama.dominio.Estado;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Serie;
import ama.dominio.Timbrado;
import ama.dominio.UsuarioSistema;
import ama.servicio.EstadoService;
import ama.servicio.PuntoExpedicionService;
import ama.servicio.SerieService;
import ama.servicio.TimbradoService;
import ama.servicio.SucursalService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/detalleTimbrado")
@PreAuthorize("hasAnyAuthority('ROOT','ADMINISTRADOR')")
public class DetalleTimbradoController {

    private final DetalleTimbradoDao detalleDao;
    private final TimbradoService timbradoService;
    private final PuntoExpedicionService puntoService;
    private final SerieService serieService;
    private final EstadoService estadoService;
    private final SucursalService sucursalService;
    private final HttpSession session;

    public DetalleTimbradoController(DetalleTimbradoDao detalleDao, TimbradoService timbradoService,
            PuntoExpedicionService puntoService, SerieService serieService,
            EstadoService estadoService, SucursalService sucursalService, HttpSession session) {
        this.detalleDao = detalleDao;
        this.timbradoService = timbradoService;
        this.puntoService = puntoService;
        this.serieService = serieService;
        this.estadoService = estadoService;
        this.sucursalService = sucursalService;
        this.session = session;
    }

    @GetMapping("/listar")
    public String listar(Model model) {
        UsuarioSistema usuario = usuario();
        List<DetalleTimbrado> detalles = usuario.getCodigoUsuarioSistema() == 0
                ? detalleDao.findAll()
                : detalleDao.findByDetalleTimbradoPKCodigoSucursalOrderByDetalleTimbradoPKCodigoTimbradoAsc(
                        usuario.getSucursal().getCodigoSucursal());
        model.addAttribute("titulo", "Detalle de timbrados");
        model.addAttribute("detalles", detalles);
        return "detalleTimbrado/listar";
    }

    @GetMapping("/agregar")
    public String agregar(Model model) {
        cargarCombos(model, null);
        model.addAttribute("detalle", new DetalleTimbrado());
        model.addAttribute("edicion", false);
        return "detalleTimbrado/formulario";
    }

    @GetMapping("/editar/{timbrado}/{punto}/{sucursal}")
    public String editar(@PathVariable Integer timbrado, @PathVariable Integer punto,
            @PathVariable Integer sucursal, Model model, RedirectAttributes redirect) {
        DetalleTimbradoPK id = new DetalleTimbradoPK(timbrado, punto, sucursal);
        DetalleTimbrado detalle = detalleDao.findById(id).orElse(null);
        if (detalle == null || !puedeAdministrar(sucursal)) {
            redirect.addFlashAttribute("error", "No se encontró el detalle de timbrado solicitado.");
            return "redirect:/detalleTimbrado/listar";
        }
        cargarCombos(model, sucursal);
        model.addAttribute("detalle", detalle);
        model.addAttribute("edicion", true);
        return "detalleTimbrado/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Integer codigoTimbrado,
            @RequestParam Integer codigoPuntoExpedicion,
            @RequestParam Integer codigoSucursal,
            @RequestParam Integer codigoSerie,
            @RequestParam Integer codigoEstado,
            @RequestParam(defaultValue = "MANUAL") String modoEmision,
            @RequestParam(defaultValue = "1") Integer numeroDesde,
            @RequestParam(defaultValue = "9999999") Integer numeroHasta,
            RedirectAttributes redirect) {
        if (!puedeAdministrar(codigoSucursal)) {
            redirect.addFlashAttribute("error", "No puede administrar otra sucursal.");
            return "redirect:/detalleTimbrado/listar";
        }
        PuntoExpedicionPK puntoId = new PuntoExpedicionPK(codigoSucursal, codigoPuntoExpedicion);
        if (puntoService.encontrar(puntoId) == null || timbradoService.encontrar(new Timbrado(codigoTimbrado)) == null) {
            redirect.addFlashAttribute("error", "El timbrado o punto de expedición seleccionado no existe.");
            return "redirect:/detalleTimbrado/listar";
        }
        DetalleTimbradoPK id = new DetalleTimbradoPK(codigoTimbrado, codigoPuntoExpedicion, codigoSucursal);
        String modo = modoEmision == null ? "MANUAL" : modoEmision.trim().toUpperCase();
        if (!("MANUAL".equals(modo) || "AUTOIMPRESOR".equals(modo))
                || numeroDesde == null || numeroHasta == null || numeroDesde < 1
                || numeroHasta < numeroDesde || numeroHasta > 9_999_999) {
            redirect.addFlashAttribute("error", "El modo de emisión o el rango autorizado no es válido.");
            return "redirect:/detalleTimbrado/listar";
        }
        DetalleTimbrado detalle = detalleDao.findById(id).orElseGet(DetalleTimbrado::new);
        detalle.setDetalleTimbradoPK(id);
        detalle.setSerie(serieService.encontrar(new Serie(codigoSerie)));
        detalle.setEstado(estadoService.encontrar(new Estado(codigoEstado)));
        detalle.setModoEmision(modo);
        detalle.setNumeroDesde(numeroDesde);
        detalle.setNumeroHasta(numeroHasta);
        detalleDao.save(detalle);
        redirect.addFlashAttribute("info", "Detalle de timbrado guardado correctamente.");
        return "redirect:/detalleTimbrado/listar";
    }

    @PostMapping("/eliminar/{timbrado}/{punto}/{sucursal}")
    @PreAuthorize("hasAuthority('ROOT')")
    public String eliminar(@PathVariable Integer timbrado, @PathVariable Integer punto,
            @PathVariable Integer sucursal, RedirectAttributes redirect) {
        DetalleTimbradoPK id = new DetalleTimbradoPK(timbrado, punto, sucursal);
        if (!detalleDao.existsById(id)) {
            redirect.addFlashAttribute("error", "El detalle de timbrado ya no existe.");
            return "redirect:/detalleTimbrado/listar";
        }
        detalleDao.deleteById(id);
        redirect.addFlashAttribute("info", "Detalle de timbrado eliminado correctamente.");
        return "redirect:/detalleTimbrado/listar";
    }

    private void cargarCombos(Model model, Integer sucursalEdicion) {
        UsuarioSistema usuario = usuario();
        model.addAttribute("titulo", "Detalle de timbrado");
        model.addAttribute("timbrados", timbradoService.listar());
        model.addAttribute("series", serieService.listar());
        model.addAttribute("estados", estadoService.findByEstadoIn(List.of("Activo", "Inactivo", "Anulado")));
        if (usuario.getCodigoUsuarioSistema() == 0) {
            model.addAttribute("sucursales", sucursalService.listar());
            model.addAttribute("puntos", puntoService.listar());
        } else {
            model.addAttribute("sucursales", List.of(usuario.getSucursal()));
            model.addAttribute("puntos", puntoService.listar(usuario.getSucursal()));
        }
    }

    private boolean puedeAdministrar(Integer sucursal) {
        UsuarioSistema usuario = usuario();
        return usuario.getCodigoUsuarioSistema() == 0
                || usuario.getSucursal().getCodigoSucursal().equals(sucursal);
    }

    private UsuarioSistema usuario() {
        return (UsuarioSistema) session.getAttribute("usuarioSistema");
    }
}
