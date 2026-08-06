package ama.controladorMVC;

import ama.dominio.DetalleUsuarioSistema;
import ama.dominio.DetalleUsuarioSistemaPK;
import ama.dominio.Rol;
import ama.dominio.UsuarioSistema;
import ama.servicio.RolService;
import ama.servicio.UsuarioSistemaService;
import ama.validador.Mayuscula;
import java.util.List;
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
import ama.servicio.DetalleUsuarioSistemaService;

@Slf4j
@Controller
@RequestMapping("/detalleUsuarioSistema")
public class DetalleUsuarioSistemaController {

    @Autowired
    private DetalleUsuarioSistemaService servicioDetalleUsuarioSistema;
    
    @Autowired
    private UsuarioSistemaService usuarioSistemaService;

    @Autowired
    private RolService rolService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

   

    @GetMapping("/agregar/{codigoUsuarioSistema}")
    public String asignarRol(UsuarioSistema usuarioSistema, Model model) {
          usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
        model.addAttribute("usuarioSistema", usuarioSistema);
        
        List<Rol> roles=rolService.listar();
        model.addAttribute("roles", roles);
        
        List<DetalleUsuarioSistema> detalleUsuarioSistema=servicioDetalleUsuarioSistema.listar(usuarioSistema);
        model.addAttribute("detallesUsuarioSistema",detalleUsuarioSistema);
        
        
        return "usuarioSistema/agregarDetalleUsuarioSistema";
    }
    @PostMapping("/guardar")
    public String guardar(DetalleUsuarioSistemaPK detalleUsuarioSistemaPK, DetalleUsuarioSistema detalleUsuarioSistema, RedirectAttributes flash) {
        detalleUsuarioSistemaPK.setCodigoRol(detalleUsuarioSistema.getRol().getCodigoRol());
        detalleUsuarioSistema.setDetalleUsuarioSistemaPK(detalleUsuarioSistemaPK);
        servicioDetalleUsuarioSistema.guardar(detalleUsuarioSistema);
        String mensaje = "Rol asignado  correctamente!!";
        flash.addFlashAttribute("mensaje", mensaje);
        return "redirect:/detalleUsuarioSistema/agregar/"+detalleUsuarioSistemaPK.getCodigoUsuarioSistema();
    }

   
    @GetMapping("/editar/{codigoUsuarioSistema}")
    public String editar(UsuarioSistema usuarioSistema, Model model) {
        model.addAttribute("titulo", "Usuario");
        usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
        if (usuarioSistema == null) {
            throw new Error("Usuario no encontrado !!");
        }
        model.addAttribute("usuarioSistema", usuarioSistema);

        return "usuarioSistema/modificarUsuarioSistema";
    }

    @PostMapping("/eliminar/{codigoUsuarioSistema}/{codigoRol}")
    public ResponseEntity<?> modal(DetalleUsuarioSistemaPK detalleUsuarioSistemaPK) {
        try {
            servicioDetalleUsuarioSistema.eliminar(detalleUsuarioSistemaPK);
            return ResponseEntity.ok("Rol eliminado correctamente !!");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al eliminar rol " +e.getMessage());
        }

    }
}
