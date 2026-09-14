package ama.controladorMVC;

import ama.dominio.Empresa;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.validador.Mayuscula;
import ama.validador.Vadidador;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ama.servicio.SucursalService;
import ama.servicio.CiudadService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequestMapping("/sucursal")
public class SucursalController {

    @Autowired
    private Vadidador validar;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new Mayuscula());
    }

    @Autowired
    private SucursalService servicioSucursal;
    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listaSucursal(@RequestParam(required = false) Integer codigoSucursal, Model modelo) {
        modelo.addAttribute("titulo", "Sucursal");
        boolean root = esRoot();
        Sucursal sucursal = resolverSucursal(codigoSucursal);
        List<Sucursal> sucursales = root ? servicioSucursal.listar() : List.of(sucursal);

        modelo.addAttribute("sucursales", sucursales);
        modelo.addAttribute("sucursalSeleccionada", sucursal);
        modelo.addAttribute("esRoot", root);
        if (root) {
            modelo.addAttribute("sucursalesDisponibles", sucursales);
        }

        return "sucursal/sucursal";
    }

    @PreAuthorize("hasAnyAuthority({'ROOT'})")
    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        modelo.addAttribute("titulo", "Sucursal");
        var sucursal = new Sucursal();
        modelo.addAttribute("sucursal", sucursal);
        var ciudades = servicioCiudad.listarCiudad();
        modelo.addAttribute("ciudades", ciudades);
        return "sucursal/modificarSucursal";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority({'ROOT','ADMINISTRADOR'})")
    public String guardar(Sucursal sucursal, RedirectAttributes flash) {
        if (esRoot() && sucursal.getCodigoSucursal() == null) {
            Integer codigoSucursal = servicioSucursal.getCodigoSucursal() + 1;
            sucursal.setCodigoSucursal(codigoSucursal);
        } else if (!esRoot()) {
            Sucursal actual = resolverSucursal(sucursal.getCodigoSucursal());
            sucursal.setCodigoSucursal(actual.getCodigoSucursal());
            sucursal.setNombreSucursal(actual.getNombreSucursal());
            sucursal.setEmpresa(actual.getEmpresa());
        }
        Empresa empresa = new Empresa();
        empresa.setCodigoEmpresa(1);
        if (esRoot()) {
            sucursal.setEmpresa(empresa);
        }
        servicioSucursal.guardar(sucursal);
        flash.addFlashAttribute("info", "Sucursal guardada correctamente!!");
        return "redirect:/sucursal/listar";
    }

    @GetMapping("/editar/{codigoSucursal}")
    @PreAuthorize("hasAnyAuthority({'ROOT','ADMINISTRADOR'})")
    public String editar(Sucursal sucursal, Model model) {
        model.addAttribute("titulo", "Editar Sucursal");
        sucursal = resolverSucursal(sucursal.getCodigoSucursal());
        model.addAttribute("sucursal", sucursal);
        model.addAttribute("ciudades", servicioCiudad.listarCiudad());
        return "sucursal/modificarSucursal";
    }

    @GetMapping("/eliminar")
    @PreAuthorize("hasAnyAuthority({'ROOT'})")
    public String eliminar(Sucursal sucursal, RedirectAttributes flash) {
        servicioSucursal.eliminar(sucursal);
        flash.addFlashAttribute("info", "Sucursal eliminada correctamente!!");
        return "redirect:/sucursal/listar";
    }

    private Sucursal resolverSucursal(Integer codigoSolicitado) {
        UsuarioSistema usuario = (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
        if (usuario == null || usuario.getSucursal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Integer codigo = esRoot() && codigoSolicitado != null
                ? codigoSolicitado : usuario.getSucursal().getCodigoSucursal();
        Sucursal sucursal = servicioSucursal.encontrar(new Sucursal(codigo));
        if (sucursal == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada");
        }
        if (!esRoot() && !Objects.equals(codigo, usuario.getSucursal().getCodigoSucursal())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return sucursal;
    }

    private boolean esRoot() {
        return tieneAutoridad("ROOT");
    }

    private boolean tieneAutoridad(String rol) {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> rol.equals(autoridad.getAuthority()));
    }
}
