package ama.controladorMVC;

import ama.dominio.Zona;
import ama.servicio.ServicioCiudad;
import ama.servicio.ServicioSucursal;
import ama.servicio.ServicioZona;
import ama.validador.Mayuscula;
import ama.validador.Vadidador;
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
    private ServicioZona servicioZona;
    @Autowired
    private ServicioSucursal servicioSucursal;
    @Autowired
    private ServicioCiudad servicioCiudad;

    @GetMapping("/listar")
    public String listaZona(Model modelo) {
        modelo.addAttribute("titulo", "Zona");
        var zonas = servicioZona.listar();
        modelo.addAttribute("zonas", zonas);

        return "zona/zona";
    }

    @GetMapping("/agregar")
    public String agregar(Model model) {
        model.addAttribute("titulo", "zona");
        Zona zona = new Zona();
        model.addAttribute("zona", zona);

        var sucursal = servicioSucursal.listar();
        model.addAttribute("sucursal", sucursal);

        var ciudad = servicioCiudad.listarCiudad();
        model.addAttribute("ciudad", ciudad);
        return "zona/modificarZona";
    }

    @PostMapping("/guardar")
    public String guardar(Zona zona, RedirectAttributes redirectAttributes) {
        if (zona.getCodigoZona() == null) {
            Integer codigoZona = servicioZona.getCodigoZona() + 1;
            zona.setCodigoZona(codigoZona);
        }
        servicioZona.guardar(zona);
        redirectAttributes.addFlashAttribute("mensaje", "Registro guardado corectamente!!");
        return "redirect:/zona/listar";
    }

    @GetMapping("/editar/{codigoZona}")
    public String editar(Zona zona, Model model) {
        model.addAttribute("titulo", "zona");
        zona = servicioZona.encontrar(zona);
        if(zona==null){
            throw  new Error("Zona no encontrada ");
        }
        model.addAttribute("zona", zona);

        var sucursal = zona.getSucursal();
        model.addAttribute(sucursal);
        var ciudad = zona.getSucursal().getCiudad();
        model.addAttribute(ciudad);
        return "zona/modificarZona";
    }

    
    @PostMapping("/eliminar/{codigoZona}")
    public ResponseEntity<String> eliminar(Zona zona) {
        try {
            servicioZona.eliminar(zona);
            return ResponseEntity.ok("Zona Eliminado Correctamente !!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se pudo eliminar el usuario " + e.getMessage());
        }
    }
}
