package ama.api.controladores;

import ama.dominio.Ciudad;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ama.servicio.CiudadService;

@Slf4j
@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api")
public class ControladorCiudadApi {

    @Autowired
    private CiudadService servicioCiudad;

    @GetMapping("/ciudades")

    public @ResponseBody
    List<Ciudad> listaSucursal() {
        var ciudades = servicioCiudad.listarCiudad();
        return ciudades;

    }

    @GetMapping("/agregarCiudada")
    public String agregar() {
        return "modificar";
    }

    @PostMapping("/guardarCiudad")
    public ResponseEntity<Ciudad> guardar(@RequestBody Ciudad ciudad) {
        Ciudad ciud = servicioCiudad.guardar(ciudad);
        return new ResponseEntity<Ciudad>(ciud, HttpStatus.OK);
    }
//    
//    @GetMapping("/editar/{idPersona}")
//    public String editar(Persona persona, Model model){
//        persona = servicioPersona.encontrarPersona(persona);
//        model.addAttribute("persona", persona);
//        return "modificar";
//    }
//    

    @GetMapping("/eliminar/{id}")
    public ResponseEntity<Ciudad> eliminar(@PathVariable Integer id) {
        Ciudad ciudad = new Ciudad();
        ciudad.setCodigoCiudad(id);
         ciudad = servicioCiudad.encontrarCiudad(ciudad);
        if (ciudad != null) {
            servicioCiudad.eliminar(ciudad);
        } else {
            return new ResponseEntity<>( HttpStatus.NOT_FOUND);
        }
        return null;
    }

}
