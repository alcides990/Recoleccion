package ama.controladorMVC;

import ama.dominio.PuntoExpedicion;
import ama.dominio.PuntoExpedicionPK;
import ama.dominio.Sucursal;
import ama.dominio.UsuarioSistema;
import ama.servicio.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

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
    public String listaPuntoExpediciones(
            @RequestParam(required = false) Integer codigoSucursal,
            Model modelo) {
        modelo.addAttribute("titulo", "PuntoExpedicion");
        Sucursal sucursal = resolverSucursal(codigoSucursal);
        List<PuntoExpedicion> puntosExpediciones = servicioPuntoExpedicion.listar(sucursal);
        modelo.addAttribute("puntosExpedicion", puntosExpediciones);
        modelo.addAttribute("sucursalSeleccionada", sucursal);
        modelo.addAttribute("esRoot", esRoot());
        if (esRoot()) {
            modelo.addAttribute("sucursalesDisponibles", servicioSucursal.listar());
        }
        return "puntoExpedicion/puntoExpedicion";
    }

    @GetMapping("/agregar")
    @PreAuthorize("hasAnyAuthority({'ROOT'})")
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
     @PreAuthorize("hasAnyAuthority({'ROOT'})")
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
     @PreAuthorize("hasAnyAuthority({'ROOT'})")
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
     @PreAuthorize("hasAnyAuthority({'ROOT'})")
    public String eliminar(PuntoExpedicionPK puntoExpedicionPK, RedirectAttributes redirectAttributes) {
        servicioPuntoExpedicion.eliminar(puntoExpedicionPK);
        redirectAttributes.addFlashAttribute("info", "PuntoExpedicion eliminada correctamente!!");
        return "redirect:/puntoExpedicion/listar";
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private Sucursal resolverSucursal(Integer codigoSolicitado) {
        UsuarioSistema usuario = getUserSession();
        if (usuario == null || usuario.getSucursal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Integer codigo = esRoot() && codigoSolicitado != null
                ? codigoSolicitado : usuario.getSucursal().getCodigoSucursal();
        Sucursal sucursal = servicioSucursal.encontrar(new Sucursal(codigo));
        if (sucursal == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada");
        }
        return sucursal;
    }

    private boolean esRoot() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> "ROOT".equals(autoridad.getAuthority()));
    }
}
