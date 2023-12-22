package ama.controladorMVC;

import ama.dominio.Categoria;
import ama.dominio.Sucursal;
import ama.errores.ClaseError;
import ama.servicio.ServicioCategoria;
import ama.servicio.ServicioCiudad;
import ama.servicio.ServicioSucursal;
import ama.validador.Vadidador;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;

@Slf4j
@Controller
@SessionAttributes("sucursal")
@RequestMapping("/categoria")
public class CategoriaController {

    
    @Autowired
    private Vadidador validar;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
//        binder.registerCustomEditor(String.class, new Mayuscula());
//        binder.addValidators(validar);
    }

    @Autowired
    private ServicioCategoria servicioCategoria;
    @Autowired
    private ServicioSucursal servicioSucursal;
    @Autowired
    private ServicioCiudad servicioCiudad;

    @GetMapping("/listar")
    public String listaCtegoria(Model modelo) {
        modelo.addAttribute("titulo", "Categoria");
        List<Categoria> categorias = servicioCategoria.listar();
        modelo.addAttribute("categorias", categorias);
        List<Sucursal> sucursal = servicioSucursal.listar();
        modelo.addAttribute("sucursal", sucursal);
        return "categoria/categoria";
    }


    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<String> guardar(
            @RequestBody @Valid Categoria categoria, BindingResult result
    ) {
        StringBuffer errores = new StringBuffer();
        if (result.hasErrors()) {
            result.getAllErrors().forEach(error -> {
                errores.append(error.getDefaultMessage());
            });
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al guardar registro: " + errores);
        }
        if (categoria.getCodigoCategoria() == null) {
            Integer codigoCategoria = servicioCategoria.getCodigoCategoria() + 1;
            categoria.setCodigoCategoria(codigoCategoria);
        }
        try {
            Categoria newCategoria = new Categoria();
            servicioCategoria.guardar(categoria);
            return ResponseEntity.ok("Registro guardado correctamente ");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al guardar registro" + e.getMessage());
        }
    }

    @PostMapping("/editar")
    @ResponseBody
    public ResponseEntity<?> editar(@RequestBody Categoria categoria) {
        var categoriaEncontrada = servicioCategoria.encontrar(categoria);
        if (categoriaEncontrada == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Registro no encontrado");
        }
        return ResponseEntity.ok(categoriaEncontrada);
    }

    @PostMapping("/eliminar/{codigoCategoria}")
    public ResponseEntity<?> eliminar(Categoria categoria) {
        try {
            servicioCategoria.eliminar(categoria);
            return ResponseEntity.ok().body("Categoria Eliminda Correctamente ");
        } catch (DataIntegrityViolationException e) {
           
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ClaseError.excepcion("Error al Eliminar Categoria ", e) );
        }
    }
}
