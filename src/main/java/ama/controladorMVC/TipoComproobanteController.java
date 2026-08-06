package ama.controladorMVC;

import ama.dominio.TipoComprobante;
import ama.validador.Mayuscula;
import ama.validador.Vadidador;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ama.servicio.TipoComprobanteService;
import ama.servicio.CiudadService;
import org.springframework.http.ResponseEntity;

@Slf4j
@Controller
@RequestMapping("/tipoComprobante")
public class TipoComproobanteController {

    @Autowired
    private Vadidador validar;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @Autowired
    private TipoComprobanteService tipoComprobanteService;
    @Autowired
    private CiudadService servicioCiudad;

    @GetMapping("/listar")
    public String listaTipoComprobante(Model modelo) {
        modelo.addAttribute("titulo", "TipoComprobante");
        var tipoComprobantees = tipoComprobanteService.listar();
        modelo.addAttribute("tipoComprobantes", tipoComprobantees);

        return "tipoComprobante/tipoComprobante";
    }

    @PreAuthorize("hasAnyAuthority({'ROOT'})")
    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        modelo.addAttribute("titulo", "TipoComprobante");
        var tipoComprobante = new TipoComprobante();
        modelo.addAttribute("tipoComprobante", tipoComprobante);
       
        return "tipoComprobante/modificarTipoComprobante";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority({'ROOT'})")
    public String guardar(TipoComprobante tipoComprobante, RedirectAttributes flash) {
        log.info(tipoComprobante.toString());
        if (tipoComprobante.getCodigoTipoComprobante() == null) {
            Integer codigoTipoComprobante = tipoComprobanteService.getCodigoTipoComprobante() + 1;
            tipoComprobante.setCodigoTipoComprobante(codigoTipoComprobante);
        }
        tipoComprobanteService.guardar(tipoComprobante);
        flash.addFlashAttribute("info", "TipoComprobante guardada correctamente!!");
        return "redirect:/tipoComprobante/listar";
    }

    @GetMapping("/editar/{codigoTipoComprobante}")
    @PreAuthorize("hasAnyAuthority({'ROOT'})")
    public String editar(TipoComprobante tipoComprobante, Model model) {
        model.addAttribute("titulo", "Editar TipoComprobante");
        tipoComprobante = tipoComprobanteService.encontrar(tipoComprobante);
        model.addAttribute("tipoComprobante", tipoComprobante);
        return "tipoComprobante/modificarTipoComprobante";
    }

    @PostMapping("/eliminar/{codigoTipoComprobante}")
    @PreAuthorize("hasAnyAuthority({'ROOT'})")
    public ResponseEntity<?> eliminar(TipoComprobante tipoComprobante, RedirectAttributes flash) {
        tipoComprobanteService.eliminar(tipoComprobante);
        return ResponseEntity.ok("TipoComprobante eliminada correctamente!!");
    }
}
