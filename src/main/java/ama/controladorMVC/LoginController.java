package ama.controladorMVC;

import ama.dominio.UsuarioSistema;
import ama.modulos.ingresosv2.IngresoComprobanteRepositoryV2;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
public class LoginController {

    private final IngresoComprobanteRepositoryV2 comprobantes;

    public LoginController(IngresoComprobanteRepositoryV2 comprobantes) {
        this.comprobantes = comprobantes;
    }

    @GetMapping("/")
    public String inicio(Model model, HttpSession session) {
        LocalDate hoy = LocalDate.now();
        UsuarioSistema usuario = (UsuarioSistema) session.getAttribute("usuarioSistema");
        List<Integer> anios = List.of(hoy.getYear());
        if (usuario != null && usuario.getSucursal() != null) {
            anios = comprobantes.aniosDisponibles(usuario.getSucursal().getCodigoSucursal());
            if (anios.isEmpty()) {
                anios = List.of(hoy.getYear());
            }
        }
        model.addAttribute("aniosIngresos", anios);
        model.addAttribute("anioActual", hoy.getYear());
        model.addAttribute("mesActual", hoy.getMonthValue());
        return "index";
    }

    @GetMapping("/login")
    public String agregar(
            @RequestParam(required = false, name = "error") String error,
            @RequestParam(required = false, name = "logout") String logout,
            Model model, Principal principal, RedirectAttributes flash,
            HttpServletRequest request) {
        model.addAttribute("titulo", "Iniciar sesión");
        if (principal != null) {
            flash.addFlashAttribute("info", "Ya ha inciado sesión anteriormente");
            return "redirect:/";
        }
        HttpSession session = request.getSession();
        UsuarioSistema usuarioSistema = (UsuarioSistema) session.getAttribute("usuarioSistema");
        String mensaje = null;

        if (error != null) {
            if (usuarioSistema == null) {
                mensaje = "Usuario no encontrado";
            } else if (usuarioSistema != null) {
                if (!usuarioSistema.getDetalleUsuarioSistema().isEmpty()) {
                    mensaje = "Contraña incorrecta!!";
                } else if (usuarioSistema.getDetalleUsuarioSistema().isEmpty()) {
                    mensaje = usuarioSistema.getNombre() + " no tiene rol asignado !!";
                    session.invalidate();
                }
            }
            model.addAttribute("error", mensaje);
        }


        return "login";
    }

}
