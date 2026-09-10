package ama.controladorMVC;

import ama.dominio.Ciudad;
import ama.servicio.CiudadService;
import ama.validador.Mayuscula;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ciudad")
@RequiredArgsConstructor
public class CiudadController {

    private final CiudadService ciudadService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @GetMapping("/listar")
    public String listaCiudad(Model model) {
        model.addAttribute("ciudades", ciudadService.listarCiudad());
        return "ciudad/ciudad";
    }

    

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        modelo.addAttribute("ciudad", new Ciudad());
        modelo.addAttribute("titulo", "Ciudad");
        return "ciudad/modificarCiudad";
    }

    @PostMapping("/guardar")
    public String guardar(Ciudad ciudad, RedirectAttributes flash) {
        String mensaje = "Ciudad modificada correctamente.";
        if (ciudad.getCodigoCiudad() == null) {
            mensaje = "Ciudad agregada correctamente.";
            Integer codigoCiudad = ciudadService.getCodigoCiudad() + 1;
            ciudad.setCodigoCiudad(codigoCiudad);
        }
        ciudadService.guardar(ciudad);
        flash.addFlashAttribute("info", mensaje);
        return "redirect:/ciudad/listar";
    }

    @GetMapping("/editar/{codigoCiudad}")
    public String editar(Ciudad ciudad, Model model) {
        ciudad = ciudadService.encontrarCiudad(ciudad);
        model.addAttribute("ciudad", ciudad);
        model.addAttribute("titulo", "Ciudad");
        return "ciudad/modificarCiudad";
    }

    @GetMapping("/eliminar")
    public String eliminar(Ciudad ciudad) {
        ciudadService.eliminar(ciudad);
        return "redirect:/ciudad/listar";
    }
}
