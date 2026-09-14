package ama.controladorMVC;

import ama.dominio.Empresa;
import ama.dominio.Timbrado;
import ama.dominio.UsuarioSistema;
import ama.servicio.EstadoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ama.servicio.TimbradoService;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/timbrado")
public class TimbradoController {

    @Autowired
    private TimbradoService timbradoService;
    @Autowired
    private EstadoService estadoService;
    @Autowired
    private HttpSession httpSession;

    @GetMapping("/listar")
    public String listaCategoria(Model modelo) {
        modelo.addAttribute("titulo", "Timbrados");
        modelo.addAttribute("timbrado", new Timbrado());
        modelo.addAttribute("estado", estadoService.findByEstadoIn(Arrays.asList("Activo", "Inactivo")));
        modelo.addAttribute("timbrados", timbradoService.listar());
        return "timbrado/timbrados";
    }

    @GetMapping("/agregar")
    public String agregar(Model modelo) {
        modelo.addAttribute("titulo", "Agregar Timbrado");
        modelo.addAttribute("timbrado", new Timbrado());
        modelo.addAttribute("estados", estadoService.findByEstadoIn(Arrays.asList("Activo")));
        return "timbrado/timbradoModificar";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<String> guardar(@RequestBody Timbrado timbrado
    ) {
        if (timbrado.getFechaInicio() == null || timbrado.getFechaFin() == null
                || timbrado.getFechaFin().before(timbrado.getFechaInicio())) {
            return ResponseEntity.badRequest().body("La fecha fin de vigencia debe ser igual o posterior a la fecha de inicio.");
        }
        Empresa empresa = getUserSession().getSucursal().getEmpresa();
        timbrado.setEmpresa(empresa);

        if (timbrado.getCodigoTimbrado() == null) {
            timbrado.setCodigoTimbrado(timbradoService.getCodigoTimbrado() + 1);
        }
        try {
            timbradoService.guardar(timbrado);
            return ResponseEntity.ok("Registro guardado correctamente ");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al guardar registro" + e.getMessage());
        }
    }

    @GetMapping("/editar")
    @ResponseBody
    public ResponseEntity<?> editar(@RequestParam Integer codigoTimbrado) {
        var timbradoEncontrada = timbradoService.encontrar(new Timbrado(codigoTimbrado));

        if (timbradoEncontrada == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Registro no encontrado");
        }
        return ResponseEntity.ok(timbradoEncontrada);
    }
    @PostMapping("/eliminar/{codigoTimbrado}")
    public ResponseEntity<?> eliminar(Timbrado timbrado) {
        try {
            log.info(timbrado.toString());
            timbradoService.eliminar(timbrado);
            return ResponseEntity.ok().body("Timbrado Eliminda Correctamente ");
        } catch (DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al Eliminar Timbrado "+ e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error al Eliminar Timbrado " + e.getMessage());
        }
    }

    private UsuarioSistema getUserSession() {
        return (UsuarioSistema) httpSession.getAttribute("usuarioSistema");
    }
}
