package ama.controladorMVC;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@Slf4j
public class ControladorInicio {


    @GetMapping("/")
    public String inicio() {
        return "index";
    }

//    @GetMapping("/agregar")
//    public String agregar(Model modelo) {
//        Ciudad ciudad = new Ciudad();
//        modelo.addAttribute("ciudad", ciudad);
//        return "modificar";
//    }
//
//    @PostMapping("/guardar")
//    public String guardar(Ciudad ciudad) {
//        servicioCiudad.guardar(ciudad);
//        return "redirect:/";
//    }
//
//    @GetMapping("/editar/{codigoCiudad}")
//    public String editar(Ciudad ciudad, Model model) {
//        ciudad = servicioCiudad.encontrarCiudad(ciudad);
//        model.addAttribute("ciudad", ciudad);
//        return "modificar";
//    }
//
//    @GetMapping("/eliminar")
//    public String eliminar(Ciudad ciudad) {
//        servicioCiudad.eliminar(ciudad);
//        return "redirect:/";
//    }
}
