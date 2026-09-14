package ama.controladorMVC;

import ama.dominio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ama.servicio.CobradorService;
import ama.servicio.ZonaService;
import ama.servicio.SucursalService;
import jakarta.servlet.http.HttpSession;
import ama.servicio.ManzanaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ama.servicio.CiudadService;

@Slf4j
@Controller
@RequestMapping("/manzana")
public class ManzanaController {

    @Autowired
    private HttpSession httpSession;
    @Autowired
    private ManzanaService manzanaService;
    @Autowired
    private CobradorService cobradorService;
    @Autowired
    private ZonaService zonaService;
    @Autowired
    private SucursalService sucursalService;
    @Autowired
    private CiudadService servicioCiudad;

    @GetMapping("/listar")
    public ResponseEntity<?> listaDetalleZonaPorZona(@RequestParam Integer codigoZona) {
        Zona zona = zonaService.encontrar(new Zona(codigoZona));
        if (zona == null || !esSucursalDeSesion(zona.getSucursal())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Zona no encontrada en su sucursal");
        }
        return ResponseEntity.ok(manzanaService.listar(zona));
    }

    @GetMapping("/editar")
    public ResponseEntity<?> editarManzana(
            @RequestParam Integer numeroManzana,
            @RequestParam Integer codigoSucursal
    ) {
        ManzanaPK manzanaPK = new ManzanaPK(numeroManzana, codigoSucursal);
        return ResponseEntity.ok(manzanaService.encontrar(manzanaPK));
    }

    @GetMapping("/agregar/{codigoZona}")
    public String agregarManzana(Zona zona, Model model) {
        Zona resultadoZona = zonaService.encontrar(zona);
        if (resultadoZona == null || !esSucursalDeSesion(resultadoZona.getSucursal())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Zona no encontrada en su sucursal");
        }
        model.addAttribute("zona", resultadoZona);

        model.addAttribute("zonas", zonaService.listar().stream()
                .filter(item -> esSucursalDeSesion(item.getSucursal()))
                .toList());
        model.addAttribute("cobrador", resultadoZona.getCobrador());

        model.addAttribute("manzana", new Manzana());

        model.addAttribute("manzanas", manzanaService.listar(zona));

        model.addAttribute("sucursal", getUserSession().getSucursal());
        return "manzana/agregar";
    }

    @PostMapping("/guardar")
    public ResponseEntity<String> guardar(Manzana manzana, ManzanaPK manzanaPK, Model model) {
        if (manzanaPK.getNumeroManzana() < 1) {
            return ResponseEntity.badRequest().body("Número de manzana debe ser mayor a cero");
        }
        Zona zona = zonaService.encontrar(manzana.getZona());
        if (zona == null || !esSucursalDeSesion(zona.getSucursal())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("La zona no pertenece a su sucursal");
        }
        Sucursal sucursalSesion = getUserSession().getSucursal();
        manzanaPK.setCodigoSucursal(sucursalSesion.getCodigoSucursal());
        Manzana manzanaRecuperada = manzanaService.encontrar(manzanaPK);

        if (manzanaRecuperada != null) {
            String mensaje = "Manzana N° " + manzanaPK.getNumeroManzana() + " ya se encueantra registrada en zona "
                    + manzanaRecuperada.getZona().getNombreZona();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }

        try {
            manzana.setManzanaPK(manzanaPK);
            manzana.setZona(zona);
            manzana.setSucursal(sucursalSesion);
            manzanaService.guardar(manzana);
            return ResponseEntity.ok("Manzana agregada correctamente!!");

        } catch (DataAccessException e) {
            String mensaje = "Error al agregar manzana " + e.getMostSpecificCause().getMessage();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }

    }

    @PostMapping("/modificar")
    public String modificar(Manzana manzana, ManzanaPK manzanaPK, RedirectAttributes flash,
            @RequestParam Integer zonaVieja
    ) {
        Sucursal sucursalSesion = getUserSession().getSucursal();
        manzanaPK.setCodigoSucursal(sucursalSesion.getCodigoSucursal());
        Manzana manzanaRecuperada = manzanaService.encontrar(manzanaPK);
        String mensaje;
        if (manzanaRecuperada == null) {
            mensaje = "Manzana N° " + manzanaPK.getNumeroManzana() + " no se encueantra registrada en la base de datos ";
            flash.addFlashAttribute("mensaje", mensaje);
            return "redirect:/manzana/agregar/" + zonaVieja;
        }

        Zona zonaNueva = zonaService.encontrar(manzana.getZona());
        if (zonaNueva == null || !esSucursalDeSesion(zonaNueva.getSucursal())) {
            flash.addFlashAttribute("mensaje", "La zona seleccionada no pertenece a su sucursal");
            return "redirect:/manzana/agregar/" + zonaVieja;
        }

        try {
            manzana.setManzanaPK(manzanaPK);
            manzana.setZona(zonaNueva);
            manzana.setSucursal(sucursalSesion);
            manzanaService.guardar(manzana);
            mensaje = "Manzana Modificada correctamente!!";
            flash.addFlashAttribute("mensaje", mensaje);

        } catch (DataAccessException e) {
            mensaje = "Error al agregar manzana " + e.getMostSpecificCause().getMessage();
            flash.addFlashAttribute("mensaje", mensaje);
        }
        return "redirect:/manzana/agregar/" + zonaVieja;

    }

    @PreAuthorize("hasAnyAuthority({'ROOT','ADMINISTRADOR'})")
    @PostMapping("/eliminar/{numeroManzana}/{codigoSucursal}")
    public ResponseEntity<String> eliminar(ManzanaPK manzanaPK) {
        try {
            if (!Integer.valueOf(manzanaPK.getCodigoSucursal())
                    .equals(getUserSession().getSucursal().getCodigoSucursal())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("La manzana no pertenece a su sucursal");
            }
            manzanaService.eliminar(new Manzana(manzanaPK));
            return ResponseEntity.ok().body("Manzana Eliminado Correctamente!!");

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al eliminar manzana"+ e.getMessage());
        }
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }

    private boolean esSucursalDeSesion(Sucursal sucursal) {
        return sucursal != null
                && sucursal.getCodigoSucursal().equals(getUserSession().getSucursal().getCodigoSucursal());
    }
}
