package ama.controladorMVC;

import ama.dominio.*;
import ama.errores.ClaseError;
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
    public ResponseEntity<?> listaDetalleZonaPorZona(@RequestBody Manzana manzana) {
        return ResponseEntity.ok(manzanaService.listar(manzana.getZona()));
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
        model.addAttribute("zona", resultadoZona);

        model.addAttribute("zonas", zonaService.listar());
        model.addAttribute("cobrador", resultadoZona.getCobrador());

        model.addAttribute("manzana", new Manzana());

        model.addAttribute("manzanas", manzanaService.listar(zona));

        model.addAttribute("sucursal", getUserSession().getSucursal());
        return "manzana/agregar";
    }

    @PostMapping("/guardar")
    public ResponseEntity<String> guardar(Manzana manzana, ManzanaPK manzanaPK, Model model) {

        manzanaPK.setCodigoSucursal(manzana.getSucursal().getCodigoSucursal());
        Manzana manzanaRecuperada = manzanaService.encontrar(manzanaPK);

        if (manzanaRecuperada != null) {
            String mensaje = "Manzana N° " + manzanaPK.getNumeroManzana() + " ya se encueantra registrada en zona "
                    + manzanaRecuperada.getZona().getNombreZona();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }

        try {
            manzana.setManzanaPK(manzanaPK);
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
        manzanaPK.setCodigoSucursal(manzana.getSucursal().getCodigoSucursal());
        Manzana manzanaRecuperada = manzanaService.encontrar(manzanaPK);
        String mensaje;
        if (manzanaRecuperada == null) {
            mensaje = "Manzana N° " + manzanaPK.getNumeroManzana() + " no se encueantra registrada en la base de datos ";
            flash.addFlashAttribute("mensaje", mensaje);
        }

        try {
            manzana.setManzanaPK(manzanaPK);
            manzanaService.guardar(manzana);
            mensaje = "Manzana Modificada correctamente!!";
            flash.addFlashAttribute("mensaje", mensaje);

        } catch (DataAccessException e) {
            mensaje = "Error al agregar manzana " + e.getMostSpecificCause().getMessage();
            flash.addFlashAttribute("mensaje", mensaje);
        }
        return "redirect:/manzana/agregar/" + zonaVieja;

    }

    @PreAuthorize("hasAnyAuthority({'ADMIN'})")
    @PostMapping("/eliminar/{numeroManzana}/{codigoSucursal}")
    public ResponseEntity<String> eliminar(ManzanaPK manzanaPK) {
        try {
            manzanaService.eliminar(new Manzana(manzanaPK));
            return ResponseEntity.ok().body("Manzana Eliminado Correctamente!!");

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Error al Elimar Registro: " + ClaseError.excepcion("Error al eliminar manzana, ", e));
        }
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }
}
