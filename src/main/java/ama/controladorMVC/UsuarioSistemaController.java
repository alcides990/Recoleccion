package ama.controladorMVC;

import ama.dao.DetalleUsuarioSistemaDao;
import ama.dominio.Estado;
import ama.dominio.Rol;
import ama.dominio.UsuarioSistema;
import ama.errores.ClaseError;
import ama.servicio.RolService;
import ama.servicio.UsuarioSistemaService;
import ama.utilerias.PageRender;
import ama.validador.Mayuscula;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/usuarioSistema")
public class UsuarioSistemaController {

    @Autowired
    private UsuarioSistemaService usuarioSistemaService;

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private RolService rolService;

    @Autowired
    private DetalleUsuarioSistemaDao detalleUsuarioSistemaDao;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @GetMapping("/listar")
     @PreAuthorize("hasAnyAuthority({'ADMIN'})")
    public String listaUsuario(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "cantElemento", defaultValue = "10") int cantElemento,
            @RequestParam(name = "filtro", defaultValue = "") String filtro, Model modelo) {
        modelo.addAttribute("titulo", "UsuarioSistema");
        Pageable pageable = PageRequest.of(page, cantElemento);
        Page<UsuarioSistema> usuariosSistema = usuarioSistemaService.findAll(pageable);
        PageRender pageRender = new PageRender("/usuarioSistema/listar", usuariosSistema);
        modelo.addAttribute("page", pageRender);
        modelo.addAttribute("usuariosSistema", usuariosSistema);

        return "usuarioSistema/usuarioSistema";
    }

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        modelo.addAttribute("titulo", "Usuario");
        UsuarioSistema usuarioSistema = new UsuarioSistema();
        modelo.addAttribute("usuarioSistema", usuarioSistema);

        return "usuarioSistema/modificarUsuarioSistema";
    }

    @PostMapping("/guardar")
    public String guardar(UsuarioSistema usuarioSistema, RedirectAttributes flash) {
        String mensaje = "Usuario modificado correctamente!!";
        UsuarioSistema userSession = (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
        usuarioSistema.setEstado(new Estado(1));
        usuarioSistema.setSucursal(userSession.getSucursal());
        if (usuarioSistema.getCodigoUsuarioSistema() == null) {
            Integer codigoUsuario = usuarioSistemaService.getCodigoUsuarioSistema() + 1;
            usuarioSistema.setCodigoUsuarioSistema(codigoUsuario);
            mensaje = "Usuario agregado correctamente";
        }
        String clave = encoder.encode(usuarioSistema.getClave());
        usuarioSistema.setClave(clave);
        log.info(usuarioSistema.getCodigoUsuarioSistema().toString());
        usuarioSistemaService.save(usuarioSistema);
        flash.addFlashAttribute("mensaje", mensaje);
        return "redirect:/usuarioSistema/listar";
    }

    @GetMapping("/roles/{codigoUsuarioSistema}")
    public String asignarRol(UsuarioSistema usuarioSistema, Model model) {
        usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
        model.addAttribute("usuarioSistema", usuarioSistema);

        List<Rol> roles = rolService.listar();
        model.addAttribute("roles", roles);

        return "usuarioSistema/agregarDetalleUsuarioSistema";
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
    @GetMapping("/editarClave/{codigoUsuarioSistema}")
    public String editarClave(UsuarioSistema usuarioSistema, Model model) {
        model.addAttribute("titulo", "Usuario");
        usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
        if (usuarioSistema == null) {
            throw new Error("Usuario no encontrado !!");
        }
        model.addAttribute("usuarioSistema", usuarioSistema);

        return "usuarioSistema/editarClave";
    }

    @PostMapping("/eliminar/{codigoUsuarioSistema}")
    public ResponseEntity<?> modal(UsuarioSistema usuarioSistema) {
        try {
            detalleUsuarioSistemaDao.deleteByUsuarioSistema(usuarioSistema);
            usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);

            usuarioSistemaService.delete(usuarioSistema);
            return ResponseEntity.ok("Usuario eliminado correctamente !!");

        } catch (Exception e) {
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ClaseError.excepcion("Error al eliminar usuario ", e));
        }

    }
}
