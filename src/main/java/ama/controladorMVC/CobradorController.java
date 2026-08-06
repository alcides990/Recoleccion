package ama.controladorMVC;

import ama.dominio.Cobrador;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.validador.Mayuscula;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ama.servicio.CobradorService;
import ama.servicio.SucursalService;
import ama.servicio.CiudadService;
import ama.servicio.EstadoService;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;

@Slf4j
@Controller
@SessionAttributes(names = {"sucursal", "ciudad", "estado"})
@RequestMapping("/cobrador")
public class CobradorController {
 
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @Autowired
    private SucursalService servicioSucursal;

    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private CobradorService servicioCobrador;
    @Autowired
    private EstadoService servicioEstado;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listaCobradores(Model modelo) {
        var cobradores = servicioCobrador.listar(getSucursalSession());
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
    public String guardar(@Valid Cobrador cobrador,
            BindingResult resul, SessionStatus status,
            RedirectAttributes flash) {
        if (resul.hasFieldErrors()) {
            return "cobrador/modificarCobrador";
        }
        if (cobrador.getCodigoCobrador() == null) {
            Integer codigoCobrador = servicioCobrador.getCodigoCobrador() + 1;
            cobrador.setCodigoCobrador(codigoCobrador);
        }
        flash.addFlashAttribute("info", "Registro guardado correctamente!!");
        status.setComplete();
        servicioCobrador.guardar(cobrador);
        return "redirect:/cobrador/listar";
    }

    @PreAuthorize("hasAnyAuthority({'ROOT','ADMIN'})")
    @GetMapping("/editar/{codigoCobrador}")
    public String editar(Cobrador cobrador, Model model) {
        cobrador = servicioCobrador.encontrar(cobrador);
        if (cobrador == null) {
            throw new Error("Cobrar no encontrado ");
        }
        model.addAttribute("cobrador", cobrador);
        var sucursal = cobrador.getSucursal();
        model.addAttribute("sucursal", sucursal);
        var ciudad = cobrador.getSucursal().getCiudad();
        model.addAttribute("ciudad", ciudad);

        var estado = servicioEstado.findByEstadoIn(Arrays.asList("ACTIVO", "INACTIVO"));
        model.addAttribute("estado", estado);
        model.addAttribute("titulo", "Cobrador");

        return "cobrador/modificarCobrador";
    }

    @PreAuthorize("hasAnyAuthority('ROOT','ADMIN')")
    @PostMapping("/eliminar/{codigoCobrador}")
    public ResponseEntity<?> eliminar(Cobrador cobrador) {
        try {
            servicioCobrador.eliminar(cobrador);
            return ResponseEntity.ok("Cobrador Eliminado Correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al Eliminar Cobrador"+e.getMessage());
        }
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal getSucursalSession() {
        return getUserSession().getSucursal();
    }
}
