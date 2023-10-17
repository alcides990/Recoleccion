package ama.controladorMVC;

import ama.dominio.Cobrador;
import ama.errores.ClaseError;
import ama.servicio.ServicioCiudad;
import ama.servicio.ServicioCobrador;
import ama.servicio.ServicioEstado;
import ama.servicio.ServicioSucursal;
import ama.validador.Mayuscula;
import ama.validador.Vadidador;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

@Slf4j
@Controller
@SessionAttributes(names = {"sucursal", "ciudad", "estado"})
@RequestMapping("/cobrador")
public class ControladorCobrador {

    @Autowired
    private Vadidador validar;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @Autowired
    private ServicioSucursal servicioSucursal;

    @Autowired
    private ServicioCiudad servicioCiudad;
    @Autowired
    private ServicioCobrador servicioCobrador;
    @Autowired
    private ServicioEstado servicioEstado;

    @GetMapping("/listar")
    public String listaCobradores(Model modelo) {
        var cobradores = servicioCobrador.listar();
        modelo.addAttribute("cobradores", cobradores);
        modelo.addAttribute("titulo", "Cobrador");

        return "cobrador/cobrador";
    }

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        var cobrador = new Cobrador();
        modelo.addAttribute("cobrador", cobrador);

        var sucursales = servicioSucursal.listar();
        modelo.addAttribute("sucursal", sucursales);

        var ciudades = servicioCiudad.listarCiudad();
        modelo.addAttribute("ciudad", ciudades);

        var estado = servicioEstado.listar();
        modelo.addAttribute("estado", estado);
        modelo.addAttribute("titulo", "Cobrador");

        return "cobrador/modificarCobrador";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid Cobrador cobrador, BindingResult resul, SessionStatus status) {
        if (resul.hasFieldErrors()) {
            return "cobrador/modificarCobrador";
        }
        if (cobrador.getCodigoCobrador() == null) {
            Integer codigoCobrador = servicioCobrador.getCodigoCobrador() + 1;
            cobrador.setCodigoCobrador(codigoCobrador);
        }
        status.setComplete();
        servicioCobrador.guardar(cobrador);
        return "redirect:/cobrador/listar";
    }

    @GetMapping("/editar/{codigoCobrador}")
    public String editar(Cobrador cobrador, Model model) {
        cobrador = servicioCobrador.encontrar(cobrador);
        if(cobrador==null){
            throw  new Error("Cobrar no encontrado ");
        }
        model.addAttribute("cobrador", cobrador);
//        log.info("Cobrador a modificar "+cobrador);
        var sucursal = cobrador.getSucursal();
        model.addAttribute("sucursal", sucursal);
        var ciudad = cobrador.getSucursal().getCiudad();
        model.addAttribute("ciudad", ciudad);

        var estado = servicioEstado.listar();
        model.addAttribute("estado", estado);
        model.addAttribute("titulo", "Cobrador");

        return "cobrador/modificarCobrador";
    }

    @PostMapping("/eliminar/{codigoCobrador}")
    public ResponseEntity<?> eliminar(Cobrador cobrador) {
        try {
            log.info("cobrador recibido "+cobrador);
            servicioCobrador.eliminar(cobrador);
            return ResponseEntity.ok("Cobrador Eliminado Correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ClaseError.excepcion("Error al Eliminar Cobrador", e));
        }
    }
}
