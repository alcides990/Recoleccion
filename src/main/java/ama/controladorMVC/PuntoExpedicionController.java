package ama.controladorMVC;

import ama.dominio.PuntoExpedicion;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.servicio.*;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/puntoExpedicion")
public class PuntoExpedicionController {

    @Autowired
    private PuntoExpedicionService servicioPuntoExpedicion;
    @Autowired
    private SucursalService servicioSucursal;
    @Autowired
    private CiudadService servicioCiudad;
    @Autowired
    private EstadoService servicioEstado;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listaPuntoExpediciones(Model modelo) {
        modelo.addAttribute("titulo", "PuntoExpedicion");
        var puntosExpediciones = servicioPuntoExpedicion.listar();
        modelo.addAttribute("puntosExpedicion", puntosExpediciones);
        modelo.addAttribute("puntosExpedicion", puntosExpediciones);
        return "puntoExpedicion/puntoExpedicion";
    }

    @GetMapping("/agregar")
    public String agregar(PuntoExpedicionPK puntoExpedicionPK, Model modelo) {
        modelo.addAttribute("titulo", "PuntoExpedicion");
        var puntoExpedicion = new PuntoExpedicion(puntoExpedicionPK);
        modelo.addAttribute("puntosExpedicion", puntoExpedicion);

        var sucursales = servicioSucursal.listar();
        modelo.addAttribute("sucursales", sucursales);
        var estados = servicioEstado.listar();
        modelo.addAttribute("estados", estados);

        var ciudades = servicioCiudad.listarCiudad();
        modelo.addAttribute("ciudades", ciudades);
        return "puntoExpedicion/modificarPuntoExpedicion";
    }

    @PostMapping("/guardar")
    public String guardar(PuntoExpedicion puntoExpedicion, PuntoExpedicionPK puntoExpedicionPK, RedirectAttributes redirectAttributes) {
        Sucursal sucursal = puntoExpedicion.getSucursal();
        if (puntoExpedicionPK.getCodigoPuntoExpedicion() == null) {
            Integer condigoPuntoExpedicion = servicioPuntoExpedicion.getCodigoPuntoExpedicion(sucursal) + 1;
            puntoExpedicionPK.setCodigoPuntoExpedicion(condigoPuntoExpedicion);
        }
        puntoExpedicionPK.setCodigoSucursal(sucursal.getCodigoSucursal());
        puntoExpedicion.setPuntoExpedicionPK(puntoExpedicionPK);
        puntoExpedicion.setEmpresa(getUserSession().getSucursal().getEmpresa());
        servicioPuntoExpedicion.guardar(puntoExpedicion);
        redirectAttributes.addFlashAttribute("info", "PuntoExpedicion agregada correctamente!!");
        return "redirect:/puntoExpedicion/listar";
    }

    @GetMapping("/editar/{codigoPuntoExpedicion}/{codigoSucursal}")
    public String editar(PuntoExpedicion puntoExpedicion,
            @PathVariable Integer codigoPuntoExpedicion,
            @PathVariable Integer codigoSucursal,
            Model model) {
        model.addAttribute("titulo", "PuntoExpedicion");
        PuntoExpedicionPK puntoExpedicionPK = PuntoExpedicionPK.builder()
                .codigoPuntoExpedicion(codigoPuntoExpedicion)
                .codigoSucursal(codigoSucursal)
                .build();
        puntoExpedicion = servicioPuntoExpedicion.encontrar(puntoExpedicionPK);
        model.addAttribute("puntosExpedicion", puntoExpedicion);

        model.addAttribute("sucursales", puntoExpedicion.getSucursal());

        model.addAttribute("estados", servicioEstado.listar());
        return "puntoExpedicion/modificarPuntoExpedicion";
    }

    @GetMapping("/eliminar/{codigoPuntoExpedicion}/{codigoSucursal}")
    public String eliminar(PuntoExpedicionPK puntoExpedicionPK, RedirectAttributes redirectAttributes) {
        servicioPuntoExpedicion.eliminar(puntoExpedicionPK);
        redirectAttributes.addFlashAttribute("info", "PuntoExpedicion eliminada correctamente!!");
        return "redirect:/puntoExpedicion/listar";
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }
}
