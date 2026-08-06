package ama.controladorMVC;

import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.dominio.Zona;
import ama.validador.Mayuscula;
import ama.validador.Vadidador;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ama.servicio.CobradorService;
import ama.servicio.ZonaService;
import ama.servicio.SucursalService;
import ama.servicio.CiudadService;

@Slf4j
@Controller
@RequestMapping("/zona")
public class ZonaController {

    @Autowired
    private Vadidador validar;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @Autowired
    private ZonaService zonaService;
    @Autowired
    private CobradorService cobradorService;
    @Autowired
    private SucursalService servicioSucursal;
    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listaZona(Model modelo) {
        modelo.addAttribute("titulo", "Zona");
        var zonas = zonaService.listar();
        modelo.addAttribute("zonas", zonas);

        return "zona/zona";
    }

    @GetMapping("/agregar")
    public String agregar(Model model) {
        model.addAttribute("titulo", "zona");
        
        Zona zona = new Zona();
        model.addAttribute("zona", zona);
        
        var cobradores = cobradorService.listarIsEstadoActivo(getSucursalSession());
        model.addAttribute("cobradores", cobradores);

        var sucursales = servicioSucursal.listar();
        model.addAttribute("sucursales", sucursales);

        var ciudad = servicioCiudad.listarCiudad();
        model.addAttribute("ciudad", ciudad);
        return "zona/modificarZona";
    }

    @PostMapping("/guardar")
    public String guardar(Zona zona, RedirectAttributes redirectAttributes) {
        if (zona.getCodigoZona() == null) {
            Integer codigoZona = zonaService.getCodigoZona() + 1;
            zona.setCodigoZona(codigoZona);
        }
        zonaService.guardar(zona);
        redirectAttributes.addFlashAttribute("mensaje", "Registro guardado corectamente!!");
        return "redirect:/zona/listar";
    }

    @GetMapping("/editar/{codigoZona}")
    public String editar(Zona zona, Model model) {
        model.addAttribute("titulo", "zona");
        zona = zonaService.encontrar(zona);
        if(zona==null){
            throw  new Error("Zona no encontrada ");
        }
        model.addAttribute("zona", zona);

        model.addAttribute("cobradores",cobradorService.listarIsEstadoActivo(getSucursalSession()));
        
        model.addAttribute("sucursales",zona.getSucursal());
        
        model.addAttribute("ciudad", zona.getSucursal().getCiudad());
        
        return "zona/modificarZona";
    }

    
    @PostMapping("/eliminar/{codigoZona}")
    public ResponseEntity<String> eliminar(Zona zona) {
        try {
            zonaService.eliminar(zona);
            return ResponseEntity.ok("Zona Eliminado Correctamente !!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se pudo eliminar el usuario " + e.getMessage());
        }
    }
    
     private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal getSucursalSession() {
        return getUserSession().getSucursal();
    }
}
