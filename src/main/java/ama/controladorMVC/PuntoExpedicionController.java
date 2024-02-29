package ama.controladorMVC;

import ama.dominio.Empresa;
import ama.dominio.PuntoExpedicion;
import ama.servicio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @GetMapping("/listar")
    public String listaPuntoExpediciones(Model modelo) {
         modelo.addAttribute("titulo", "PuntoExpedicion");
        var puntosExpediciones = servicioPuntoExpedicion.listar();
        modelo.addAttribute("puntosExpedicion", puntosExpediciones);

        return "puntoExpedicion/puntoExpedicion";
    }

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
         modelo.addAttribute("titulo", "PuntoExpedicion");
        var puntoExpedicion = new PuntoExpedicion();
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
    public String guardar(PuntoExpedicion puntoexpedicion) {
        if (puntoexpedicion.getCodigoPuntoExpedicion() == null) {
            Integer condigoPuntoExpedicion = servicioPuntoExpedicion.getCodigoPuntoExpedicion() + 1;
            puntoexpedicion.setCodigoPuntoExpedicion(condigoPuntoExpedicion);
        }
        Empresa empresa = new Empresa();
        empresa.setCodigoEmpresa(1);
        puntoexpedicion.setEmpresa(empresa);
        servicioPuntoExpedicion.guardar(puntoexpedicion);
        return "redirect:/puntoExpedicion/listar";
    }

    @GetMapping("/editar/{codigoPuntoExpedicion}")
    public String editar(PuntoExpedicion puntoExpedicion, Model model) {
         model.addAttribute("titulo", "PuntoExpedicion");
        puntoExpedicion = servicioPuntoExpedicion.encontrar(puntoExpedicion);
        model.addAttribute("puntosExpedicion", puntoExpedicion);

        var sucursales = puntoExpedicion.getSucursal();
        model.addAttribute("sucursales", sucursales);

        var ciudades = sucursales.getCiudad();
        model.addAttribute("ciudades", ciudades);

        var estados = servicioEstado.listar();
        model.addAttribute("estados", estados);
        return "puntoExpedicion/modificarPuntoExpedicion";
    }

    @GetMapping("/eliminar")
    public String eliminar(PuntoExpedicion puntoExpedicon) {
        log.info("PUNTOEXPEDICION A ELIMIANR: "+puntoExpedicon);
        servicioPuntoExpedicion.eliminar(puntoExpedicon);
        return "redirect:/puntoExpedicion/listar";
    }
}
