package ama.controladorMVC;

import ama.dao.DetalleUsuarioSistemaDao;
import ama.dominio.Estado;
import ama.dominio.Rol;
import ama.dominio.UsuarioSistema;
import ama.servicio.RolService;
import ama.servicio.UsuarioSistemaService;
import ama.utilerias.PageRender;
import ama.validador.Mayuscula;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
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
    @PreAuthorize("hasAnyAuthority({'ADMINISTRADOR','ROOT'})")
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
    @PreAuthorize("hasAnyAuthority({'ADMINISTRADOR','ROOT'})")
    public String agregar(Model modelo) {
        modelo.addAttribute("titulo", "Usuario");
        UsuarioSistema usuarioSistema = new UsuarioSistema();
        modelo.addAttribute("usuarioSistema", usuarioSistema);

        return "usuarioSistema/modificarUsuarioSistema";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority({'ADMINISTRADOR','ROOT'})")
    public String guardar(UsuarioSistema usuarioSistema, RedirectAttributes flash) {
        String mensaje = "Usuario modificado correctamente!!";
        UsuarioSistema userSession = (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
        usuarioSistema.setEstado(new Estado(1));
        usuarioSistema.setSucursal(userSession.getSucursal());
        if (usuarioSistema.getCodigoUsuarioSistema() == null) {
            Integer codigoUsuario = usuarioSistemaService.getCodigoUsuarioSistema() + 1;
            usuarioSistema.setCodigoUsuarioSistema(codigoUsuario);
            mensaje = "Usuario agregado correctamente";
        String clave = encoder.encode(usuarioSistema.getClave());
        usuarioSistema.setClave(clave);
        }
        usuarioSistemaService.save(usuarioSistema);
        flash.addFlashAttribute("mensaje", mensaje);
        return "redirect:/usuarioSistema/listar";
    }

    @PreAuthorize("hasAnyAuthority({'ROOT','ADMINISTRADOR'})")
    @GetMapping("/roles/{codigoUsuarioSistema}")
    public String asignarRol(UsuarioSistema usuarioSistema, Model model) {
        usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
        model.addAttribute("usuarioSistema", usuarioSistema);

        List<Rol> roles = rolService.listar();
        model.addAttribute("roles", roles);

        return "usuarioSistema/agregarDetalleUsuarioSistema";
    }

    @GetMapping("/editar/{codigoUsuarioSistema}")
    public String editar(UsuarioSistema usuarioSistemaRequest, Model model) {
        model.addAttribute("titulo", "Usuario");
        UsuarioSistema usuarioSistema = usuarioSistemaService.findById(usuarioSistemaRequest.getCodigoUsuarioSistema())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado !!"));
        model.addAttribute("usuarioSistema", usuarioSistema);

        return "usuarioSistema/modificarUsuarioSistema";
    }

    @GetMapping("/editarClave/{codigoUsuarioSistema}")
    public String editarClave(UsuarioSistema usuarioSistema, Model model) {
        model.addAttribute("titulo", "Usuario");
        usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado !!"));
        model.addAttribute("usuarioSistema", usuarioSistema);

        return "usuarioSistema/editarClave";
    }

    @PostMapping("/editarClave")
    public String modifgicarContraseña(UsuarioSistema usuario, RedirectAttributes flash, HttpServletRequest request, HttpServletResponse response) {
        UsuarioSistema usuarioSistema = usuarioSistemaService.findById(usuario.getCodigoUsuarioSistema())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado !!"));
        String clave = encoder.encode(usuario.getClave());
        usuarioSistema.setClave(clave);
        usuarioSistemaService.save(usuarioSistema);
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        flash.addFlashAttribute("info", "Contraseña modificada corectamente!!");
        return "redirect:/login";

    }

    @PostMapping("/passwordReset/{codigoUsuarioSistema}")
    public ResponseEntity<?> passwordReset(UsuarioSistema usuarioSistema) {
        try {
            usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);
            String clave = encoder.encode("1234");
            usuarioSistema.setClave(clave);
            usuarioSistema = usuarioSistemaService.save(usuarioSistema);

            return ResponseEntity.ok("Clave reseteada al 1234, inicie sesion y modifique su clave!!");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al realizar reseteo de Clave "+ e);
        }

    }

    @Transactional
    @PostMapping("/eliminar/{codigoUsuarioSistema}")
    public ResponseEntity<?> modal(UsuarioSistema usuarioSistema) {
        try {
            detalleUsuarioSistemaDao.deleteByUsuarioSistema(usuarioSistema);
            usuarioSistema = usuarioSistemaService.findById(usuarioSistema.getCodigoUsuarioSistema()).orElse(null);

            usuarioSistemaService.delete(usuarioSistema);
            return ResponseEntity.ok("Usuario eliminado correctamente !!");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al eliminar usuario "+ e.getMessage());
        }

    }
}
