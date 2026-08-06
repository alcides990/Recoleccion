package ama.controladorMVC;

import ama.dominio.Empresa;
import ama.dominio.Sucursal;
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

    @GetMapping("/listar")
    public String listaSucursal(Model modelo) {
        modelo.addAttribute("titulo", "Sucursal");
        var sucursales = servicioSucursal.listar();
        modelo.addAttribute("sucursales", sucursales);

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
    @PreAuthorize("hasAnyAuthority({'ROOT'})")
    public String guardar(Sucursal sucursal, RedirectAttributes flash) {
        if (sucursal.getCodigoSucursal() == null) {
            Integer codigoSucursal = servicioSucursal.getCodigoSucursal() + 1;
            sucursal.setCodigoSucursal(codigoSucursal);
        }
        Empresa empresa = new Empresa();
        empresa.setCodigoEmpresa(1);
        sucursal.setEmpresa(empresa);
        servicioSucursal.guardar(sucursal);
        flash.addFlashAttribute("info", "Sucursal guardada correctamente!!");
        return "redirect:/sucursal/listar";
    }

    @GetMapping("/editar/{codigoSucursal}")
    @PreAuthorize("hasAnyAuthority({'ROOT'})")
    public String editar(Sucursal sucursal, Model model) {
        model.addAttribute("titulo", "Editar Sucursal");
        sucursal = servicioSucursal.encontrar(sucursal);
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
}
