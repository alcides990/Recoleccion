package ama.controladorMVC;

import ama.dominio.UsuarioSistema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
public class LoginController {

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/login")
    public String agregar(
            @RequestParam(required = false, name = "error") String error,
            @RequestParam(required = false, name = "logout") String logout,
            Model model, Principal principal, RedirectAttributes flash,
            HttpServletRequest request) {
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

        if (logout
                != null) {
            model.addAttribute("info", "Ha cerrado secion con exito!!");
        }

        return "login";
    }

}
