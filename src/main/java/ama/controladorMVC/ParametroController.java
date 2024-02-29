package ama.controladorMVC;

import ama.dominio.Comision;
import ama.dominio.Empresa;
import ama.dominio.Parametro;
import ama.dominio.UsuarioSistema;
import ama.servicio.ParametroService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ama.servicio.SucursalService;
import ama.servicio.CiudadService;
import ama.servicio.ComisionService;
import ama.servicio.EmpresaServise;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/parametro")
public class ParametroController {

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private ParametroService parametroService;
    @Autowired
    private SucursalService sucursalService;
    @Autowired
    private CiudadService ciudadService;
    @Autowired
    private EmpresaServise empresaServise;
    @Autowired
    private ComisionService comisionService;

    @GetMapping("/listar")
    public String listaCategoria(Model modelo) {
        modelo.addAttribute("titulo", "Parametro");
        UsuarioSistema userSession = getUserSession();
        modelo.addAttribute("empresa", empresaServise.encontrar(userSession.getSucursal().getEmpresa()));
        Parametro parametro = parametroService.encontrar(userSession.getSucursal());
        modelo.addAttribute("parametro", parametro);

        List<Comision> comisiones = comisionService.listar();
        modelo.addAttribute("comisiones", comisiones);

        modelo.addAttribute("sucursal", userSession.getSucursal());
        return "parametro";
    }
    @Transactional
    @PostMapping("/guardar")
    public String guardar(Empresa empresa, Parametro parametro, RedirectAttributes flash) {
        parametro.setEmpresa(empresa);
        if(parametro.getCierrePeriodo().isAfter(LocalDate.now())){
            flash.addFlashAttribute("error", "Fecha cierre periodo no puede ser mayor a la fecha de hoy!!");
            return "redirect:/parametro/listar";
        }
        parametroService.guardar(parametro);
        flash.addFlashAttribute("mensaje", "Datos de parametro modificado correctamiente!!");
        return "redirect:/parametro/listar";
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }
}
